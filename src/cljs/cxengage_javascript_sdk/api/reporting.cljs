(ns cxengage-javascript-sdk.api.reporting
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.module-gateway :as mg]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.pubsub :refer [sdk-response sdk-error-response]]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.errors :as err]))

(s/def ::start-polling-params
  (s/keys :req-un []
          :opt-un [:specs/callback]))

(defn start-polling
  ([params callback]
   (start-polling (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/reporting/polling-response"
         {:keys [stats interval callback]} params]
    (if-let [error (cond
                     (not (s/valid? ::start-polling-params params)) (err/invalid-params-err)
                     (not (state/session-started?)) (err/invalid-sdk-state-err "Session not started.")
                     (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Active Tenant not set.")
                     :else false)]
      (sdk-error-response "cxengage/reporting/polling-response" error (:callback params))
      (let [polling-body (iu/base-module-request
                          :REPORTING/START_POLLING
                          {:tenant-id (state/get-active-tenant-id)
                           :interval interval
                           :stats stats})]
        (go (let [polling-response (a/<! (mg/send-module-message polling-body))]
              (sdk-response pubsub-topic polling-response callback))))))))

(s/def ::available-stats-params
  (s/keys :req-un []
          :opt-un [:specs/callback]))

(defn available-stats
  ([]
   (available-stats {}))
  ([params callback]
   (available-stats (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/reporting/available-stats-response"
         {:keys [callback]} params]
    (if-let [error (cond
                     (not (s/valid? ::available-stats-params params)) (err/invalid-params-err)
                     (not (state/session-started?)) (err/invalid-sdk-state-err "Session not started.")
                     (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Active Tenant not set.")
                     :else false)]
      (sdk-error-response "cxengage/reporting/available-stats-response" error (:callback params))
      (let [available-stats-body (iu/base-module-request
                                  :REPORTING/AVAILABLE_STATS
                                  {:tenant-id (state/get-active-tenant-id)})]
        (go (let [available-stats-response (a/<! (mg/send-module-message available-stats-body))]
              (sdk-response pubsub-topic available-stats-response callback))))))))

(s/def ::check-capacity-params
  (s/keys :req-un [:specs/resourceId]
          :opt-un [:specs/callback]))

(defn check-capacity
  ([params callback]
   (check-capacity (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/reporting/check-capacity-response"
         {:keys [resourceId callback]} params]
    (if-let [error (cond
                     (not (s/valid? ::check-capacity-params params)) (err/invalid-params-err)
                     (not (state/session-started?)) (err/invalid-sdk-state-err "Session not started.")
                     (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Active Tenant not set.")
                     :else false)]
      (sdk-error-response "cxengage/reporting/check-capacity-response" error (:callback params))
      (let [check-capacity-body (iu/base-module-request
                                  :REPORTING/CHECK_CAPACITY
                                  {:tenant-id (state/get-active-tenant-id)
                                   :resource-id resourceId})]
        (go (let [check-capacity-response (a/<! (mg/send-module-message check-capacity-body))]
              (sdk-response pubsub-topic check-capacity-response callback))))))))
