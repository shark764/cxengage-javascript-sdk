(ns client-sdk.modules.reporting
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :as a]
            [lumbajack.core :refer [log]]
            [client-sdk-utils.core :as u]))

(def module-state (atom {}))

(defn start-polling [response-chan message]
  (log :debug "Starting reporting polls...")
  (let [{:keys [tenant-id token interval]} message
        reporting-req-map {:method :post
                           :body {:requests {:widget {:statistic "interaction-starts-count"}}}
                           :url (u/api-url (:env @module-state)
                                           (str "/tenants/" tenant-id "/realtime-statistics/batch"))
                           :token token}]
    (go-loop [next-batch-resp-chan (a/promise-chan)]
      (u/api-request (merge reporting-req-map {:resp-chan next-batch-resp-chan}))
      (log :debug "Batch request sent!")
      (let [{:keys [results]} (a/<! next-batch-resp-chan)]
        (a/put! response-chan results)
        (log :debug "Reporting data received!")
        (a/<! (a/timeout (or interval 3000)))
        (recur (a/promise-chan))))))

(defn check-capacity
  [response-chan message]
  (let [{:keys [token resp-chan tenant-id user-id]} message
        request-map {:method :get
                     :url (u/api-url (:env @module-state)
                                     (str "/tenants/" tenant-id "/users/" user-id "/realtime-statistics/resource-capacity"))
                     :token token
                     :resp-chan resp-chan}]
    (u/api-request request-map)
    (go (let [result (a/<! response-chan)]
          (a/put! resp-chan result)))))

(defn available-stats
  [response-chan message]
  (let [{:keys [token resp-chan tenant-id user-id]} message
        request-map {:method :get
                     :url (u/api-url (:env @module-state)
                                     (str "/tenants/" tenant-id "/realtime-statistics/available"))
                     :token token
                     :resp-chan resp-chan}]
    (u/api-request request-map)
    (go (let [result (a/<! response-chan)]
          (a/put! resp-chan result)))))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :REPORTING/POLL (partial start-polling (a/promise-chan))
                      :REPORTING/CHECK_CAPACITY (partial check-capacity (a/promise-chan))
                      :REPORTING/AVAILABLE_STATS (partial available-stats (a/promise-chan))
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in Reporting SDK module." (:type message)))))

(defn module-shutdown-handler [message]
  (log :info "Received shutdown message from Core - Reporting Module shutting down...."))

(defn init [env]
  (log :info "Initializing SDK module: Reporting")
  (swap! module-state assoc :env env)
  (let [module-inputs< (a/chan 1024)
        module-shutdown< (a/chan 1024)]
    (u/start-simple-consumer! module-inputs< module-router)
    (u/start-simple-consumer! module-shutdown< module-shutdown-handler)
    {:messages module-inputs<
     :shutdown module-shutdown<}))
