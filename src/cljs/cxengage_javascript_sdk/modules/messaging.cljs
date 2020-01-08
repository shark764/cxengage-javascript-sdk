(ns cxengage-javascript-sdk.modules.messaging
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]])
  (:require [cljsjs.paho]
            [camel-snake-kebab.core :as camel]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cljs.core.async :as a]
            [clojure.set :refer [rename-keys]]
            [cljs-time.core :as time]
            [cljs-time.format :as fmt]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cognitect.transit :as t]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs.spec.alpha :as s])
  (:import goog.crypt))

(def ^:no-doc service-name "iotdevicegateway")
(def ^:no-doc algorithm "AWS4-HMAC-SHA256")
(def ^:no-doc method "GET")
(def ^:no-doc canonical-uri "/mqtt")

(s/def ::mqtt-conf (s/keys :req-un [::endpoint ::region-name ::secret-key ::access-key]
                           :opt-un [::session-token]))

(def ^:no-doc get-host
  (memoize
   (fn [endpoint region-name]
     (str endpoint ".iot." region-name ".amazonaws.com"))))

(def ^:no-doc get-date-stamp
  (memoize
   (fn [date]
     (fmt/unparse (fmt/formatter "yyyyMMdd") date))))

(def ^:no-doc get-amz-date-stamp
  (memoize
   (fn [date]
     (fmt/unparse (fmt/formatter "yyyyMMddTHHmmssZ") date))))

(defn- get-canonical-query-string
  [date access-key credential-scope]
  (str "X-Amz-Algorithm=AWS4-HMAC-SHA256"
       "&X-Amz-Credential=" (js/encodeURIComponent (str access-key "/" credential-scope))
       "&X-Amz-Date=" (get-amz-date-stamp date)
       "&X-Amz-Expires=86400"
       "&X-Amz-SignedHeaders=host"))

(defn- get-canonical-request
  [date access-key credential-scope host]
  (let [canonical-query-string (get-canonical-query-string date access-key credential-scope)
        canonical-headers (str "host:" host "\n")
        canonical-request (clojure.string/join "\n" [method canonical-uri canonical-query-string canonical-headers "host" (iu/sha256 "")])]
    canonical-request))

(defn- get-credential-scope
  [date region-name service-name]
  (str (get-date-stamp date) "/" region-name "/" service-name "/aws4_request"))

(defn- sign-string
  [date {:keys [secret-key region-name]} service-name credential-scope canonical-request]
  (let [string-to-sign (clojure.string/join "\n" [algorithm (get-amz-date-stamp date) credential-scope (iu/sha256 canonical-request)])
        signing-key (iu/get-signature-key secret-key (get-date-stamp date) region-name service-name)]
    (iu/sign signing-key string-to-sign)))

(defn- get-iot-url
  [date {:keys [endpoint region-name access-key secret-key session-token] :as mqtt-conf}]
  (let [host (get-host endpoint region-name)
        credential-scope (get-credential-scope date region-name service-name)
        canonical-request (get-canonical-request date access-key credential-scope host)
        signature (sign-string date mqtt-conf service-name credential-scope canonical-request)
        canonical-query-string (str (get-canonical-query-string date access-key credential-scope) "&X-Amz-Signature=" signature)
        security-token-string (when-not (clojure.string/blank? session-token)
                                (str "&X-Amz-Security-Token=" (js/encodeURIComponent session-token)))]
    (str "wss://" host canonical-uri "?" canonical-query-string security-token-string)))

