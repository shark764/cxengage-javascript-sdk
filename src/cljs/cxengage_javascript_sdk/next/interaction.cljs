(ns cxengage-javascript-sdk.next.interaction
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.module-gateway :as mg]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.pubsub :refer [sdk-response sdk-error-response]]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-cljs-utils.core :as u]
            [cxengage-javascript-sdk.domain.errors :as err]
            [cxengage-javascript-sdk.next.protocols :as pr]
            [cxengage-javascript-sdk.next.errors :as e]
            [cxengage-javascript-sdk.next.pubsub :as p]))

(defn send-interrupt*
  ([module params]
   (let [params (iu/extract-params params)
         module-state @(:state module)
         {:keys [interaction-id interrupt-type interrupt-body publish-fn]} params
         tenant-id (state/get-active-tenant-id)
         interrupt-request {:method :post
                            :body {:source "client"
                                   :interrupt-type interrupt-type
                                   :interrupt interrupt-body}
                            :url (str (state/get-base-api-url) "tenants/" tenant-id "/interactions/" interaction-id "/interrupts")}]
     (do (go (let [interrupt-response (a/<! (iu/api-request interrupt-request))
                   {:keys [api-response status]} interrupt-response]
               (if (not= status 200)
                 (publish-fn (e/api-error api-response))
                 (publish-fn {:interacton-id interaction-id}))))
         nil))))

(s/def ::assign-contact-params
  (s/keys :req-un [:specs/contact-id :specs/interaction-id]
          :opt-un [:specs/callback]))

(s/def ::unassign-contact-params
  (s/keys :req-un [:specs/contact-id :specs/interaction-id]
          :opt-un [:specs/callback]))

(s/def ::accept-interaction-params
  (s/keys :req-un [:specs/interaction-id]
          :opt-un [:specs/callback]))

(s/def ::end-interaction-params
  (s/keys :req-un [:specs/interaction-id]
          :opt-un [:specs/callback]))

(s/def ::contact-operation-params
  (s/keys :req-un [:specs/interaction-id :specs/contact-id]
          :opt-un [:specs/callback]))

(s/def ::wrapup-params
  (s/keys ::req-un [:specs/interaction-id
                    :specs/wrapup]
          ::opt-un [:specs/callback]))

(defn send-interrupt
  ([module type] (e/wrong-number-of-args-error))
  ([module type client-params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (send-interrupt type module (merge (iu/extract-params client-params) {:callback (first others)}))))
  ([module type client-params]
   (let [client-params (iu/extract-params client-params)
         {:keys [callback interaction-id contact-id]} client-params
         {:keys [sub-id action-id channel-type resource-id tenant-id resource direction]} (state/get-interaction interaction-id)
         {:keys [extension role-id session-id work-offer-id]} resource
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
                            :end {:validation ::end-interaction-params
                                  :interrupt-type "resource-disconnect"
                                  :publish-fn (fn [r] (p/publish "interactions/end-acknowledge" r callback))
                                  :interrupt-body {:resource-id (state/get-active-user-id)}
                                  :interaction-id interaction-id}
                            :accept {:validation ::end-interaction-params
                                     :interrupt-type "offer-accept"
                                     :publish-fn (fn [r] (p/publish "interactions/accept-acknowledge" r callback))
                                     :interrupt-body {:resource-id (state/get-active-user-id)}
                                     :interaction-id interaction-id}
                            :assign {:validation ::contact-operation-params
                                     :interrupt-type "interaction-contact-selected"
                                     :publish-fn (fn [r] (p/publish "interactions/contact-assigned" r callback))
                                     :interrupt-body contact-assignment-body
                                     :interaction-id interaction-id}
                            :unassign {:validation ::contact-operation-params
                                       :interrupt-type "interaction-contact-deselected"
                                       :publish-fn (fn [r] (p/publish "interactions/contact-unassigned" r callback))
                                       :interrupt-body contact-assignment-body
                                       :interaction-id interaction-id}
                            :enable-wrapup {:validation ::wrapup-params
                                            :interrupt-type "wrapup-on"
                                            :publish-fn (fn [r] (p/publish "interactions/wrapup-enabled" r callback))
                                            :interrupt-body {:resource-id (state/get-active-user-id)}
                                            :interaction-id interaction-id}
                            :disable-wrapup {:validation ::wrapup-params
                                             :interrupt-type "wrapup-off"
                                             :publish-fn (fn [r] (p/publish "interactions/wrapup-disabled" r callback))
                                             :interrupt-body {:resource-id (state/get-active-user-id)}
                                             :interaction-id interaction-id}
                            :end-wrapup {:validation ::wrapup-params
                                         :interrupt-type "wrapup-end"
                                         :publish-fn (fn [r] (p/publish "interactions/wrapup-ended" r callback))
                                         :interrupt-body {:resource-id (state/get-active-user-id)}
                                         :interaction-id interaction-id})]
     (if-not (s/valid? (:validation interrupt-params) client-params)
       ((:publish-fn interrupt-params) (e/invalid-args-error (s/explain-data (:validation interrupt-params) client-params)))
       (send-interrupt* module interrupt-params)))))

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
                                    :endWrapup (partial send-interrupt this :end-wrapup)}}
                 :module-name module-name})
      (a/put! core-messages< {:module-registration-status :success
                              :module module-name})
      (js/console.info "<----- Started " module-name " module! ----->")))
  (stop [this]))
