(ns client-sdk.api.reporting
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [client-sdk.state :as state]))

(defn start-polling-handler
  ([module-chan result-chan callback]
   (start-polling-handler module-chan result-chan {} callback))
  ([module-chan result-chan params callback]
   (let [{:keys [interval]} params
         reporting-msg {:resp-chan result-chan
                        :type :REPORTING/START_POLLING
                        :interval interval
                        :token (state/get-token)
                        :tenant-id (state/get-active-tenant-id)}]
      (a/put! module-chan reporting-msg)
      (go (let [{:keys [result]} (a/<! result-chan)]
           (callback))))))

(defn check-capacity-handler
  [module-chan result-chan callback]
  (let [reporting-msg {:resp-chan result-chan
                       :type :REPORTING/CHECK_CAPACITY
                       :token (state/get-token)
                       :tenant-id (state/get-active-tenant-id)
                       :user-id (state/get-active-user-id)}]
       (a/put! module-chan reporting-msg)
       (go (let [{:keys [results]} (a/<! result-chan)]
            (callback (clj->js results))))))

(defn available-stats-handler
  [module-chan result-chan callback]
  (let [reporting-msg {:resp-chan result-chan
                       :type :REPORTING/AVAILABLE_STATS
                       :token (state/get-token)
                       :tenant-id (state/get-active-tenant-id)}]
       (a/put! module-chan reporting-msg)
       (go (callback (clj->js (a/<! result-chan))))))

(defn api []
  (let [module-chan (state/get-module-chan :reporting)]
    {:startPolling (partial start-polling-handler module-chan (a/promise-chan))
     :getCapacity (partial check-capacity-handler module-chan (a/promise-chan))
     :getAvailableStats (partial available-stats-handler module-chan (a/promise-chan))}))
