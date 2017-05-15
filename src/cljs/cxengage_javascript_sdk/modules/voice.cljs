(ns cxengage-javascript-sdk.modules.voice
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cxengage-javascript-sdk.macros :refer [def-sdk-fn]])
  (:require [cljsjs.paho]
            [cljs.core.async :as a]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs.spec :as s]))

(s/def ::generic-voice-interaction-fn-params
  (s/keys :req-un [::specs/interaction-id]
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

(defn send-interrupt
  ([module type] (e/wrong-number-of-sdk-fn-args-err))
  ([module type client-params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (send-interrupt module type (merge (ih/extract-params client-params) {:callback (first others)}))))
  ([module type client-params]
   (let [client-params (ih/extract-params client-params)
         {:keys [callback interaction-id resource-id target-resource-id queue-id transfer-extension transfer-resource-id transfer-queue-id transfer-type]} client-params
         transfer-type (if (= transfer-type "cold") "cold-transfer" "warm-transfer")
         simple-interrupt-body {:resource-id (state/get-active-user-id)}
         target-interrupt-body {:resource-id (state/get-active-user-id)
                                :target-resource target-resource-id}
         interrupt-params (case type
                            :hold {:validation ::generic-voice-interaction-fn-params
                                   :interrupt-type "customer-hold"
                                   :topic (p/get-topic :hold-acknowledged)
                                   :interrupt-body simple-interrupt-body}
                            :resume {:validation ::generic-voice-interaction-fn-params
                                     :interrupt-type "customer-resume"
                                     :topic (p/get-topic :resume-acknowledged)
                                     :interrupt-body simple-interrupt-body}
                            :mute {:validation ::generic-voice-interaction-fn-params
                                   :interrupt-type "mute-resource"
                                   :topic (p/get-topic :mute-acknowledged)
                                   :interrupt-body target-interrupt-body}
                            :unmute {:validation ::generic-voice-interaction-fn-params
                                     :interrupt-type "unmute-resource"
                                     :topic (p/get-topic :unmute-acknowledged)
                                     :interrupt-body target-interrupt-body}
                            :resource-hold {:validation ::generic-voice-interaction-fn-params
                                            :interrupt-type "resource-hold"
                                            :topic (p/get-topic :resource-hold-acknowledged)
                                            :interrupt-body target-interrupt-body}
                            :resource-resume {:validation ::generic-voice-interaction-fn-params
                                              :interrupt-type "resource-resume"
                                              :topic (p/get-topic :resource-resume-acknowledged)
                                              :interrupt-body target-interrupt-body}
                            :resume-all {:validation ::generic-voice-interaction-fn-params
                                         :interrupt-type "resume-all"
                                         :topic (p/get-topic :resume-all-acknowledged)
                                         :interrupt-body simple-interrupt-body}
                            :remove-resource {:validation ::generic-voice-interaction-fn-params
                                              :interrupt-type "remove-resource"
                                              :topic (p/get-topic :resource-removed-acknowledged)
                                              :interrupt-body target-interrupt-body}
                            :start-recording {:validation ::generic-voice-interaction-fn-params
                                              :interrupt-type "recording-start"
                                              :topic (p/get-topic :recording-start-acknowledged)
                                              :interrupt-body simple-interrupt-body}
                            :stop-recording {:validation ::generic-voice-interaction-fn-params
                                             :interrupt-type "recording-stop"
                                             :topic (p/get-topic :recording-stop-acknowledged)
                                             :interrupt-body simple-interrupt-body}
                            :transfer-to-resource {:validation ::resource-transfer-params
                                                   :interrupt-type "customer-transfer"
                                                   :topic (p/get-topic :customer-transfer-acknowledged)
                                                   :interrupt-body {:transfer-resource-id resource-id
                                                                    :resource-id (state/get-active-user-id)
                                                                    :transfer-type transfer-type}}
                            :transfer-to-queue {:validation ::queue-transfer-params
                                                :interrupt-type "customer-transfer"
                                                :topic (p/get-topic :customer-transfer-acknowledged)
                                                :interrupt-body {:transfer-queue-id queue-id
                                                                 :resource-id (state/get-active-user-id)
                                                                 :transfer-type transfer-type}}
                            :transfer-to-extension {:validation ::extension-transfer-params
                                                    :interrupt-type "customer-transfer"
                                                    :topic (p/get-topic :customer-transfer-acknowledged)
                                                    :interrupt-body {:transfer-extension transfer-extension
                                                                     :resource-id (state/get-active-user-id)
                                                                     :transfer-type transfer-type}}
                            :cancel-resource-transfer {:validation ::generic-voice-interaction-fn-params
                                                       :interrupt-type "transfer-cancel"
                                                       :topic (p/get-topic :cancel-transfer-acknowledged)
                                                       :interrupt-body {:transfer-resource-id transfer-resource-id
                                                                        :resource-id (state/get-active-user-id)
                                                                        :transfer-type transfer-type}}
                            :cancel-queue-transfer {:validation ::generic-voice-interaction-fn-params
                                                    :interrupt-type "transfer-cancel"
                                                    :topic (p/get-topic :cancel-transfer-acknowledged)
                                                    :interrupt-body {:transfer-queue-id transfer-queue-id
                                                                     :resource-id (state/get-active-user-id)
                                                                     :transfer-type transfer-type}}
                            :cancel-extension-transfer {:validation ::generic-voice-interaction-fn-params
                                                        :interrupt-type "transfer-cancel"
                                                        :topic (p/get-topic :cancel-transfer-acknowledged)
                                                        :interrupt-body {:transfer-extension transfer-extension
                                                                         :resource-id (state/get-active-user-id)
                                                                         :transfer-type transfer-type}})]
     (if-not (s/valid? (:validation interrupt-params) client-params)
       (p/publish {:topics (:topic interrupt-params)
                   :error (e/args-failed-spec-err)
                   :callback callback})
       (do #_(when (and (= transfer-type "warm-transfer")
                        (or (= type :transfer-to-resource)
                            (= type :transfer-to-queue)
                            (= type :transfer-to-extension)))
               (send-interrupt module :hold {:interaction-id interaction-id}))
           (iu/send-interrupt* (assoc interrupt-params
                                      :interaction-id interaction-id
                                      :callback callback)))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.dial({
;;   phoneNumber: "{{number}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::dial-params
  (s/keys :req-un [::specs/phone-number]
          :opt-un [::specs/callback]))

(def-sdk-fn dial
  ::dial-params
  (p/get-topic :dial-send-acknowledged)
  [params]
  (let [{:keys [topic phone-number callback]} params
        dial-url (str (state/get-base-api-url) "tenants/tenant-id/interactions")
        tenant-id (state/get-active-tenant-id)
        resource-id (state/get-active-user-id)
        dial-body {:channel-type "voice"
                   :contact-point "click to call"
                   :customer phone-number
                   :direction "outbound"
                   :interaction {:resource-id resource-id}
                   :metadata {}
                   :source "twilio"}
        dial-request {:method :post
                      :url (iu/build-api-url-with-params
                            dial-url
                            {:tenant-id tenant-id})
                      :body dial-body}]
    (do (go (let [dial-response (a/<! (iu/api-request dial-request))
                  {:keys [api-response status]} dial-response]
              (when (= status 200)
                (p/publish {:topics topic
                            :response api-response
                            :callback callback})))))))

;; -------------------------------------------------------------------------- ;;
;; Twilio Initialization Functions
;; -------------------------------------------------------------------------- ;;

(defn update-twilio-connection [connection]
  (state/set-twilio-connection connection))

(defn handle-twilio-error [script config error]
  (js/console.error error script config))

(defn ^:private twilio-init
  [config]
  (let [audio-params (ih/camelify {"audio" true})
        script-init (fn [& args]
                      (let [{:keys [js-api-url credentials]} config
                            {:keys [token]} credentials
                            script (js/document.createElement "script")
                            body (.-body js/document)
                            debug-twilio? (= (state/get-log-level) :debug)]
                        (.setAttribute script "type" "text/javascript")
                        (.setAttribute script "src" js-api-url)
                        (.appendChild body script)
                        ;; The active user's Twilio token as well as the specific twilio
                        ;; SDK version is retrieved from their config details. The url
                        ;; to the twilio SDK is then appended to the window on a script
                        ;; element - which allows us to further initialize the Twilio
                        ;; Device in order to receive voice interactions.
                        (go-loop []
                          (if (and (aget js/window "Twilio")
                                   (aget js/window "Twilio" "Device")
                                   (aget js/window "Twilio" "Device" "setup"))
                            (do
                              (state/set-twilio-device (js/Twilio.Device.setup token #js {"debug" debug-twilio?}))
                              (js/Twilio.Device.incoming update-twilio-connection)
                              (js/Twilio.Device.ready update-twilio-connection)
                              (js/Twilio.Device.cancel update-twilio-connection)
                              (js/Twilio.Device.offline update-twilio-connection)
                              (js/Twilio.Device.disconnect update-twilio-connection)
                              (js/Twilio.Device.error handle-twilio-error))
                            (do (a/<! (a/timeout 250))
                                (recur))))))]
    (-> js/navigator
        (.-mediaDevices)
        (.getUserMedia audio-params)
        (.then script-init)
        (.catch (fn [err] (e/no-microphone-access-error))))))
;; If a user "blocks" microphone access through their browser
;; it causes issues with Twilio. This is a our way of detecting
;; and notifying the user of this problem.

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
  ::send-digits-params
  (p/get-topic :send-digits-acknowledged)
  [params]
  (let [connection (state/get-twilio-connection)
        {:keys [interaction-id topic digit callback]} params
        pubsub-response {:interaction-id interaction-id
                         :digit-sent digit}]
    (if (and (= :active (state/find-interaction-location interaction-id))
             (= "voice" (:channel-type (state/get-active-interaction interaction-id))))
      (try
        (do (.sendDigits connection digit)
            (p/publish {:topics topic
                        :response pubsub-response
                        :callback callback}))
        (catch js/Object e (p/publish {:topics topic
                                       :error (e/failed-to-send-twilio-digits-err)})))
      (p/publish {:topics (p/get-topic :failed-to-send-digits-invalid-interaction)
                  :error (e/failed-to-send-digits-invalid-interaction-err)}))))


;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.voice.getRecordings({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(defn get-recording [interaction-id tenant-id artifact-id callback]
  (go (let [audio-recording (a/<! (iu/get-artifact interaction-id tenant-id artifact-id))
            {:keys [api-response status]} audio-recording]
        (when (= status 200)
          (p/publish {:topics (p/get-topic :recording-response)
                      :response (:files api-response)
                      :callback callback})))))

(s/def ::get-recordings-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-recordings
  ::get-recordings-params
  (p/get-topic :recording-response)
  [params]
  (let [{:keys [interaction-id topic callback]} params]
    (go (let [interaction-files (a/<! (iu/get-interaction-files interaction-id))
              {:keys [api-response status]} interaction-files
              {:keys [results]} api-response
              tenant-id (state/get-active-tenant-id)
              audio-recordings (filterv #(= (:artifact-type %) "audio-recording") results)]
          (if (= (count audio-recordings) 0)
            (p/publish {:topics topic
                        :response []
                        :callback callback})
            (doseq [rec audio-recordings]
              (get-recording interaction-id tenant-id (:artifact-id rec) callback)))))))

;; -------------------------------------------------------------------------- ;;
;; SDK Voice Module
;; -------------------------------------------------------------------------- ;;

(defrecord VoiceModule []
  pr/SDKModule
  (start [this]
    (let [module-name :voice
          twilio-integration (state/get-integration-by-type "twilio")]
      (if-not twilio-integration
        (ih/send-core-message {:type :module-registration-status
                               :status :failure
                               :module-name module-name})
        (do (twilio-init twilio-integration)
            (ih/register {:api {:interactions {:voice {:customer-hold (partial send-interrupt this :hold)
                                                       :customer-resume (partial send-interrupt this :resume)
                                                       :mute (partial send-interrupt this :mute)
                                                       :unmute (partial send-interrupt this :unmute)
                                                       :start-recording (partial send-interrupt this :start-recording)
                                                       :stop-recording (partial send-interrupt this :stop-recording)
                                                       :transfer-to-resource (partial send-interrupt this :transfer-to-resource)
                                                       :transfer-to-queue (partial send-interrupt this :transfer-to-queue)
                                                       :transfer-to-extension (partial send-interrupt this :transfer-to-extension)
                                                       :cancel-resource-transfer (partial send-interrupt this :cancel-resource-transfer)
                                                       :cancel-queue-transfer (partial send-interrupt this :cancel-queue-transfer)
                                                       :cancel-extension-transfer (partial send-interrupt this :cancel-extension-transfer)
                                                       :dial dial
                                                       :send-digits send-digits
                                                       :get-recordings get-recordings
                                                       :resource-remove (partial send-interrupt this :remove-resource)
                                                       :resource-hold (partial send-interrupt this :resource-hold)
                                                       :resource-resume (partial send-interrupt this :resource-resume)
                                                       :resume-all (partial send-interrupt this :resume-all)}}}
                          :module-name module-name})
            (ih/send-core-message {:type :module-registration-status
                                   :status :success
                                   :module-name module-name})))))
  (stop [this])
  (refresh-integration [this]
    (go-loop []
      (let [twilio-token-ttl (get-in (state/get-integration-by-type "twilio") [:credentials :ttl])
            min-ttl (* twilio-token-ttl 500)]
        (a/<! (a/timeout min-ttl))
        (let [resource-id (state/get-active-user-id)
              tenant-id (state/get-active-tenant-id)
              topic (p/get-topic :config-response)
              config-url (str (state/get-base-api-url) "tenants/tenant-id/users/resource-id/config")
              config-request {:method :get
                              :url (iu/build-api-url-with-params
                                    config-url
                                    {:tenant-id tenant-id
                                     :resource-id resource-id})}
              config-response (a/<! (iu/api-request config-request))
              {:keys [status api-response]} config-response
              {:keys [result]} api-response
              {:keys [integrations]} result
              twilio-integration (peek (filterv #(= (:type %1) "twilio") integrations))
              twilio-token (get-in twilio-integration [:credentials :token])]
          (state/update-integration "twilio" twilio-integration)
          (if (not= status 200)
            (p/publish {:topics topic
                        :error (e/failed-to-refresh-twilio-integration-err)})
            (state/set-twilio-device (js/Twilio.Device.setup twilio-token))))
        (recur)))))
