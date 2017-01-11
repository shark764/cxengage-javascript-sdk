(ns client-sdk.api.reporting
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [client-sdk.state :as state]))

(defn start-polling
  ([module-chan result-chan callback]
   (start-polling module-chan result-chan {} callback))
  ([module-chan result-chan params callback]
   (let [module-chan (state/get-module-chan :reporting)
         {:keys [interval]} params
         reporting-msg {:resp-chan result-chan
                        :type :REPORTING/POLL
                        :interval interval
                        :token (state/get-token)
                        :tenant-id (state/get-active-tenant-id)}]
     (a/put! module-chan reporting-msg)
     (go (let [response (a/<! result-chan)
               {:keys [result]} response]
           (a/put! (state/get-module-chan :pubsub)
                   (merge {:msg-type :REPORTING/POLL_RESPONSE} response))
           (callback))))))

(defn check-capacity
  [module-chan result-chan callback]
  (let [module-chan (state/get-module-chan :reporting)
        reporting-msg {:resp-chan result-chan
                       :type :REPORTING/CHECK_CAPACITY
                       :token (state/get-token)
                       :tenant-id (state/get-active-tenant-id)
                       :user-id (state/get-active-user-id)}]
    (a/put! module-chan reporting-msg)
    (go (let [response (a/<! result-chan)
              {:keys [results]} response]
          (a/put! (state/get-module-chan :pubsub)
                  (merge {:msg-type :REPORTING/CHECK_CAPACITY_RESPONSE} response))
          (callback (clj->js results))))))

(defn available-stats
  [module-chan result-chan callback]
  (let [module-chan (state/get-module-chan :reporting)
        reporting-msg {:resp-chan result-chan
                       :type :REPORTING/AVAILABLE_STATS
                       :token (state/get-token)
                       :tenant-id (state/get-active-tenant-id)}]
    (a/put! module-chan reporting-msg)
    (go (let [response (a/<! result-chan)]
          (a/put! (state/get-module-chan :pubsub)
                  (merge {:msg-type :REPORTING/AVAILABLE_STATS_RESPONSE} response))
          (callback (clj->js response))))))
