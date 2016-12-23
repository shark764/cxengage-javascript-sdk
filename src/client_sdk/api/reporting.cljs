(ns client-sdk.api.reporting
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [client-sdk.state :as state]))

(defn start-polling-handler [module-chan result-chan callback interval]
   (let [reporting-msg {:resp-chan result-chan
                        :type :REPORTING/START_POLLING
                        :interval interval
                        :token (state/get-token)
                        :tenant-id (state/get-active-tenant-id)}]
      (a/put! module-chan reporting-msg)
      (go (let [{:keys [result]} (a/<! result-chan)]
           (callback)))))

(defn api []
  (let [module-chan (state/get-module-chan :reporting)]
    {:startPolling (partial start-polling-handler module-chan (a/promise-chan))}))
