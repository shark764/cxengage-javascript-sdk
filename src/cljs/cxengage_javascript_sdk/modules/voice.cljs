(ns cxengage-javascript-sdk.modules.voice
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]])
  (:require [cljsjs.paho]
            [cljs.core.async :as a]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.modules.interaction :as int]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs.spec.alpha :as s]))

(s/def ::silent-monitor-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/chosen-extension ::specs/callback]))

(s/def ::generic-voice-interaction-fn-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(s/def ::generic-resource-voice-interaction-fn-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback ::specs/target-resource-id]))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.silentMonitor({
;;   interactionId: "{{uuid}}"
;;   chosenExtension: "{{object}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn silent-monitor
  ""
  {:validation ::silent-monitor-params
   :topic-key :silent-monitoring-start-acknowledged}
  [params]
  (let [{:keys [interaction-id chosen-extension topic callback]} params
        extension (if chosen-extension chosen-extension (first (state/get-all-extensions)))]
    (if extension
      (let [interrupt-type "silent-monitoring"
            interrupt-body {:resource-id (state/get-active-user-id)
                            :active-extension extension
                            :session-id (state/get-session-id)}
            {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
        (if (= status 200)
          (do
            (state/set-monitored-interaction! interaction-id)
            (p/publish {:topics topic
                        :response (merge {:interaction-id interaction-id} interrupt-body)
                        :callback callback}))
          (p/publish {:topics topic
                      :error (e/failed-to-start-silent-monitoring interaction-id interrupt-response)
                      :callback callback})))
      (p/publish {:topics topic
                  :error (e/failed-to-start-silent-monitoring-no-extension interaction-id (state/get-all-extensions))
                  :callback callback}))))

(def-sdk-fn customer-hold
  "Place the customer on hold.
  ```javascript
  CxEngage.interactions.voice.customerHold({
    interactionId: '{{uuid}}'
  });
  ```"
  {:validation ::generic-voice-interaction-fn-params
   :topic-key :hold-acknowledged}
  [params]
  (let [{:keys [interaction-id topic callback]} params
        interrupt-type "customer-hold"
        interrupt-body {:resource-id (state/get-active-user-id)}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-place-customer-on-hold-err interaction-id interrupt-response)
                  :callback callback}))))

(def-sdk-fn customer-resume
  "Take the customer off of hold.
  ```javascript
  CxEngage.interactions.voice.customerResume({
    interactionId: '{{uuid}}'
  });
  ```"
  {:validation ::generic-voice-interaction-fn-params
   :topic-key :resume-acknowledged}
  [params]
  (let [{:keys [interaction-id topic callback]} params
        interrupt-type "customer-resume"
        interrupt-body {:resource-id (state/get-active-user-id)}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-resume-customer-err interaction-id interrupt-response)
                  :callback callback}))))

(def-sdk-fn mute
  "Mute a resource on the call.
  ```javascript
  CxEngage.interactions.voice.mute({
    interactionId: '{{uuid}}',
    targetResourceId: '{{uuid}}' (Optional, defaults to current user id)
  });
  ```"
  {:validation ::generic-resource-voice-interaction-fn-params
   :topic-key :mute-acknowledged}
  [params]
  (let [{:keys [interaction-id target-resource-id topic callback]} params
        resource-id (state/get-active-user-id)
        interrupt-type "mute-resource"
        interrupt-body {:resource-id resource-id
                        :target-resource (or target-resource-id resource-id)}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-mute-target-resource-err interaction-id target-resource-id resource-id interrupt-response)
                  :callback callback}))))

(def-sdk-fn unmute
  "Unumute a resource on the call.
  ```javascript
  CxEngage.interactions.voice.unmute({
    interactionId: '{{uuid}}',
    targetResourceId: '{{uuid}}' (Optional, defaults to current user id)
  });
  ```"
  {:validation ::generic-resource-voice-interaction-fn-params
   :topic-key :unmute-acknowledged}
  [params]
  (let [{:keys [interaction-id target-resource-id topic callback]} params
        resource-id (state/get-active-user-id)
        interrupt-type "unmute-resource"
        interrupt-body {:resource-id resource-id
                        :target-resource (or target-resource-id resource-id)}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-unmute-target-resource-err interaction-id target-resource-id resource-id interrupt-response)
                  :callback callback}))))

