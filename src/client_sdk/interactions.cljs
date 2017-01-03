(ns client-sdk.interactions
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [lumbajack.core :refer [log]]
            [client-sdk-utils.core :as u]))

(def module-state (atom {}))

(defn send-interrupt [message]
  (let [{:keys [token interruptType source resourceId interactionId tenantId]} message
        interrupt-body {:source source
                        :interruptType interruptType
                        :interrupt {:resource-id resourceId}}
        request-map {:method :post
                     :body interrupt-body
                     :url (u/api-url (:env @module-state)
                                     (str "/tenants/" tenantId "/interactions/" interactionId "/interrupts"))
                     :token token}]
    (u/api-request request-map)))

(defn acknowledge-flow-action [message]
  (let [{:keys [token tenantId actionId interactionId subId resourceId]} message
        acknowledge-body {:source "client"
                          :subId subId
                          :update {:resourceId resourceId}}
        request-map {:method :post
                     :body acknowledge-body
                     :url (u/api-url (:env @module-state)
                                     (str "/tenants/" tenantId "/interactions/" interactionId "/actions/" actionId))
                     :token token}]
    (u/api-request request-map)))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :INTERACTIONS/SEND_INTERRUPT send-interrupt
                      :INTERACTIONS/ACKNOWLEDGE_FLOW_ACTION acknowledge-flow-action
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in Interactions SDK module." (:type message)))))

(defn init [env]
  (swap! module-state assoc :env env)
  (let [module-inputs< (a/chan 1024)
        module-shutdown< (a/chan 1024)]
    (u/start-simple-consumer! module-inputs< module-router)
    {:messages module-inputs<
     :shutdown module-shutdown<}))
