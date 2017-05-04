(ns cxengage-javascript-sdk.modules.messaging
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljsjs.paho]
            [camel-snake-kebab.core :as camel]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [clojure.set :refer [rename-keys]]
            [cljs-time.core :as time]
            [cljs-time.format :as fmt]
            [cljs-time.instant]
            [goog.crypt :as c]
            [cljs-uuid-utils.core :as id]
            [cxengage-cljs-utils.core :as cxu]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cognitect.transit :as t]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
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
        canonical-request (clojure.string/join "\n" [method canonical-uri canonical-query-string canonical-headers "host" (iu/sha256 "")])]
    canonical-request))

(defn get-credential-scope
  [date region-name service-name]
  (str (get-date-stamp date) "/" region-name "/" service-name "/aws4_request"))

(defn sign-string
  [date {:keys [secret-key region-name]} service-name credential-scope canonical-request]
  (let [string-to-sign (clojure.string/join "\n" [algorithm (get-amz-date-stamp date) credential-scope (iu/sha256 canonical-request)])
        signing-key (iu/get-signature-key secret-key (get-date-stamp date) region-name service-name)]
    (iu/sign signing-key string-to-sign)))

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
  [payload topic callback]
  (let [msg (Paho.MQTT.Message. payload)
        pubsub-topic (p/get-topic :send-message-acknowledged)]
    (set! (.-destinationName msg) topic)
    (set! (.-qos msg) 1)
    (.send (get-mqtt-client) msg)
    (p/publish {:topics pubsub-topic
                :response true
                :callback callback})))

(defn on-connect [done-init<]
  (log :debug "Mqtt client connected")
  (a/put! done-init< {:module-registration-status :success
                      :module :mqtt})
  (p/publish {:topics (p/get-topic :messaging-enabled)
              :response true}))

(defn on-failure [done-init< msg]
  (log :debug "Mqtt Client failed to connect")
  (a/put! done-init< {:module-registration-status :success
                      :module :mqtt
                      :message msg}))

(defn disconnect [client]
  (log :debug "Disconnecting mqtt client")
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

(defn subscribe-to-messaging-interaction [message]
  (let [{:keys [tenant-id interaction-id env]} message
        topic (str (name env) "/tenants/" tenant-id "/channels/" interaction-id)]
    (subscribe topic)))

(defn unsubscribe-from-messaging-interaction* [message])

(defn gen-payload [message]
  (let [{:keys [message resource-id tenant-id interaction-id]} message
        uid (str (id/make-random-uuid))
        metadata {:name "Agent"
                  :first-name "Agent"
                  :last-name "Agent"
                  :type "agent"}
        body {:text message}]
    {:id uid
     :tenant-id tenant-id
     :type "message"
     :to interaction-id
     :from resource-id
     :metadata metadata
     :body body
     :timestamp (fmt/unparse (fmt/formatters :date-time) (time/now))}))

(defn format-payload [message]
  (t/write (t/writer :json-verbose) (clojure.walk/stringify-keys message)))

(s/def ::message string?)
(s/def ::interaction-id string?)
(s/def ::send-message-params
  (s/keys :req-un [::interaction-id
                   ::message]
          :opt-un [::specs/callback]))

(defn send-message
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (send-message module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [module-state @(:state module)
         params (iu/extract-params params)
         {:keys [interaction-id callback]} params
         topic (p/get-topic :send-message-acknowledged)
         tenant-id (state/get-active-tenant-id)
         payload (assoc params
                        :resource-id (state/get-active-user-id)
                        :tenant-id tenant-id)]
     (if-not (s/valid? ::send-message-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::send-message-params params))
                   :callback callback})
       (let [payload (-> payload
                         (gen-payload)
                         (format-payload))
             mqtt-topic (str (name (state/get-env)) "/tenants/" tenant-id "/channels/" interaction-id)]
         (send-message-impl payload mqtt-topic callback)))
     nil)))

(defn get-transcript [interaction-id tenant-id artifact-id callback]
  (go (let [transcript (a/<! (iu/get-artifact interaction-id tenant-id artifact-id))
            {:keys [api-response status]} transcript]
        (if-not (= status 200)
          (p/publish {:topics (p/get-topic :transcript-response)
                      :response (e/api-error "api returned error")
                      :callback callback})
          (p/publish {:topics (p/get-topic :transcript-response)
                      :response (:files api-response)
                      :callback callback})))))

(s/def ::send-sms-by-interrupt-params
  (s/keys :req-un [::specs/message
                   ::specs/interaction-id]
          :opt-un [::specs/callback]))