(def-sdk-fn resource-hold
  "Place a resource on the call on hold.
  ```javascript
  CxEngage.interactions.voice.resourceHold({
    interactionId: '{{uuid}}',
    targetResourceId: '{{uuid}}' (Optional, defaults to current user id)
  });
  ```"
  {:validation ::generic-resource-voice-interaction-fn-params
   :topic-key :resource-hold-acknowledged}
  [params]
  (let [{:keys [interaction-id target-resource-id topic callback]} params
        resource-id (state/get-active-user-id)
        interrupt-type "resource-hold"
        interrupt-body {:resource-id resource-id
                        :target-resource (or target-resource-id resource-id)}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-place-resource-on-hold-err interaction-id target-resource-id resource-id interrupt-response)
                  :callback callback}))))

(def-sdk-fn resource-resume
  "Take a resource on the call off of hold.
  ```javascript
  CxEngage.interactions.voice.resourceResume({
    interactionId: '{{uuid}}',
    targetResourceId: '{{uuid}}' (Optional, defaults to current user id)
  });
  ```"
  {:validation ::generic-resource-voice-interaction-fn-params
   :topic-key :resource-resume-acknowledged}
  [params]
  (let [{:keys [interaction-id target-resource-id topic callback]} params
        resource-id (state/get-active-user-id)
        interrupt-type "resource-resume"
        interrupt-body {:resource-id resource-id
                        :target-resource (or target-resource-id resource-id)}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-resume-resource-err interaction-id target-resource-id resource-id interrupt-response)
                  :callback callback}))))

(def-sdk-fn resume-all
  "Take all resources on the call off of hold.
  ```javascript
  CxEngage.interactions.voice.resumeAll({
    interactionId: '{{uuid}}'
  });
  ```"
  {:validation ::generic-voice-interaction-fn-params
   :topic-key :resume-all-acknowledged}
  [params]
  (let [{:keys [interaction-id topic callback]} params
        interrupt-type "resume-all"
        interrupt-body {:resource-id (state/get-active-user-id)}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-resume-all-err interaction-id interrupt-response)
                  :callback callback}))))

(def-sdk-fn remove-resource
  "Remove a resource from the call (hang up the resource).
  ```javascript
  CxEngage.interactions.voice.resourceRemove({
    interactionId: '{{uuid}}',
    targetResourceId: '{{uuid}}' (Optional, defaults to current user id)
  });
  ```"
  {:validation ::generic-resource-voice-interaction-fn-params
   :topic-key :resource-removed-acknowledged}
  [params]
  (let [{:keys [interaction-id target-resource-id topic callback]} params
        resource-id (state/get-active-user-id)
        interrupt-type "remove-resource"
        interrupt-body {:resource-id resource-id
                        :target-resource (or target-resource-id resource-id)}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-remove-resource-err interaction-id target-resource-id resource-id interrupt-response)
                  :callback callback}))))

(def-sdk-fn start-recording
  "Start recording.
  ```javascript
  CxEngage.interactions.voice.startRecording({
    interactionId: '{{uuid}}'
  });
  ```"
  {:validation ::generic-voice-interaction-fn-params
   :topic-key :recording-start-acknowledged}
  [params]
  (let [{:keys [interaction-id topic callback]} params
        resource-id (state/get-active-user-id)
        interrupt-type "recording-start"
        interrupt-body {:resource-id resource-id}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-start-recording-err interaction-id resource-id interrupt-response)
                  :callback callback}))))

