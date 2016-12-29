(ns client-sdk.mqtt
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljsjs.paho]
            [camel-snake-kebab.core :as camel]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cljs.core.async :as a]
            [clojure.set :refer [rename-keys]]
            [cljs-time.core :as time]
            [cljs-time.format :as fmt]
            [cljs-time.instant]
            [goog.crypt :as c]
            [lumbajack.core :refer [log]]
            [cljs-uuid-utils.core :as uuid]
            [client-sdk-utils.core :as u]
            [cljs.spec :as s])
  (:import goog.crypt))

(def module-state (atom {}))

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
  [client topic qos]
  (.subscribe client topic #js {:qos 1}))

(defn unsubscribe
  [client topic]
  (.unsubscribe client topic))

(defn send-message
  [client payload topic qos]
  (let [msg (Paho.MQTT.Message. payload)]
    (set! (.-destinationName msg) topic)
    (set! (.-qos msg) qos)
    (.send client msg)))

(defn on-connect [client done-init<]
  (log :info "Mqtt client connected")
  (a/put! done-init< {:status :ok}))

(defn disconnect [client]
  (log :info "Disconnecting mqtt client")
  (.disconnect client))

(defn connect
  [endpoint client-id on-received done-init<]
  (let [mqtt (Paho.MQTT.Client. endpoint client-id)
        connect-options (js/Object.)]
    (set! (.-onConnectionLost mqtt) (fn [reason-code reason-message]
                                      (log :error "Mqtt Connection Lost" {:reasonCode reason-code
                                                                          :reasonMessage reason-message})))
    (set! (.-onMessageArrived mqtt) (fn [msg]
                                      (when msg (on-received msg))))
    (set! (.-onSuccess connect-options) (fn [] (on-connect mqtt done-init<)))
    (set! (.-onFailure connect-options) (fn [_ _ msg] (log :error "Mqtt Client failed to connect: " msg)))
    (.connect mqtt connect-options)
    mqtt))

(defn mqtt-init
  [mqtt-conf client-id on-received done-init<]
  (connect (get-iot-url (time/now) mqtt-conf) client-id on-received done-init<))

(s/fdef init
        :args (s/cat :mqtt-conf ::mqtt-conf :client-id string? :on-received fn?))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :MQTT nil
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in MQTT SDK module." (:type message)))))

(defn init
  [env done-init< client-id config on-msg-fn]
  (swap! module-state assoc :env env)
  (let [module-inputs< (a/chan 1024)
        mqtt-config (first (filter #(= (:type %) "messaging") (:integrations config)))
        mqtt-config (->> (merge (select-keys mqtt-config [:region :endpoint])
                                (select-keys (:credentials mqtt-config) [:secretKey :accessKey :sessionToken]))
                         (transform-keys camel/->kebab-case-keyword)
                         (#(rename-keys % {:region :region-name})))]
    (u/start-simple-consumer! module-inputs< module-router)
    (mqtt-init mqtt-config client-id on-msg-fn done-init<)
    module-inputs<))
