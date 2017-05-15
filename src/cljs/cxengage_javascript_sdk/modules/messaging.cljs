(ns cxengage-javascript-sdk.modules.messaging
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cxengage-javascript-sdk.macros :refer [def-sdk-fn]])
  (:require [cljsjs.paho]
            [camel-snake-kebab.core :as camel]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cljs.core.async :as a]
            [clojure.set :refer [rename-keys]]
            [cljs-time.core :as time]
            [cljs-time.format :as fmt]
            [cljs-time.instant]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.interop-helpers :as ih]
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
  (js/console.log (str "Subscribed to MQTT topic: " topic)))

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

(defn on-connect []
  (js/console.log "Mqtt client connected"))

(defn on-failure [msg]
  (js/console.log "Mqtt Client failed to connect " msg)
  (p/publish {:topics (p/get-topic :mqtt-failed-to-connect)
              :error (e/failed-to-connect-to-mqtt-err)})
  (ih/send-core-message {:type :module-registration-status
                         :status :failure
                         :module-name :messaging}))

(defn disconnect [client]
  (js/console.log "Disconnecting mqtt client")
  (.disconnect client))

(defn connect
  [endpoint client-id on-received]
  (let [mqtt (Paho.MQTT.Client. endpoint client-id)
        connect-options (js/Object.)]
    (set! (.-onConnectionLost mqtt) (fn [reason-code reason-message]
                                      (js/console.error "Mqtt Connection Lost" {:reasonCode reason-code
                                                                                :reasonMessage reason-message})))
    (set! (.-onMessageArrived mqtt) (fn [msg]
                                      (when msg (on-received msg))))
    (set! (.-onSuccess connect-options) (fn [] (on-connect)))
    (set! (.-onFailure connect-options) (fn [_ _ msg]
                                          (on-failure msg)))
    (.connect mqtt connect-options)
    mqtt))

(s/fdef init
        :args (s/cat :mqtt-conf ::mqtt-conf :client-id string? :on-received fn?))

(defn mqtt-init
  [mqtt-conf client-id on-received]
  (let [mqtt-client (connect (get-iot-url (time/now) mqtt-conf) client-id on-received)]
    (swap! module-state assoc :mqtt-client mqtt-client)))

(defn subscribe-to-messaging-interaction [message]
  (let [{:keys [tenant-id interaction-id env]} message
        topic (str (name env) "/tenants/" tenant-id "/channels/" interaction-id)]
    (subscribe topic)))

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

;; ----------------------------------------------------------------;;
;; CxEngage.interactions.messaging.sendMessage({
;;   interactionId: "{{interaction-id}}",
;;   message: "The message you want to send"
;; });
;; ----------------------------------------------------------------;;

(s/def ::send-message-params
  (s/keys :req-un [::specs/interaction-id
                   ::specs/message]
          :opt-un [::specs/callback]))

(def-sdk-fn send-message
  ::send-message-params
  (p/get-topic :send-message-acknowledged)
  [params]
  (let [{:keys [interaction-id message topic callback]} params
        tenant-id (state/get-active-tenant-id)
        payload (-> {:resource-id (state/get-active-user-id)
                     :tenant-id tenant-id
                     :interaction-id interaction-id
                     :message message}
                    (gen-payload)
                    (format-payload))
        mqtt-topic (str (name (state/get-env)) "/tenants/" tenant-id "/channels/" interaction-id)]
    (send-message-impl payload mqtt-topic callback)))

;; ----------------------------------------------------------------;;
;; CxEngage.interactions.messaging.sendOutboundSms({
;;   interactionId: "{{interaction-id}}",
;;   message: "The message you want to send"
;; });
;; ----------------------------------------------------------------;;

(s/def ::send-sms-by-interrupt-params
  (s/keys :req-un [::specs/interaction-id
                   ::specs/message]
          :opt-un [::specs/callback]))