(def-sdk-fn stop-recording
  "Stop recording.
  ```javascript
  CxEngage.interactions.voice.stopRecording({
    interactionId: '{{uuid}}'
  });
  ```"
  {:validation ::generic-voice-interaction-fn-params
   :topic-key :recording-stop-acknowledged}
  [params]
  (let [{:keys [interaction-id topic callback]} params
        resource-id (state/get-active-user-id)
        interrupt-type "recording-stop"
        interrupt-body {:resource-id resource-id}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-stop-recording-err interaction-id resource-id interrupt-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.transferToResource({
;;   interactionId: "{{uuid}}",
;;   resourceId: "{{uuid}}",
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;

(defn transfer-to-resource [params]
  (log :warn "Function 'CxEngage.interactions.voice.transferToResource' will be deprecated.
              Use 'CxEngage.interactions.transferToResource' instead.")
  (int/transfer-to-resource params))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.transferToQueue({
;;   interactionId: "{{uuid}}",
;;   queueId: "{{uuid}}",
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;

(defn transfer-to-queue [params]
  (log :warn "Function 'CxEngage.interactions.voice.transferToQueue' will be deprecated.
              Use 'CxEngage.interactions.transferToQueue' instead.")
  (int/transfer-to-queue params))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.transferToExtension({
;;   interactionId: "{{uuid}}",
;;   transferExtension: {type: "pstn", value: "+15055555555"},
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;

(defn transfer-to-extension [params]
  (log :warn "Function 'CxEngage.interactions.voice.transferToExtension' will be deprecated.
              Use 'CxEngage.interactions.transferToExtension' instead.")
  (int/transfer-to-extension params))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.cancelResourceTransfer({
;;   interactionId: "{{uuid}}",
;;   targetResourceId: "{{uuid}}",
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;

(defn cancel-resource-transfer [params]
  (log :warn "Function 'CxEngage.interactions.voice.cancelResourceTransfer' will be deprecated.
              Use 'CxEngage.interactions.cancelResourceTransfer' instead.")
  (int/cancel-resource-transfer params))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.cancelQueueTransfer({
;;   interactionId: "{{uuid}}",
;;   transferQueueId: "{{uuid}}",
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;

(defn cancel-queue-transfer [params]
  (log :warn "Function 'CxEngage.interactions.voice.cancelQueueTransfer' will be deprecated.
              Use 'CxEngage.interactions.cancelQueueTransfer' instead.")
  (int/cancel-queue-transfer params))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.cancelExtensionTransfer({
;;   interactionId: "{{uuid}}",
;;   transferExtension: {type: "pstn", value: "+15055555555"},
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;

(defn cancel-extension-transfer [params]
  (log :warn "Function 'CxEngage.interactions.voice.cancelExtensionTransfer' will be deprecated.
              Use 'CxEngage.interactions.cancelExtensionTransfer' instead.")
  (int/cancel-extension-transfer params))

(s/def ::dial-params
  (s/keys :req-un [::specs/phone-number]
          :opt-un [::specs/pop-uri ::specs/outbound-ani ::specs/flow-id ::specs/outbound-identifier-id ::specs/outbound-identifier-list-id ::specs/callback]))

(def-sdk-fn dial
  "Perform an outbound dial.
  ```javascript
  CxEngage.interactions.voice.dial({
    phoneNumber: '{{number}}',
    outboundAni: '{{string}}' (Optional, used for outbound identifier),
    outboundIdentifierId: '{{uuid}}' (Optional, used for outbound identifier),
    outboundIdentifierListId: '{{uuid}}' (Optional, used for outbound identifier),
    flow-id: '{{uuid}}' (Optional, used to specify flow),
    popUri: '{{string}}' (Optional, used for salesforce screen pop)
  });
  ```"
  {:validation ::dial-params
   :topic-key :dial-send-acknowledged}
  [params]
  (let [{:keys [topic phone-number outbound-ani pop-uri flow-id outbound-identifier-id outbound-identifier-list-id callback]} params
        resource-id (state/get-active-user-id)
        session-id (state/get-session-id)
        outbound-integration-type (state/get-outbound-integration-type)
        dial-body {:channel-type "voice"
                   :contact-point (if outbound-ani
                                    outbound-ani
                                    "click to call")
                   :customer phone-number
                   :direction "agent-initiated"
                   :interaction {:resource-id resource-id
                                 :session-id session-id
                                 :pop-uri pop-uri
                                 :outbound-identifier-id outbound-identifier-id
                                 :outbound-identifier-list-id outbound-identifier-list-id}
                   :metadata {}
                   :source outbound-integration-type}
        dial-body (merge dial-body (if flow-id
                                     {:flow-id flow-id}
                                     {}))]
    (let [dial-response (a/<! (rest/create-interaction-request dial-body))
          {:keys [api-response status]} dial-response]
      (if (= status 200)
        (p/publish {:topics topic
                    :response api-response
                    :callback callback})
        (p/publish {:topics topic
                    :error (e/failed-to-perform-outbound-dial-err phone-number dial-response)
                    :callback callback})))))

(s/def ::cancel-dial-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn cancel-dial
  "Cancel an outbound dial (before the interaction has been received).
  ```javascript
  CxEngage.interactions.voice.cancelDial({
    interactionId: '{{uuid}}'
  });
  ```"
  {:validation ::cancel-dial-params
   :topic-key :cancel-dial-acknowledged}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        tenant-id (state/get-active-tenant-id)
        interrupt-body {:resource-id (state/get-active-user-id)}
        interrupt-type "work-cancel"
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-cancel-outbound-dial-err interaction-id interrupt-response)
                  :callback callback}))))

(s/def ::send-digits-params
  (s/keys :req-un [::specs/interaction-id
                   ::specs/digit]
          :opt-un [::specs/callback]))

(def-sdk-fn send-digits
  "Send a DTMF signal to the call (Twilio only).
  ```javascript
  CxEngage.interactions.voice.sendDigits({
    interactionId: '{{uuid}}',
    digit: '{{DTMF digit}}' (1-9, *, or #)
  });
  ```"
  {:validation ::send-digits-params
   :topic-key :send-digits-acknowledged}
  [params]
  (let [connection (state/get-twilio-connection)
        {:keys [interaction-id topic digit callback]} params
        pubsub-response {:interaction-id interaction-id
                         :digit-sent digit}]
    (if-not (and (state/get-integration-by-type "twilio")
                 (= (:provider (state/get-active-extension)) "twilio"))
      (p/publish {:topics (topics/get-topic :no-twilio-integration)
                  :error (e/no-twilio-integration-err)
                  :callback callback})
      (if (and (= :active (state/find-interaction-location interaction-id))
               (= "voice" (:channel-type (state/get-active-interaction interaction-id))))
        (try
          (do (.sendDigits connection digit)
              (p/publish {:topics topic
                          :response pubsub-response
                          :callback callback}))
          (catch js/Object e (p/publish {:topics topic
                                         :error (e/failed-to-send-twilio-digits-err digit)
                                         :callback callback})))
        (p/publish {:topics (topics/get-topic :failed-to-send-digits-invalid-interaction)
                    :error (e/failed-to-send-digits-invalid-interaction-err interaction-id)
                    :callback callback})))))

;; -------------------------------------------------------------------------- ;;
;; SDK Voice Module
;; -------------------------------------------------------------------------- ;;

(defrecord VoiceModule []
  pr/SDKModule
  (start [this]
    (let [module-name :voice]
      (ih/register {:api {:interactions {:voice {:customer-hold customer-hold
                                                 :customer-resume customer-resume
                                                 :mute mute
                                                 :unmute unmute
                                                 :start-recording start-recording
                                                 :stop-recording stop-recording
                                                 :transfer-to-resource transfer-to-resource
                                                 :transfer-to-queue transfer-to-queue
                                                 :transfer-to-extension transfer-to-extension
                                                 :cancel-resource-transfer cancel-resource-transfer
                                                 :cancel-queue-transfer cancel-queue-transfer
                                                 :cancel-extension-transfer cancel-extension-transfer
                                                 :dial dial
                                                 :cancel-dial cancel-dial
                                                 :send-digits send-digits
                                                 :resource-remove remove-resource
                                                 :resource-hold resource-hold
                                                 :resource-resume resource-resume
                                                 :resume-all resume-all
                                                 :silent-monitor silent-monitor}}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
