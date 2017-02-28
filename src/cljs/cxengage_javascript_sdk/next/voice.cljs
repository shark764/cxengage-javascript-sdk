(ns cxengage-javascript-sdk.next.voice
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
            [cxengage-javascript-sdk.domain.specs :as spec]
            [cxengage-javascript-sdk.next.protocols :as pr]
            [cognitect.transit :as t]
            [cxengage-javascript-sdk.next.errors :as e]
            [cxengage-javascript-sdk.next.pubsub :as p]
            [cljs.spec :as s]))

(defn send-interrupt
  ([module type] (e/wrong-number-of-args-error))
  ([module type client-params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (send-interrupt type module (merge (iu/extract-params client-params) {:callback (first others)}))))
  ([module type client-params]
   (let [client-params (iu/extract-params client-params)
         {:keys [callback interaction-id]} client-params
         simple-interrupt-body {:resource-id (state/get-active-user-id)}
         interrupt-params (case type
                            :hold {:validation ::hold-params
                                   :interrupt-type "customer-hold"
                                   :publish-fn (fn [r] (p/publish "interactions/voice/hold-acknowledged" r callback))
                                   :interrupt-body simple-interrupt-body}
                            :resume {:validation ::resume-params
                                     :interrupt-type "customer-resume"
                                     :publish-fn (fn [r] (p/publish "interactions/voice/resume-acknowledged" r callback))
                                     :interrupt-body simple-interrupt-body}
                            :mute {:validation ::mute-params
                                   :interrupt-type "mute-resource"
                                   :publish-fn (fn [r] (p/publish "interactions/voice/mute-acknowledged" r callback))
                                   :interrupt-body simple-interrupt-body}
                            :unmute {:validation ::unmute-params
                                     :interrupt-type "unmute-resource"
                                     :publish-fn (fn [r] (p/publish "interactions/voice/unmute-acknowledged" r callback))
                                     :interrupt-body simple-interrupt-body}
                            :start-recording {:validation ::start-recording-params
                                              :interrupt-type "recording-start"
                                              :publish-fn (fn [r] (p/publish "interactions/voice/start-recording-acknowledged" r callback))
                                              :interrupt-body simple-interrupt-body}
                            :stop-recording {:validation ::stop-recording-params
                                             :interrupt-type "recording-stop"
                                             :publish-fn (fn [r] (p/publish "interactions/voice/stop-recording-acknowledged" r callback))
                                             :interrupt-body simple-interrupt-body}


                            :transfer {:validation ::transfer-params
                                       :interrupt-type ""
                                       :publish-fn (fn [r] (p/publish "interactions/voice/transfer-acknowledged" r callback))
                                       :interrupt-body {}}
                            :cancel-transfer {:validation ::cancel-transfer-params
                                              :interrupt-type ""
                                              :publish-fn (fn [r] (p/publish "interactions/voice/cancel-transfer-acknowledged" r callback))
                                              :interrupt-body {}}
                            :dial {:validation ::dial-params
                                   :interrupt-type ""
                                   :publish-fn (fn [r] (p/publish "interactions/voice/dial-acknowledged" r callback))
                                   :interrupt-body {}})]
     (if-not (s/valid? (:validation interrupt-params) client-params)
       ((:publish-fn interrupt-params) (e/invalid-args-error (s/explain-data (:validation interrupt-params) client-params)))
       (iu/send-interrupt* module (assoc interrupt-params :interaction-id interaction-id))))))

(def initial-state
  {:module-name :voice})

(defrecord VoiceModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)
          twilio-integration (state/get-integration-by-type "twilio")
          twilio-integration (->> (merge (select-keys twilio-integration [:region :endpoint])
                                         (select-keys (:credentials twilio-integration) [:secret-key :access-key :session-token]))
                                  (transform-keys camel/->kebab-case-keyword)
                                  (#(rename-keys % {:region :region-name})))]
      (if-not twilio-integration
        (a/put! core-messages< {:module-registration-status :failure
                                :module module-name})
        (do #_(twilio-init twilio-integration core-messages<)
            (register {:api {:interactions {:voice {:hold (partial send-interrupt this :hold)
                                                    :resume (partial send-interrupt this :resume)
                                                    :mute (partial send-interrupt this :mute)
                                                    :unmute (partial send-interrupt this :unmute)
                                                    :start-recording (partial send-interrupt :start-recording)
                                                    :stop-recording (partial send-interrupt this :stop-recording)
                                                    :transfer (partial send-interrupt this :transfer)
                                                    :cancel-trasfer (partial send-interrupt this :cancel-transfer)
                                                    :dial (partial send-interrupt this :dial)}}}
                       :module-name module-name})
            (js/console.info "<----- Started " module-name " module! ----->")))))
  (stop [this]))
