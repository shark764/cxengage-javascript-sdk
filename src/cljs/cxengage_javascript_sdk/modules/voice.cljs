(ns cxengage-javascript-sdk.modules.voice
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn]])
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

(s/def ::generic-voice-interaction-fn-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(s/def ::generic-resource-voice-interaction-fn-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback ::specs/target-resource-id]))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.silentMonitor({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn silent-monitor
  {:validation ::generic-voice-interaction-fn-params
   :topic-key :silent-monitoring-start-acknowledged}
  [params]
  (let [{:keys [interaction-id topic callback]} params
        extension (first (state/get-all-extensions))]
    (if extension
      (let [interrupt-type "silent-monitoring"
            interrupt-body {:resource-id (state/get-active-user-id)
                            :active-extension extension
                            :session-id (state/get-session-id)}
            {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
        (if (= status 200)
          (p/publish {:topics topic
                      :response (merge {:interaction-id interaction-id} interrupt-body)
                      :callback callback})
          (p/publish {:topics topic
                      :error (e/failed-to-start-silent-monitoring interaction-id interrupt-response)
                      :callback callback})))
      (p/publish {:topics topic
                  :error (e/failed-to-start-silent-monitoring-no-extension interaction-id (state/get-all-extensions))
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.customerHold({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn customer-hold
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.customerResume({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn customer-resume
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.mute({
;;   interactionId: "{{uuid}}",
;;   targetResourceId: "{{uuid}}" (Optional, defaults to current user id)
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn mute
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.unmute({
;;   interactionId: "{{uuid}}",
;;   targetResourceId: "{{uuid}}" (Optional, defaults to current user id)
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn unmute
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.resourceHold({
;;   interactionId: "{{uuid}}",
;;   targetResourceId: "{{uuid}}" (Optional, defaults to current user id)
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn resource-hold
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.resourceResume({
;;   interactionId: "{{uuid}}",
;;   targetResourceId: "{{uuid}}" (Optional, defaults to current user id)
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn resource-resume
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.resumeAll({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn resume-all
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.resourceRemove({
;;   interactionId: "{{uuid}}",
;;   targetResourceId: "{{uuid}}" (Optional, defaults to current user id)
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn remove-resource
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.startRecording({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn start-recording
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.stopRecording({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn stop-recording
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.dial({
;;   phoneNumber: "{{number}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::dial-params
  (s/keys :req-un [::specs/phone-number]
          :opt-un [::specs/callback]))

(def-sdk-fn dial
  {:validation ::dial-params
   :topic-key :dial-send-acknowledged}
  [params]
  (let [{:keys [topic phone-number callback]} params
        resource-id (state/get-active-user-id)
        session-id (state/get-session-id)
        outbound-integration-type (state/get-outbound-integration-type)
        dial-body {:channel-type "voice"
                   :contact-point "click to call"
                   :customer phone-number
                   :direction "outbound"
                   :interaction {:resource-id resource-id
                                 :session-id session-id}
                   :metadata {}
                   :source outbound-integration-type}]
    (let [dial-response (a/<! (rest/create-interaction-request dial-body))
          {:keys [api-response status]} dial-response]
      (if (= status 200)
        (p/publish {:topics topic
                    :response api-response
                    :callback callback})
        (p/publish {:topics topic
                    :error (e/failed-to-perform-outbound-dial-err phone-number dial-response)
                    :callback callback})))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.cancelDial({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::cancel-dial-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn cancel-dial
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.sendDigits({
;;   interactionId: "{{uuid}}",
;;   digit: "{{number}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::send-digits-params
  (s/keys :req-un [::specs/interaction-id
                   ::specs/digit]
          :opt-un [::specs/callback]))

(def-sdk-fn send-digits
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
;; CxEngage.interactions.voice.getRecordings({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(defn get-recording [interaction-id tenant-id artifact-id callback]
  (go (let [audio-recording (a/<! (rest/get-artifact-by-id-request artifact-id interaction-id nil))
            {:keys [api-response status]} audio-recording
            topic (topics/get-topic :recording-response)]
        (if (= status 200)
          (p/publish {:topics topic
                      :response (:files api-response)
                      :callback callback})
          (p/publish {:topics topic
                      :error (e/failed-to-get-specific-recording-err interaction-id artifact-id audio-recording)
                      :callback callback})))))

(s/def ::get-recordings-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-recordings
  {:validation ::get-recordings-params
   :topic-key :recording-response}
  [params]
  (let [{:keys [interaction-id topic callback]} params
        interaction-files (a/<! (rest/get-interaction-artifacts-request interaction-id nil))
        {:keys [api-response status]} interaction-files
        {:keys [results]} api-response
        tenant-id (state/get-active-tenant-id)
        audio-recordings (filterv #(= (:artifact-type %) "audio-recording") results)]
    (if (= (count audio-recordings) 0)
      (p/publish {:topics topic
                  :response []
                  :callback callback})
      (doseq [rec audio-recordings]
        (get-recording interaction-id tenant-id (:artifact-id rec) callback)))))

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
                                                 :get-recordings get-recordings
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