(defn- subscribe
  [topic]
  (.subscribe (state/get-mqtt-client) topic #js {:qos 1})
  (log :info (str "Subscribed to MQTT topic: " topic)))

(defn- unsubscribe
  [topic]
  (.unsubscribe (state/get-mqtt-client) topic))

(defn- send-message-impl
  [payload topic callback]
  (let [msg (Paho.MQTT.Message. payload)
        pubsub-topic (topics/get-topic :send-message-acknowledged)]
    (set! (.-destinationName msg) topic)
    (set! (.-qos msg) 1)
    (.send (state/get-mqtt-client) msg)
    (p/publish {:topics pubsub-topic
                :response true
                :callback callback})))

(defn- send-action-impl
  [payload topic action-topic callback]
  (let [msg (Paho.MQTT.Message. payload)
        pubsub-topic (topics/get-topic action-topic)]
    (set! (.-destinationName msg) topic)
    (set! (.-qos msg) 1)
    (.send (state/get-mqtt-client) msg)
    (p/publish {:topics pubsub-topic
                :response true
                :callback callback})))

(defn- on-connect []
  (log :debug "Mqtt client connected"))

(defn- on-failure [msg]
  (log :error "Mqtt Client failed to connect " msg)
  (p/publish {:topics (topics/get-topic :mqtt-failed-to-connect)
              :error (e/failed-to-connect-to-mqtt-err msg)})
  (ih/send-core-message {:type :module-registration-status
                         :status :failure
                         :module-name :messaging}))

(defn- disconnect [client]
  (log :debug "Disconnecting mqtt client")
  (.disconnect client))

(defn- connect
  [endpoint client-id on-received]
  (let [mqtt (Paho.MQTT.Client. endpoint client-id)
        connect-options (js/Object.)]
    (set! (.-reconnect connect-options) true)
    (set! (.-onConnectionLost mqtt) (fn [reason-code reason-message]
                                      (if (zero? (get (js->clj reason-code :keywordize-keys true) :errorCode))
                                        (log :info "Previous Mqtt Session Successfully Disconnected")
                                        (do
                                          (p/publish {:topics (topics/get-topic :mqtt-lost-connection)
                                                      :error (e/mqtt-connection-lost-err {:reason-code reason-code
                                                                                          :reason-message reason-message})})
                                          (log :error "Mqtt Connection Lost")
                                          (log :error (clj->js {:reasonCode reason-code
                                                                :reasonMessage reason-message}))))))
    (set! (.-onMessageArrived mqtt) (fn [msg]
                                      (log :debug "Raw msg received from MQTT:" msg)
                                      (when msg (on-received msg))))
    (set! (.-onSuccess connect-options) on-connect)
    (set! (.-onFailure connect-options) (fn [_ _ msg]
                                          (on-failure msg)))
    (.connect mqtt connect-options)
    mqtt))

(s/fdef init
        :args (s/cat :mqtt-conf ::mqtt-conf :client-id string? :on-received fn?))

(defn- mqtt-init
  [mqtt-conf client-id on-received]
  (let [mqtt-client (connect (get-iot-url (time/now) mqtt-conf) client-id on-received)]
    (state/set-mqtt-client mqtt-client)))

(defn- subscribe-to-messaging-interaction [message]
  (let [{:keys [tenant-id interaction-id env]} message
        topic (str (name env) "/tenants/" tenant-id "/channels/" interaction-id)]
    (log :debug "Subscribing to topic:" topic)
    (subscribe topic)))

(defn- unsubscribe-to-messaging-interaction [message]
  (let [{:keys [tenant-id interaction-id env]} message
        topic (str (name env) "/tenants/" tenant-id "/channels/" interaction-id)]
    (log :debug "Unsubscribing from topic:" topic)
    (unsubscribe topic)))

(defn- gen-payload [message]
  (let [{:keys [message action-type resource-id tenant-id interaction-id]} message
        uid (str (id/make-random-uuid))
        message-body (if action-type action-type message)
        metadata {:name "Agent"
                  :first-name "Agent"
                  :last-name "Agent"
                  :type "agent"}
        body {:text message-body}]
    {:id uid
     :tenant-id tenant-id
     :type (if action-type action-type "message")
     :to interaction-id
     :from resource-id
     :metadata metadata
     :body body
     :timestamp (fmt/unparse (fmt/formatters :date-time) (time/now))}))

(defn- format-payload [message]
  (t/write (t/writer :json-verbose) (clojure.walk/stringify-keys message)))

(def-sdk-fn send-smooch-message
  "``` javascript
  CxEngage.interactions.messaging.sendSmoochMessage({
    interactionId: {{uuid}}, (required)
    message: {{string}}, (required)
  });
  ```

  Sends a message to all participants in the interaction.

  Topic: cxengage/interactions/messaging/send-smooch-message"
  {:validation ::send-message-params
   :topic-key :smooch-message-received}
  [params]
  (let [{:keys [interaction-id message topic callback]} params
        smooch-response (a/<! (rest/send-smooch-message interaction-id message))
        {:keys [api-response status]} smooch-response]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-send-smooch-message interaction-id message)
                  :callback callback}))))

(s/def ::send-smooch-conversation-read-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn send-smooch-conversation-read
  "``` javascript
    CxEngage.interactions.messaging.sendSmoochConversationRead({
      interactionId: {{uuid}}, (required)
    });
  ```
  Sends a conversation read event to all participants in the interaction.

  Topic: cxengage/interactions/messaging/smooch-conversation-read-received

  Possible Errors:

  - [Messaging: 6008] (/cxengage-javascript-sdk.domain.errors.html#var-failed-to-send-smooch-conversation-read)"
  {:validation ::send-smooch-conversation-read-params
   :topic-key :smooch-conversation-read-received}
  [params]
  (let [{:keys [interaction-id topic callback]} params
        smooch-response (a/<! (rest/send-smooch-conversation-read interaction-id))
        {:keys [api-response status]} smooch-response
        error (if-not (= 200 status) (e/failed-to-send-smooch-conversation-read interaction-id))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

(s/def ::send-smooch-typing-indicator-params
  (s/keys :req-un [::specs/interaction-id
                   ::specs/typing]
          :opt-un [::specs/callback]))

(def-sdk-fn send-smooch-typing-indicator
  "``` javascript
    CxEngage.interactions.messaging.sendSmoochTypingIndicator({
      interactionId: {{uuid}}, (required)
      typing: {{boolean}}, (required)
    });
  ```
  Sends a typing indicator to all participants in the interaction.

  Topic: cxengage/interactions/messaging/smooch-typing-received

  Possible Errors:

  - [Messaging: 6009] (/cxengage-javascript-sdk.domain.errors.html#var-failed-to-send-smooch-typing)"
  {:validation ::send-smooch-typing-indicator-params
   :topic-key :smooch-typing-agent-received}
  [params]
  (let [{:keys [interaction-id typing topic callback]} params
        smooch-response (a/<! (rest/send-smooch-typing interaction-id typing))
        {:keys [api-response status]} smooch-response
        error (if-not (= status 200) (e/failed-to-send-smooch-typing interaction-id typing))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

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
  ""
  {:validation ::send-message-params
   :topic-key :send-message-acknowledged}
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

(s/def ::set-typing-indicator-params
  (s/keys :req-un [::specs/interaction-id ::specs/enable-indicator]
          :opt-un [::specs/callback]))

(def-sdk-fn set-typing-indicator
  "``` javascript
  CxEngage.interactions.messaging.setTypingIndicator({
    interactionId: {{uuid}}, (required)
    enableIndicator: {{boolean}}, (required)
  });
  ```
  Sends a **typing on** or **typing off** sender action.

  Topic: cxengage/interactions/messaging/set-typing-indicator"
  {:validation ::set-typing-indicator-params
   :topic-key :set-typing-indicator}
  [params]
  (let [{:keys [interaction-id topic enable-indicator callback]} params
        action-type (if enable-indicator "typing_on" "typing_off") 
        tenant-id (state/get-active-tenant-id)
        payload (-> {:resource-id (state/get-active-user-id)
                     :tenant-id tenant-id
                     :interaction-id interaction-id
                     :action-type action-type}
                    (gen-payload)
                    (format-payload))
        
        mqtt-topic (str (name (state/get-env)) "/tenants/" tenant-id "/channels/" interaction-id)]
    (send-action-impl payload mqtt-topic :set-typing-indicator callback)))

(s/def ::mark-as-seen-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn mark-as-seen
   "``` javascript
  CxEngage.interactions.messaging.markAsSeen({
    interactionId: {{uuid}}, (required)
  });
  ```
  Sends a **mark seen** sender action.

  Topic: cxengage/interactions/messaging/mark-as-seen"
  {:validation ::mark-as-seen-params
   :topic-key :mark-as-seen}
  [params]
  (let [{:keys [interaction-id topic callback]} params
        tenant-id (state/get-active-tenant-id)
        payload (-> {:resource-id (state/get-active-user-id)
                     :tenant-id tenant-id
                     :interaction-id interaction-id
                     :action-type "mark_seen"}
                    (gen-payload)
                    (format-payload))
        mqtt-topic (str (name (state/get-env)) "/tenants/" tenant-id "/channels/" interaction-id)]
    (send-action-impl payload mqtt-topic :mark-as-seen callback)))

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
  ""
  {:validation ::send-sms-by-interrupt-params
   :topic-key :send-outbound-sms-response}
  [params]
  (let [{:keys [interaction-id message topic callback]} params
        tenant-id (state/get-active-tenant-id)
        interrupt-body {:message message}
        sms-response (a/<! (rest/send-interrupt-request interaction-id "send-sms" interrupt-body))
        {:keys [api-response status]} sms-response]
    (if (= status 200)
      (p/publish {:topics topic
                  :response {:interaction-id interaction-id}
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-send-outbound-sms-err interaction-id message sms-response)
                  :callback callback}))))

;; ----------------------------------------------------------------;;
;; CxEngage.interactions.messaging.initializeOutboundSms({
;;   phoneNumber: "+18005555555",
;;   message: "The message you want to send"
;;   popUri: "{{string}}" (Optional, used for salesforce screen pop)
;;   outboundAni: "{{string}}" (Optional)-- outbound
;;   flowId: "{{string}}" (Optional)
;; });
;; ----------------------------------------------------------------;;

(s/def ::click-to-sms-params
  (s/keys :req-un [::specs/phone-number
                   ::specs/message]
          :opt-on [::specs/pop-uri ::specs/callback ::specs/outbound-ani ::specs/flow-id ::specs/outbound-identifier-id ::specs/outbound-identifier-list-id]))

(def-sdk-fn click-to-sms
  ""
  {:validation ::click-to-sms-params
   :topic-key :initialize-outbound-sms-response}
  [params]
  (let [{:keys [phone-number message pop-uri topic callback outbound-ani flow-id outbound-identifier-id outbound-identifier-list-id]} params
        phone-number (iu/normalize-phone-number phone-number)
        tenant-id (state/get-active-tenant-id)
        resource-id (state/get-active-user-id)
        session-id (state/get-session-id)
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
                  :contact-point (if outbound-ani 
                                     outbound-ani
                                     "outbound")
                  :channel-type "sms"
                  :direction "agent-initiated"
                  :metadata metadata
                  :interaction {:message message
                                :resource-id resource-id
                                :session-id session-id
                                :pop-uri pop-uri
                                :outbound-identifier-id outbound-identifier-id
                                :outbound-identifier-list-id outbound-identifier-list-id}}
          sms-body (merge sms-body (if flow-id 
                                      {:flow-id flow-id} 
                                      {}))
        sms-response (a/<! (rest/create-interaction-request sms-body))
        {:keys [api-response status]} sms-response]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-outbound-sms-interaction-err phone-number message sms-response)
                  :callback callback}))))

;; NOTE: THIS HAS BEEN ADDED TO THE REPORTING NAMESPACE AND WILL SOON BE DEPRECATED
;; ----------------------------------------------------------------;;
;; CxEngage.interactions.messaging.getTranscripts({
;;   interactionId: "{{interaction-id}}"
;; });
;; ----------------------------------------------------------------;;

(s/def ::get-transcripts-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(defn- get-transcript [interaction-id tenant-id artifact-id callback]
  (go (let [transcript (a/<! (rest/get-artifact-by-id-request artifact-id interaction-id nil))
            {:keys [api-response status]} transcript
            topic (topics/get-topic :transcript-response)]
        (if (= status 200)
          (p/publish {:topics topic
                      :response (:files api-response)
                      :callback callback})
          (p/publish {:topics topic
                      :error (e/failed-to-get-specific-messaging-transcript-err interaction-id artifact-id transcript)
                      :callback callback})))))

(def-sdk-fn get-transcripts
  ""
  {:validation ::get-transcripts-params
   :topic-key :transcript-response}
  [params]
  (let [{:keys [interaction-id topic callback]} params
        interaction-files (a/<! (rest/get-interaction-artifacts-request interaction-id nil))
        {:keys [api-response status]} interaction-files
        {:keys [results]} api-response
        tenant-id (state/get-active-tenant-id)
        transcripts (filterv #(= (:artifact-type %) "messaging-transcript") results)]
    (if (= status 200)
      (if (= (count transcripts) 0)
        (p/publish {:topics topic
                    :response []
                    :callback callback})
        (doseq [t transcripts]
          (get-transcript interaction-id tenant-id (:artifact-id t) callback)))
      (p/publish {:topics topic
                  :error (e/failed-to-get-messaging-transcripts-err interaction-id interaction-files)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; SDK Messaging Module
;; -------------------------------------------------------------------------- ;;

(defrecord MessagingModule [on-msg-fn]
  pr/SDKModule
  (start [this]
    (let [module-name :messaging
          client-id (state/get-active-user-id)
          mqtt-integration (state/get-integration-by-type "messaging")]
      (if-not mqtt-integration
        (ih/send-core-message {:type :module-registration-status
                               :status :failure
                               :module-name module-name})
        (let [formatted-integration (->> (merge (select-keys mqtt-integration [:region :endpoint])
                                                (select-keys
                                                 (:credentials mqtt-integration)
                                                 [:secret-key :access-key :session-token]))
                                         (transform-keys camel/->kebab-case-keyword)
                                         (#(rename-keys % {:region :region-name})))]
          (do (mqtt-init formatted-integration client-id on-msg-fn)
              (ih/register {:api {:interactions {:messaging {:send-smooch-message send-smooch-message
                                                             :send-smooch-conversation-read send-smooch-conversation-read
                                                             :send-smooch-typing-indicator send-smooch-typing-indicator
                                                             :send-message send-message
                                                             :get-transcripts get-transcripts
                                                             :initialize-outbound-sms click-to-sms
                                                             :send-outbound-sms send-sms-by-interrupt
                                                             :mark-as-seen mark-as-seen
                                                             :set-typing-indicator set-typing-indicator}}}
                            :module-name module-name})
              (ih/send-core-message {:type :module-registration-status
                                     :status :success
                                     :module-name module-name}))))))
  (stop [this])
  (refresh-integration [this]))
