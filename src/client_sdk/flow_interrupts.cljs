(ns client-sdk.flow-interrupts
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as a]
            [lumbajack.core :as l :refer [log]]
            [client-sdk-utils.core :as u]))

(enable-console-print!)

(defn flow-interrupt
  [message]
  (let [{:keys [token resp-chan tenant-id interaction-id interrupt-details interrupt-type]} message
        request-map {:method :post
                     :url "https://dev-api.cxengagelabs.net/v1/tenants/" tenant-id "/interactions/" interaction-id "interrupts"
                     :body {:interrupt interrupt-details
                            :source "client"
                            :interrupt-type interrupt-type}
                     :token token
                     :resp-chan resp-chan}]
    (u/api-request request-map)))

(defn init []
  (let [flow-interrupt-chan (a/chan 1024)
        module-topics {:interrupt flow-interrupt-chan}]
    (u/start-simple-consumer! flow-interrupt-chan flow-interrupt)
    module-topics))
