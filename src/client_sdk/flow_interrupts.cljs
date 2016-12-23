(ns client-sdk.flow-interrupts
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as a]
            [lumbajack.core :as l :refer [log]]
            [client-sdk-utils.core :as u]))

(enable-console-print!)

(defn flow-interrupt
  [result-chan message]
  (let [{:keys [token resp-chan tenant-id interaction-id interrupt-details interrupt-type]} message
        request-map {:method :post
                     :url "https://dev-api.cxengagelabs.net/v1/tenants/" tenant-id "/interactions/" interaction-id "/interrupts"
                     :body {:interrupt interrupt-details
                            :source "client"
                            :interrupt-type interrupt-type}
                     :token token
                     :resp-chan resp-chan}]
    (u/api-request request-map)))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :FLOW/INTERRUPT (partial flow-interrupt (a/promise-chan))
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in Flow Interrupt SDK module." (:type message)))))

(defn init []
  (let [module-inputs< (a/chan 2014)]
    (u/start-simple-consumer! module-inputs< module-router)
    module-inputs<))