(defn send-sms-by-interrupt
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (send-sms-by-interrupt module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [{:keys [interaction-id message callback] :as params} (iu/extract-params params)
         tenant-id (state/get-active-tenant-id)
         topic (p/get-topic :send-outbound-sms-response)
         api-url (get-in module [:config :api-url])
         sms-url (str api-url "tenants/tenant-id/interactions/interaction-id/interrupts")
         interrupt-body {:interrupt-type "send-sms"
                         :interrupt {:message message}
                         :source "Client"}
         sms-request {:method :post
                      :url (iu/build-api-url-with-params
                            sms-url
                            {:tenant-id tenant-id
                             :interaction-id interaction-id})
                      :body interrupt-body}]
     (if-not (s/valid? ::send-sms-by-interrupt-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::send-sms-by-interrupt-params params))
                   :callback callback})
       (do (go (let [sms-response (a/<! (iu/api-request sms-request))
                     {:keys [api-response status]} sms-response]
                 (if (not= status 200)
                   (p/publish {:topics topic
                               :error (e/api-error api-response)
                               :callback callback})
                   (p/publish {:topics topic
                               :response {:interaction-id interaction-id}
                               :callback callback}))))
           nil)))))

(s/def ::click-to-sms-params
  (s/keys :req-un [::specs/phone-number
                   ::specs/message]
          :opt-on [::specs/callback]))

(defn click-to-sms
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (click-to-sms module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         {:keys [phone-number message callback]} params
         phone-number (iu/normalize-phone-number phone-number)
         tenant-id (state/get-active-tenant-id)
         resource-id (state/get-active-user-id)
         interaction-id (str (id/make-random-uuid))
         topic (p/get-topic :initialize-outbound-sms-response)
         api-url (get-in module [:config :api-url])
         sms-url (str api-url "tenants/tenant-id/interactions")
         metadata {:customer phone-number
                   :customer-name "SMS User"
                   :channel-type "sms"
                   :source "twilio"}
         sms-body {:id interaction-id
                   :source "messaging"
                   :customer phone-number
                   :contact-point "outbound"
                   :channel-type "sms"
                   :direction "outbound"
                   :metadata metadata
                   :interaction {:message message
                                 :resource-id resource-id}}
         sms-request {:method :post
                      :url (iu/build-api-url-with-params
                            sms-url
                            {:tenant-id tenant-id})
                      :body sms-body}]
     (if-not (s/valid? ::click-to-sms-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::click-to-sms-params params))
                   :callback callback})
       (do (go (let [sms-response (a/<! (iu/api-request sms-request))
                     {:keys [api-response status]} sms-response]
                 (if (not= status 200)
                   (p/publish {:topics topic
                               :error (e/api-error api-response)
                               :callback callback})
                   (p/publish {:topics topic
                               :response api-response
                               :callback callback}))))
           nil)))))

(defn get-transcripts
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (get-transcripts module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         {:keys [interaction-id callback]} params]
     (go (let [interaction-files (a/<! (iu/get-interaction-files interaction-id))
               {:keys [api-response status]} interaction-files
               {:keys [results]} api-response
               tenant-id (state/get-active-tenant-id)
               transcripts (filterv #(= (:artifact-type %) "messaging-transcript") results)]
           (if-not (= status 200)
             (p/publish {:topics (p/get-topic :transcript-response)
                         :response (e/api-error "api returned error")
                         :callback callback})
             (if (= (count transcripts) 0)
               (p/publish {:topics (p/get-topic :transcript-response)
                           :response []
                           :callback callback})
               (doseq [t transcripts]
                 (get-transcript interaction-id tenant-id (:artifact-id t) callback))))))
     nil)))


(def initial-state
  {:module-name :messaging
   :urls {}})

(defrecord MessagingModule [config state core-messages< on-msg-fn]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)
          client-id (state/get-active-user-id)
          mqtt-integration (state/get-integration-by-type "messaging")
          mqtt-integration (->> (merge (select-keys mqtt-integration [:region :endpoint])
                                       (select-keys (:credentials mqtt-integration) [:secret-key :access-key :session-token]))
                                (transform-keys camel/->kebab-case-keyword)
                                (#(rename-keys % {:region :region-name})))]
      (if-not mqtt-integration
        (a/put! core-messages< {:module-registration-status :failure
                                :module module-name})
        (do (mqtt-init mqtt-integration client-id on-msg-fn core-messages<)
            (register {:api {:interactions {:messaging {:send-message (partial send-message this)
                                                        :get-transcripts (partial get-transcripts this)
                                                        :initialize-outbound-sms (partial click-to-sms this)
                                                        :send-outbound-sms (partial send-sms-by-interrupt this)}}}
                       :module-name module-name})
            (log :info (str "<----- Started " (name module-name) " SDK module! ----->"))))))
  (stop [this])
  (refresh-integration [this]))
