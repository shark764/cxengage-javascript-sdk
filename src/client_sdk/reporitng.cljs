(ns client-sdk.reporting
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :as a]
            [lumbajack.core :refer [log]]
            [client-sdk-utils.core :as u]))

(enable-console-print!)

(defn start-polling [result-chan message]
  (log :debug "Starting reporting polls...")
  (let [{:keys [tenant-id token interval]} message
        reporting-req-map {:method :post
                           :body {:requests {:widget {:statistic "interaction-starts-count"}}}
                           :url (str "https://dev-api.cxengagelabs.net/v1/tenants/"
                                     tenant-id "/realtime-statistics/batch")
                           :token token}]
    (go-loop [next-batch-resp-chan (a/promise-chan)]
      (u/api-request (merge reporting-req-map {:resp-chan next-batch-resp-chan}))
      (log :debug "Batch request sent!")
      (let [{:keys [results]} (a/<! next-batch-resp-chan)]
        (a/put! result-chan results)
        (log :debug "Reporting data received!")
        (a/<! (a/timeout (or interval 3000)))
        (recur (a/promise-chan))))))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :REPORTING/START_POLLING (partial start-polling (a/promise-chan))
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in Reporting SDK module." (:type message)))))

(defn init []
  (let [module-inputs< (a/chan 2014)]
    (u/start-simple-consumer! module-inputs< module-router)
    module-inputs<))
