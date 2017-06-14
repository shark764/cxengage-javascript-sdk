(ns cxengage-javascript-sdk.modules.voice
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]]
                   [cljs-sdk-utils.macros :refer [def-sdk-fn]])
  (:require [cljsjs.paho]
            [cljs.core.async :as a]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cljs-sdk-utils.interop-helpers :as ih]
            [cljs-sdk-utils.specs :as specs]
            [cljs-sdk-utils.topics :as topics]
            [cljs-sdk-utils.protocols :as pr]
            [cljs-sdk-utils.errors :as e]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs.spec :as s]))

(s/def ::generic-voice-interaction-fn-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(s/def ::generic-resource-voice-interaction-fn-params
  (s/keys :req-un [::specs/interaction-id ::specs/target-resource-id]
          :opt-un [::specs/callback]))

(s/def ::extension-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/transfer-extension ::specs/transfer-type]
          :opt-un [::specs/callback]))

(s/def ::queue-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/queue-id ::specs/transfer-type]
          :opt-un [::specs/callback]))

(s/def ::resource-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/resource-id ::specs/transfer-type]
          :opt-un [::specs/callback]))

(s/def ::cancel-resource-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/transfer-resource-id ::specs/transfer-type]
          :opt-un [::specs/callback]))

(s/def ::cancel-queue-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/transfer-queue-id ::specs/transfer-type]
          :opt-un [::specs/callback]))

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
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-place-customer-on-hold-err)
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
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-resume-customer-err)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.mute({
;;   interactionId: "{{uuid}}",
;;   targetResourceId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn mute
  {:validation ::generic-resource-voice-interaction-fn-params
   :topic-key :mute-acknowledged}
  [params]
  (let [{:keys [interaction-id target-resource-id topic callback]} params
        interrupt-type "mute-resource"
        interrupt-body {:resource-id (state/get-active-user-id)
                        :target-resource target-resource-id}
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-mute-target-resource-err)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.unmute({
;;   interactionId: "{{uuid}}",
;;   targetResourceId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn unmute
  {:validation ::generic-resource-voice-interaction-fn-params
   :topic-key :unmute-acknowledged}
  [params]
  (let [{:keys [interaction-id target-resource-id topic callback]} params
        interrupt-type "unmute-resource"
        interrupt-body {:resource-id (state/get-active-user-id)
                        :target-resource target-resource-id}
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-unmute-target-resource-err)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.resourceHold({
;;   interactionId: "{{uuid}}",
;;   targetResourceId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn resource-hold
  {:validation ::generic-resource-voice-interaction-fn-params
   :topic-key :resource-hold-acknowledged}
  [params]
  (let [{:keys [interaction-id target-resource-id topic callback]} params
        interrupt-type "resource-hold"
        interrupt-body {:resource-id (state/get-active-user-id)
                        :target-resource target-resource-id}
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-place-resource-on-hold-err)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.resourceResume({
;;   interactionId: "{{uuid}}",
;;   targetResourceId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn resource-resume
  {:validation ::generic-resource-voice-interaction-fn-params
   :topic-key :resource-resume-acknowledged}
  [params]
  (let [{:keys [interaction-id target-resource-id topic callback]} params
        interrupt-type "resource-resume"
        interrupt-body {:resource-id (state/get-active-user-id)
                        :target-resource target-resource-id}
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-resume-resource-err)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.resourceAll({
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
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-resume-all-err)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.removeResource({
;;   interactionId: "{{uuid}}",
;;   targetResourceId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn remove-resource
  {:validation ::generic-resource-voice-interaction-fn-params
   :topic-key :resource-removed-acknowledged}
  [params]
  (let [{:keys [interaction-id target-resource-id topic callback]} params
        interrupt-type "remove-resource"
        interrupt-body {:resource-id (state/get-active-user-id)
                        :target-resource target-resource-id}
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-remove-resource-err)
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
        interrupt-type "recording-start"
        interrupt-body {:resource-id (state/get-active-user-id)}
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-start-recording-err)
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
        interrupt-type "recording-stop"
        interrupt-body {:resource-id (state/get-active-user-id)}
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-stop-recording-err)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.transferToResource({
;;   interactionId: "{{uuid}}",
;;   resourceId: "{{uuid}}",
;;   transferType: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn transfer-to-resource
  {:validation ::resource-transfer-params
   :topic-key :customer-transfer-acknowledged}
  [params]
  (let [{:keys [interaction-id resource-id transfer-type topic callback]} params
        transfer-type (if (= transfer-type "cold") "cold-transfer" "warm-transfer")
        interrupt-type "customer-transfer"
        interrupt-body {:transfer-resource-id resource-id
                        :resource-id (state/get-active-user-id)
                        :transfer-type transfer-type}
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-transfer-to-resource-err)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.transferToQueue({
;;   interactionId: "{{uuid}}",
;;   queueId: "{{uuid}}",
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn transfer-to-queue
  {:validation ::queue-transfer-params
   :topic-key :customer-transfer-acknowledged}
  [params]
  (let [{:keys [interaction-id queue-id transfer-type topic callback]} params
        transfer-type (if (= transfer-type "cold") "cold-transfer" "warm-transfer")
        interrupt-type "customer-transfer"
        interrupt-body {:transfer-queue-id queue-id
                        :resource-id (state/get-active-user-id)
                        :transfer-type transfer-type}
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-transfer-to-queue-err)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.transferToExtension({
;;   interactionId: "{{uuid}}",
;;   transferExtension: {type: "pstn", value: "+15055555555"},
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn transfer-to-extension
  {:validation ::extension-transfer-params
   :topic-key :customer-transfer-acknowledged}
  [params]
  (let [{:keys [interaction-id transfer-extension transfer-type topic callback]} params
        transfer-type (if (= transfer-type "cold") "cold-transfer" "warm-transfer")
        interrupt-type "customer-transfer"
        interrupt-body {:transfer-extension transfer-extension
                        :resource-id (state/get-active-user-id)
                        :transfer-type transfer-type}
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-transfer-to-extension-err)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.cancelResourceTransfer({
;;   interactionId: "{{uuid}}",
;;   targetResourceId: "{{uuid}}",
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn cancel-resource-transfer
  {:validation ::cancel-resource-transfer-params
   :topic-key :cancel-transfer-acknowledged}
  [params]
  (let [{:keys [interaction-id transfer-resource-id transfer-type topic callback]} params
        transfer-type (if (= transfer-type "cold") "cold-transfer" "warm-transfer")
        interrupt-type "transfer-cancel"
        interrupt-body {:transfer-resource-id transfer-resource-id
                        :resource-id (state/get-active-user-id)
                        :transfer-type transfer-type}
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-cancel-resource-transfer-err)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.cancelQueueTransfer({
;;   interactionId: "{{uuid}}",
;;   transferQueueId: "{{uuid}}",
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn cancel-queue-transfer
  {:validation ::cancel-queue-transfer-params
   :topic-key :cancel-transfer-acknowledged}
  [params]
  (let [{:keys [interaction-id transfer-queue-id transfer-type topic callback]} params
        transfer-type (if (= transfer-type "cold") "cold-transfer" "warm-transfer")
        interrupt-type "transfer-cancel"
        interrupt-body {:transfer-queue-id transfer-queue-id
                        :resource-id (state/get-active-user-id)
                        :transfer-type transfer-type}
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-cancel-queue-transfer-err)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.cancelExtensionTransfer({
;;   interactionId: "{{uuid}}",
;;   transferExtension: {type: "pstn", value: "+15055555555"},
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn cancel-extension-transfer
  {:validation ::extension-transfer-params
   :topic-key :cancel-transfer-acknowledged}
  [params]
  (let [{:keys [interaction-id transfer-extension transfer-type topic callback]} params
        transfer-type (if (= transfer-type "cold") "cold-transfer" "warm-transfer")
        interrupt-type "transfer-cancel"
        interrupt-body {:transfer-extension transfer-extension
                        :resource-id (state/get-active-user-id)
                        :transfer-type transfer-type}
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-cancel-extension-transfer-err)
                  :callback callback}))))

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
        dial-body {:channel-type "voice"
                   :contact-point "click to call"
                   :customer phone-number
                   :direction "outbound"
                   :interaction {:resource-id resource-id}
                   :metadata {}
                   :source "twilio"}]
    (let [dial-response (a/<! (rest/create-interaction-request dial-body))
          {:keys [api-response status]} dial-response]
      (if (= status 200)
        (p/publish {:topics topic
                    :response api-response
                    :callback callback})
        (p/publish {:topics topic
                    :error (e/failed-to-perform-outbound-dial-err)
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
        {:keys [status]} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-cancel-outbound-dial-err)
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
                                         :error (e/failed-to-send-twilio-digits-err)
                                         :callback callback})))
        (p/publish {:topics (topics/get-topic :failed-to-send-digits-invalid-interaction)
                    :error (e/failed-to-send-digits-invalid-interaction-err)
                    :callback callback})))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.getRecordings({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(defn get-recording [interaction-id tenant-id artifact-id callback]
  (go (let [audio-recording (a/<! (rest/get-artifact-by-id-request artifact-id interaction-id))
            {:keys [api-response status]} audio-recording
            topic (topics/get-topic :recording-response)]
        (if (= status 200)
          (p/publish {:topics topic
                      :response (:files api-response)
                      :callback callback})
          (p/publish {:topics topic
                      :error (e/failed-to-get-specific-recording-err)
                      :callback callback})))))

(s/def ::get-recordings-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-recordings
  {:validation ::get-recordings-params
   :topic-key :recording-response}
  [params]
  (let [{:keys [interaction-id topic callback]} params
        interaction-files (a/<! (rest/get-interaction-artifacts-request interaction-id))
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
                                                 :resume-all resume-all}}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
