(ns cxengage-javascript-sdk.modules.mqtt
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]])
  (:require [cljsjs.paho]
            [camel-snake-kebab.core :as camel]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cljs.core.async :as a]
            [clojure.set :refer [rename-keys]]
            [cljs-time.core :as time]
            [cljs-time.format :as fmt]
            [cljs-time.instant]
            [goog.crypt :as c]
            [cljs-uuid-utils.core :as id]
            [cxengage-cljs-utils.core :as u]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cognitect.transit :as t]
            [cljs.spec :as s])
  (:import goog.crypt))

(def module-state (atom {}))

(defn get-mqtt-client []
  (get @module-state :mqtt-client))

(def service-name "iotdevicegateway")
(def algorithm "AWS4-HMAC-SHA256")
(def method "GET")
(def canonical-uri "/mqtt")

;;;;;;;;;;;;
;; spec
;;;;;;;;;;;;

(s/def ::mqtt-conf (s/keys :req-un [::endpoint ::region-name ::secret-key ::access-key]
                           :opt-un [::session-token]))

;;;;;;;;;;;;
;; fns
;;;;;;;;;;;;

(def get-host
  (memoize
   (fn [endpoint region-name]
     (str endpoint ".iot." region-name ".amazonaws.com"))))

(def get-date-stamp
  (memoize
   (fn [date]
     (fmt/unparse (fmt/formatter "yyyyMMdd") date))))

(def get-amz-date-stamp
  (memoize
   (fn [date]
     (fmt/unparse (fmt/formatter "yyyyMMddTHHmmssZ") date))))

(defn get-canonical-query-string
  [date access-key credential-scope]
  (str "X-Amz-Algorithm=AWS4-HMAC-SHA256"
       "&X-Amz-Credential=" (js/encodeURIComponent (str access-key "/" credential-scope))
       "&X-Amz-Date=" (get-amz-date-stamp date)
       "&X-Amz-Expires=86400"
       "&X-Amz-SignedHeaders=host"))

(defn get-canonical-request
  [date access-key credential-scope host]
  (let [canonical-query-string (get-canonical-query-string date access-key credential-scope)
        canonical-headers (str "host:" host "\n")
        canonical-request (clojure.string/join "\n" [method canonical-uri canonical-query-string canonical-headers "host" (u/sha256 "")])]
    canonical-request))

(defn get-credential-scope
  [date region-name service-name]
  (str (get-date-stamp date) "/" region-name "/" service-name "/aws4_request"))

(defn sign-string
  [date {:keys [secret-key region-name]} service-name credential-scope canonical-request]
  (let [string-to-sign (clojure.string/join "\n" [algorithm (get-amz-date-stamp date) credential-scope (u/sha256 canonical-request)])
        signing-key (u/get-signature-key secret-key (get-date-stamp date) region-name service-name)]
    (u/sign signing-key string-to-sign)))

(defn get-iot-url
  [date {:keys [endpoint region-name access-key secret-key session-token] :as mqtt-conf}]
  (let [host (get-host endpoint region-name)
        credential-scope (get-credential-scope date region-name service-name)
        canonical-request (get-canonical-request date access-key credential-scope host)
        signature (sign-string date mqtt-conf service-name credential-scope canonical-request)
        canonical-query-string (str (get-canonical-query-string date access-key credential-scope) "&X-Amz-Signature=" signature)
        security-token-string (when-not (clojure.string/blank? session-token)
                                (str "&X-Amz-Security-Token=" (js/encodeURIComponent session-token)))]
    (str "wss://" host canonical-uri "?" canonical-query-string security-token-string)))

