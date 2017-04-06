(ns cxengage-javascript-sdk.modules.voice
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljsjs.paho]
            [camel-snake-kebab.core :as camel]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cljs.core.async :as a]
            [clojure.set :refer [rename-keys]]
            [cljs-time.core :as time]
            [cljs-time.format :as fmt]
            [cljs-time.instant]
            [cxengage-javascript-sdk.helpers :refer [log]]
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
  ([module type] (e/wrong-number-of-args-error))
  ([module type client-params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (send-interrupt module type (merge (iu/extract-params client-params) {:callback (first others)}))))
  ([module type client-params]
   (let [client-params (iu/extract-params client-params)
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
       (p/publish {:topic (:topic interrupt-params)
                   :error (e/invalid-args-error (s/explain-data (:validation interrupt-params) client-params))
                   :callback callback})
       (do #_(when (and (= transfer-type "warm-transfer")
                        (or (= type :transfer-to-resource)
                            (= type :transfer-to-queue)
                            (= type :transfer-to-extension)))
               (send-interrupt module :hold {:interaction-id interaction-id}))
           (iu/send-interrupt* module (assoc interrupt-params
                                             :interaction-id interaction-id
                                             :callback callback)))))))

(s/def ::dial-params
  (s/keys :req-un [::specs/phone-number]
          :opt-un [::specs/callback]))

(defn dial
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (dial module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         {:keys [phone-number callback]} params
         topic (p/get-topic :dial-send-acknowledged)
         api-url (get-in module [:config :api-url])
         dial-url (str api-url "tenants/tenant-id/interactions")
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
     (if-not (s/valid? ::dial-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::dial-params params))
                   :callback callback})
       (do (go (let [dial-response (a/<! (iu/api-request dial-request))
                     {:keys [api-response status]} dial-response]
                 (if (not= status 200)
                   (p/publish {:topics topic
                               :error (e/api-error api-response)
                               :callback callback})
                   (p/publish {:topics topic
                               :response api-response
                               :callback callback}))))
           nil)))))

(defn update-twilio-connection [connection]
  (state/set-twilio-connection connection))

(defn handle-twilio-error [script config error]
  (log :error error script config))

(defn ^:private twilio-init
  [config done-init<]
  (let [audio-params (iu/camelify {"audio" true})
        script-init (fn [& args]
                      (let [{:keys [js-api-url credentials]} config
                            {:keys [token]} credentials
                            script (js/document.createElement "script")
                            body (.-body js/document)
                            debug-twilio? (= (state/get-log-level) :debug)]
                        (.setAttribute script "type" "text/javascript")
                        (.setAttribute script "src" js-api-url)
                        (.appendChild body script)
                        (go-loop []
                          (if (aget js/window "Twilio")
                            (do
                              (state/set-twilio-device (js/Twilio.Device.setup token #js {"debug" debug-twilio?}))
                              (js/Twilio.Device.incoming update-twilio-connection)
                              (js/Twilio.Device.ready update-twilio-connection)
                              (js/Twilio.Device.cancel update-twilio-connection)
                              (js/Twilio.Device.offline update-twilio-connection)
                              (js/Twilio.Device.disconnect update-twilio-connection)
                              (js/Twilio.Device.error handle-twilio-error)
                              (p/publish {:topics (p/get-topic :voice-enabled)
                                          :response true})
                              (log :info "<----- Started voice SDK module! ----->"))
                            (do (a/<! (a/timeout 250))
                                (recur))))))]
    (-> js/navigator
        (.-mediaDevices)
        (.getUserMedia audio-params)
        (.then script-init)
        (.catch (fn [err] (e/no-microphone-access-error err))))))

(s/def ::send-digits-params
  (s/keys :req-un [::specs/interaction-id
                   ::specs/digit]
          :opt-un [::specs/callback]))

(defn send-digits
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (send-digits module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [connection (state/get-twilio-connection)
         {:keys [interaction-id digit callback] :as params} (iu/extract-params params)
         module-state @(:state module)
         topic (p/get-topic :send-digits-acknowledged)]
     (if-not (s/valid? ::send-digits-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::send-digits-params params))
                   :callback callback})
       (when (and (= :active (state/find-interaction-location interaction-id))
                  (= "voice" (:channel-type (state/get-active-interaction interaction-id))))
         (when connection
           (try
             (do (.sendDigits connection digit)
                 (p/publish {:topics topic
                             :response {:interaction-id interaction-id
                                        :digit-sent digit}
                             :callback callback}))
             (catch js/Object e (str "Caught: Invalid Dial-Tone Multiple Frequency signal: " e)))))))))

(defn get-recording [interaction-id tenant-id artifact-id callback]
  (go (let [audio-recording (a/<! (iu/get-artifact interaction-id tenant-id artifact-id))
            {:keys [api-response status]} audio-recording]
        (if-not (= status 200)
          (p/publish {:topics (p/get-topic :recording-response)
                      :response (e/api-error "api returned error")
                      :callback callback})
          (p/publish {:topics (p/get-topic :recording-response)
                      :response (:files api-response)
                      :callback callback})))))

(defn get-recordings
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (get-recordings module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         {:keys [interaction-id callback]} params]
     (go (let [interaction-files (a/<! (iu/get-interaction-files interaction-id))
               {:keys [api-response status]} interaction-files
               {:keys [results]} api-response
               tenant-id (state/get-active-tenant-id)
               audio-recordings (filterv #(= (:artifact-type %) "audio-recording") results)]
           (if-not (= status 200)
             (p/publish {:topics (p/get-topic :recording-response)
                         :response (e/api-error "api returned error")
                         :callback callback})
             (if (= (count audio-recordings) 0)
               (p/publish {:topics (p/get-topic :recording-response)
                           :response []
                           :callback callback})
               (doseq [rec audio-recordings]
                 (get-recording interaction-id tenant-id (:artifact-id rec) callback))))))
     nil)))

(def initial-state
  {:module-name :voice})

(defrecord VoiceModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)
          twilio-integration (state/get-integration-by-type "twilio")]
      (if-not twilio-integration
        (a/put! core-messages< {:module-registration-status :failure
                                :module module-name})
        (do (twilio-init twilio-integration core-messages<)
            (register {:api {:interactions {:voice {:customer-hold (partial send-interrupt this :hold)
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
                                                    :dial (partial dial this)
                                                    :send-digits (partial send-digits this)
                                                    :get-recordings (partial get-recordings this)
                                                    :resource-remove (partial send-interrupt this :remove-resource)
                                                    :resource-hold (partial send-interrupt this :resource-hold)
                                                    :resource-resume (partial send-interrupt this :resource-resume)
                                                    :resume-all (partial send-interrupt this :resume-all)}}}
                       :module-name module-name})))))
  (stop [this]))
