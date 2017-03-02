(ns cxengage-javascript-sdk.modules.interaction
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-cljs-utils.core :as cxu]
            [cxengage-javascript-sdk.domain.errors :as err]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.interaction-management :as int]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]))

(s/def ::generic-interaction-fn-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(s/def ::contact-operation-params
  (s/keys :req-un [::specs/interaction-id :specs/contact-id]
          :opt-un [::specs/callback]))

(s/def ::wrapup-params
  (s/keys :req-un [::specs/interaction-id ::specs/wrapup]
          :opt-un [::specs/callback]))

(defn send-interrupt
  ([module type] (e/wrong-number-of-args-error))
  ([module type client-params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (send-interrupt type module (merge (iu/extract-params client-params) {:callback (first others)}))))
  ([module type client-params]
   (let [client-params (iu/extract-params client-params)
         {:keys [callback interaction-id contact-id]} client-params
         {:keys [sub-id action-id channel-type resource-id tenant-id resource direction channel-type]} (state/get-interaction interaction-id)
         {:keys [extension role-id session-id work-offer-id]} resource
         basic-interrupt-body {:resource-id (state/get-active-user-id)}
         contact-assignment-body {:tenant-id tenant-id
                                  :contact-id contact-id
                                  :interaction-id interaction-id
                                  :sub-id sub-id
                                  :action-id action-id
                                  :work-offer-id work-offer-id
                                  :session-id session-id
                                  :resource-id resource-id
                                  :direction direction
                                  :channel-type channel-type}
         interrupt-params (case type
                            :end {:validation ::generic-interaction-fn-params
                                  :interrupt-type "resource-disconnect"
                                  :publish-fn (fn [r] (p/publish "interactions/end-acknowledged" r callback))
                                  :interrupt-body basic-interrupt-body}
                            :accept {:validation ::generic-interaction-fn-params
                                     :interrupt-type "offer-accept"
                                     :publish-fn (fn [r] (p/publish "interactions/accept-acknowledged" r callback))
                                     :interrupt-body basic-interrupt-body
                                     :on-confirm-fn (fn []
                                                      (when (= channel-type "voice")
                                                        (let [connection (state/get-twilio-connection)]
                                                          (.accept connection)))
                                                      (when (or (= channel-type "sms")
                                                                (= channel-type "messaging"))
                                                        (int/get-messaging-history tenant-id interaction-id)))}
                            :assign {:validation ::contact-operation-params
                                     :interrupt-type "interaction-contact-selected"
                                     :publish-fn (fn [r] (p/publish "interactions/contact-assigned" r callback))
                                     :interrupt-body contact-assignment-body}
                            :focus {:validation ::generic-interaction-fn-params
                                    :interrupt-type "interaction-focused"
                                    :publish-fn (fn [r] (p/publish "interactions/focus-acknowledged" r callback))
                                    :interrupt-body (assoc basic-interrupt-body :session-id (state/get-session-id))}
                            :unfocus {:validation ::generic-interaction-fn-params
                                      :interrupt-type "interaction-unfocused"
                                      :publish-fn (fn [r] (p/publish "interactions/unfocus-acknowledged" r callback))
                                      :interrupt-body (assoc basic-interrupt-body :session-id (state/get-session-id))}
                            :unassign {:validation ::contact-operation-params
                                       :interrupt-type "interaction-contact-deselected"
                                       :publish-fn (fn [r] (p/publish "interactions/contact-unassigned" r callback))
                                       :interrupt-body contact-assignment-body}
                            :enable-wrapup {:validation ::wrapup-params
                                            :interrupt-type "wrapup-on"
                                            :publish-fn (fn [r] (p/publish "interactions/wrapup-enabled" r callback))
                                            :interrupt-body basic-interrupt-body}
                            :disable-wrapup {:validation ::wrapup-params
                                             :interrupt-type "wrapup-off"
                                             :publish-fn (fn [r] (p/publish "interactions/wrapup-disabled" r callback))
                                             :interrupt-body basic-interrupt-body}
                            :end-wrapup {:validation ::wrapup-params
                                         :interrupt-type "wrapup-end"
                                         :publish-fn (fn [r] (p/publish "interactions/wrapup-ended" r callback))
                                         :interrupt-body basic-interrupt-body})]
     (if-not (s/valid? (:validation interrupt-params) client-params)
       ((:publish-fn interrupt-params) (e/invalid-args-error (s/explain-data (:validation interrupt-params) client-params)))
       (iu/send-interrupt* module (assoc interrupt-params :interaction-id interaction-id))))))

(def initial-state
  {:module-name :interactions})

(defrecord InteractionModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)]
      (register {:api {module-name {:accept (partial send-interrupt this :accept)
                                    :end (partial send-interrupt this :end)
                                    :reject (partial send-interrupt this :end)
                                    :assignContact (partial send-interrupt this :assign)
                                    :unassignContact (partial send-interrupt this :unassign)
                                    :enableWrapup (partial send-interrupt this :enable-wrapup)
                                    :disableWrapup (partial send-interrupt this :disable-wrapup)
                                    :endWrapup (partial send-interrupt this :end-wrapup)
                                    :focus (partial send-interrupt this :focus)
                                    :unfocus (partial send-interrupt this :unfocus)}}
                 :module-name module-name})
      (a/put! core-messages< {:module-registration-status :success
                              :module module-name})
      (js/console.info "<----- Started " (name module-name) " module! ----->")))
  (stop [this]))