(defn subscribe
  [topic]
  (.subscribe (get-mqtt-client) topic #js {:qos 1})
  (log :debug (str "Subscribed to MQTT topic: " topic)))

(defn unsubscribe
  [topic]
  (.unsubscribe (get-mqtt-client) topic))

(defn send-message-impl
  [payload topic]
  (let [msg (Paho.MQTT.Message. payload)]
    (set! (.-destinationName msg) topic)
    (set! (.-qos msg) 1)
    (.send (get-mqtt-client) msg)))

(defn on-connect [done-init<]
  (log :debug "Mqtt client connected")
  (a/put! done-init< {:status :ok}))

(defn on-failure [done-init< msg]
  (log :debug "Mqtt Client failed to connect")
  (a/put! done-init< {:status :error
                      :msg msg}))

(defn disconnect [client]
  (log :debug "Disconnecting mqtt client")
  (.disconnect client))

(defn connect
  [endpoint client-id on-received done-init<]
  (let [mqtt (Paho.MQTT.Client. endpoint client-id)
        connect-options (js/Object.)]
    (set! (.-onConnectionLost mqtt) (fn [reason-code reason-message]
                                      (a/put! (:error-channel @module-state) {:type :messaging/SHUTDOWN})
                                      (log :error "Mqtt Connection Lost" {:reasonCode reason-code
                                                                          :reasonMessage reason-message})))
    (set! (.-onMessageArrived mqtt) (fn [msg]
                                      (when msg (on-received msg))))
    (set! (.-onSuccess connect-options) (fn [] (on-connect done-init<)))
    (set! (.-onFailure connect-options) (fn [_ _ msg]
                                          (a/put! (:error-channel @module-state) {:type :messaging/SHUTDOWN})
                                          (on-failure done-init< msg)))
    (.connect mqtt connect-options)
    mqtt))

(defn mqtt-init
  [mqtt-conf client-id on-received done-init<]
  (let [mqtt-client (connect (get-iot-url (time/now) mqtt-conf) client-id on-received done-init<)]
    (swap! module-state assoc :mqtt-client mqtt-client)))

(s/fdef init
        :args (s/cat :mqtt-conf ::mqtt-conf :client-id string? :on-received fn?))

(defn subscribe-to-interaction [message]
  (let [{:keys [tenantId interactionId]} message
        topic (str (name (get @module-state :env)) "/tenants/" tenantId "/channels/" interactionId)]
    (subscribe topic)))

(defn unsubscribe-from-interaction [message])

(defn gen-payload [message]
  (let [{:keys [message userId tenantId interactionId]} message
        uid (str (id/make-random-uuid))
        metadata {:name "Agent"
                  :first-name "Agent"
                  :last-name "Agent"
                  :type "agent"}
        body {:text message}]
    {:id uid
     :tenant-id tenantId
     :type "message"
     :to interactionId
     :from userId
     :metadata metadata
     :body body
     :timestamp (fmt/unparse (fmt/formatters :date-time) (time/now))}))

(defn format-payload [message]
  (t/write (t/writer :json-verbose) (clojure.walk/stringify-keys message)))

(defn send-message [message]
  (let [{:keys [tenantId interactionId resp-chan]} message
        payload (-> message
                    (gen-payload)
                    (format-payload))
        topic (str (name (get @module-state :env)) "/tenants/" tenantId "/channels/" interactionId)]
    (send-message-impl payload topic)
    (a/put! resp-chan true)))

(defn module-shutdown-handler [msg-chan sd-chan message]
  (when message
    (a/close! msg-chan)
    (a/close! sd-chan)
    (reset! module-state {}))
  (log :debug "Received shutdown message from Core - MQTT Module shutting down...."))

(defn module-router [msg-chan sd-chan message]
  (let [handling-fn (case (:type message)
                      :MQTT/SUBSCRIBE_TO_INTERACTION subscribe-to-interaction
                      :MQTT/UNSUBSCRIBE_FROM_INTERACTION unsubscribe-from-interaction
                      :MQTT/SEND_MESSAGE send-message
                      :MQTT/SHUTDOWN (partial module-shutdown-handler msg-chan sd-chan)
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in MQTT SDK module." (:type message)))))

(defn init
  [env done-init< client-id config on-msg-fn err-chan]
  (log :debug "Initializing SDK module: MQTT")
  (swap! module-state assoc :env env)
  (let [module-inputs< (a/chan 1024)
        module-shutdown< (a/chan 1024)
        mqtt-config (first (filter #(= (:type %) "messaging") (:integrations config)))
        mqtt-config (->> (merge (select-keys mqtt-config [:region :endpoint])
                                (select-keys (:credentials mqtt-config) [:secretKey :accessKey :sessionToken]))
                         (transform-keys camel/->kebab-case-keyword)
                         (#(rename-keys % {:region :region-name})))]
    (swap! module-state assoc :error-channel err-chan)
    (if-not mqtt-config
      (a/put! done-init< {:status :failure})
      (do (u/start-simple-consumer! module-inputs< (partial module-router module-inputs< module-shutdown<))
          (u/start-simple-consumer! module-shutdown< (partial module-shutdown-handler module-inputs< module-shutdown<))
          (mqtt-init mqtt-config client-id on-msg-fn done-init<)
          {:messages module-inputs<
           :shutdown module-shutdown<}))))