(def-sdk-fn send-sms-by-interrupt
  ::send-sms-by-interrupt-params
  (p/get-topic :send-outbound-sms-response)
  [params]
  (let [{:keys [interaction-id message topic callback]} params
        tenant-id (state/get-active-tenant-id)
        interrupt-body {:interrupt-type "send-sms"
                        :interrupt {:message message}
                        :source "Client"}
        sms-request {:method :post
                     :url (iu/api-url
                           "tenants/tenant-id/interactions/interaction-id/interrupts"
                           {:tenant-id tenant-id
                            :interaction-id interaction-id})
                     :body interrupt-body}
        sms-response (a/<! (iu/api-request sms-request))
        {:keys [api-response status]} sms-response]
    (when (= status 200)
      (p/publish {:topics topic
                  :response {:interaction-id interaction-id}
                  :callback callback}))))

;; ----------------------------------------------------------------;;
;; CxEngage.interactions.messaging.initializeOutboundSms({
;;   phoneNumber: "+18005555555",
;;   message: "The message you want to send"
;; });
;; ----------------------------------------------------------------;;


(s/def ::click-to-sms-params
  (s/keys :req-un [::specs/phone-number
                   ::specs/message]
          :opt-on [::specs/callback]))

(def-sdk-fn click-to-sms
  ::click-to-sms-params
  (p/get-topic :initialize-outbound-sms-response)
  [params]
  (let [{:keys [phone-number message topic callback]} params
        phone-number (iu/normalize-phone-number phone-number)
        tenant-id (state/get-active-tenant-id)
        resource-id (state/get-active-user-id)
        interaction-id (str (id/make-random-uuid))
        metadata {:customer phone-number
                  :customer-name "SMS User"
                  :channel-type "sms"
                  :name "Agent"
                  :type "agent"
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
                     :url (iu/api-url
                           "tenants/tenant-id/interactions"
                           {:tenant-id tenant-id})
                     :body sms-body}
        sms-response (a/<! (iu/api-request sms-request))
        {:keys [api-response status]} sms-response]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback}))))

;; ----------------------------------------------------------------;;
;; CxEngage.interactions.messaging.getTranscripts({
;;   interactionId: "{{interaction-id}}"
;; });
;; ----------------------------------------------------------------;;

(s/def ::get-transcripts-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(defn get-transcript [interaction-id tenant-id artifact-id callback]
  (go (let [transcript (a/<! (iu/get-artifact interaction-id tenant-id artifact-id))
            {:keys [api-response status]} transcript]
        (when (= status 200)
          (p/publish {:topics (p/get-topic :transcript-response)
                      :response (:files api-response)
                      :callback callback})))))

(def-sdk-fn get-transcripts
  ::get-transcripts-params
  (p/get-topic :transcript-response)
  [params]
  (let [{:keys [interaction-id topic callback]} params
        interaction-files (a/<! (iu/get-interaction-files interaction-id))
        {:keys [api-response status]} interaction-files
        {:keys [results]} api-response
        tenant-id (state/get-active-tenant-id)
        transcripts (filterv #(= (:artifact-type %) "messaging-transcript") results)]
    (when (= status 200)
      (if (= (count transcripts) 0)
        (p/publish {:topics topic
                    :response []
                    :callback callback})
        (doseq [t transcripts]
          (get-transcript interaction-id tenant-id (:artifact-id t) callback))))))

;; -------------------------------------------------------------------------- ;;
;; SDK Messaging Module
;; -------------------------------------------------------------------------- ;;

(defrecord MessagingModule [on-msg-fn]
  pr/SDKModule
  (start [this]
    (let [module-name :messaging
          client-id (state/get-active-user-id)
          mqtt-integration (state/get-integration-by-type "messaging")
          mqtt-integration (->> (merge (select-keys mqtt-integration [:region :endpoint])
                                       (select-keys
                                        (:credentials mqtt-integration)
                                        [:secret-key :access-key :session-token]))
                                (transform-keys camel/->kebab-case-keyword)
                                (#(rename-keys % {:region :region-name})))]
      (if-not mqtt-integration
        (ih/send-core-message {:type :module-registration-status
                               :status :failure
                               :module-name module-name})
        (do (mqtt-init mqtt-integration client-id on-msg-fn)
            (ih/register {:api {:interactions {:messaging {:send-message send-message
                                                           :get-transcripts get-transcripts
                                                           :initialize-outbound-sms click-to-sms
                                                           :send-outbound-sms send-sms-by-interrupt}}}
                          :module-name module-name})
            (ih/send-core-message {:type :module-registration-status
                                   :status :success
                                   :module-name module-name})))))
  (stop [this])
  (refresh-integration [this]))
