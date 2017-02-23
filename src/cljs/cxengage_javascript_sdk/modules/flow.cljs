(ns cxengage-javascript-sdk.modules.flow
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-cljs-utils.core :as u]))

(def module-state (atom {}))

(defn send-interrupt [message]
  (let [{:keys [resp-chan token interruptType source interrupt interactionId tenantId]} message
        interrupt-body {:source source
                        :interruptType interruptType
                        :interrupt interrupt}
        request-map {:method :post
                     :body interrupt-body
                     :url (str (state/get-base-api-url) "/tenants/" tenantId "/interactions/" interactionId "/interrupts")
                     :token token
                     :resp-chan resp-chan}
        resp (a/promise-chan)]
    (u/api-request request-map)
    (go (let [{:keys [result status] :as interrupt-response} (a/<! resp-chan)]
          (if (and (not= status 200) (not= interruptType "resource-disconnect"))
            (do (log :error "Bad interrupt response from server, ending interaction" interruptType)
                (a/put! (:error-channel @module-state) {:type :interaction/SHUTDOWN
                                                        :interaction-id interactionId}))
            (log :error "Bad interrupt response from server while ending interaction." interruptType))
          (a/put! resp interrupt-response)
          resp))))

(defn acknowledge-flow-action [message]
  (let [{:keys [token tenantId actionId interactionId subId resourceId]} message
        acknowledge-body {:source "client"
                          :subId subId
                          :update {:resourceId resourceId}}
        request-map {:method :post
                     :body acknowledge-body
                     :url (str (state/get-base-api-url) "/tenants/" tenantId "/interactions/" interactionId "/actions/" actionId)
                     :token token}]
    (u/api-request request-map)))

(defn create-interaction [message]
  (let [{:keys [resp-chan token tenantId resourceId customer]} message
        dial-body {:source "twilio"
                   :customer customer
                   :channelType "voice"
                   :direction "outbound"
                   :contactPoint "click to call"
                   :interaction {:resourceId resourceId}
                   :metadata {}}
        request-map {:method :post
                     :body dial-body
                     :url (str (state/get-base-api-url) "/tenants/" tenantId "/interactions")
                     :token token
                     :resp-chan resp-chan}]
    (u/api-request request-map)))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :INTERACTIONS/SEND_INTERRUPT send-interrupt
                      :INTERACTIONS/ACKNOWLEDGE_FLOW_ACTION acknowledge-flow-action
                      :INTERACTIONS/CREATE_INTERACTION create-interaction
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in Interactions SDK module." (:type message)))))

(defn module-shutdown-handler [msg-chan sd-chan message]
  (when message
    (a/close! msg-chan)
    (a/close! sd-chan)
    (reset! module-state {}))
  (log :info "Received shutdown message from Core - Interactions Module shutting down...."))

(defn init [err-chan]
  (log :debug "Initializing SDK module: Flow")
  (swap! module-state assoc :error-channel err-chan)
  (let [module-inputs< (a/chan 1024)
        module-shutdown< (a/chan 1024)]
    (u/start-simple-consumer! module-inputs< module-router)
    (u/start-simple-consumer! module-shutdown< (partial module-shutdown-handler module-inputs< module-shutdown<))
    {:messages module-inputs<
     :shutdown module-shutdown<}))
