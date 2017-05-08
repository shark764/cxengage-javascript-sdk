(ns cxengage-javascript-sdk.next-modules.reporting
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cxengage-javascript-sdk.macros :refer [def-sdk-fn]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cljs-uuid-utils.core :as uuid]))

(def stat-subscriptions (atom {}))

(defn start-polling
  [module]
  (let [tenant-id (st/get-active-tenant-id)
        topic (p/get-topic :batch-response)
        polling-delay (st/get-reporting-refresh-rate)]
    (let [api-url (str (st/get-base-api-url) "tenants/tenant-id/realtime-statistics/batch")]
      (go-loop []
        (a/<! (a/timeout polling-delay))
        (if (empty? (:statistics @stat-subscriptions))
          (recur)
          (let [polling-request {:method :post
                                 :body {:requests (:statistics @stat-subscriptions)}
                                 :url (iu/build-api-url-with-params
                                       api-url
                                       {:tenant-id tenant-id})}
                {:keys [api-response status]} (a/<! (iu/api-request polling-request true))
                {:keys [results]} api-response]
            (if (not= status 200)
              (do (js/console.error "Batch request failed.")
                  (p/publish {:topics topic
                              :error (e/client-request-err)}))
              (do (js/console.info "Batch request received!")
                  (p/publish {:topics topic
                              :response results})

                  (recur))))))
      nil)))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.addStatisticSubscription({
;;   statistic: "{{string}}",
;;   resourceId: "{{uuid}}",
;;   queueId: "{{queueId}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::add-statistic-params
  (s/keys :req-un [::specs/statistic]
          :opt-un [::specs/callback ::specs/queue-id ::specs/resource-id]))

(def-sdk-fn add-stat-subscription
   ::add-statistic-params
   (p/get-topic :add-stat)
   [params]
   (let [{:keys [stat-id topic callback]} params
         tenant-id (st/get-active-tenant-id)
         stat-bundle (dissoc params :callback)]
       (let [stat-id (str (uuid/make-random-uuid))]
         (swap! stat-subscriptions assoc-in [:statistics stat-id] stat-bundle)
         (p/publish {:topics topic
                     :response {:stat-id stat-id}
                     :callback callback})
         (go (let [batch-url (str (st/get-base-api-url) "tenants/tenant-id/realtime-statistics/batch")
                   polling-request {:method :post
                                    :body {:requests (:statistics @stat-subscriptions)}
                                    :url (iu/build-api-url-with-params
                                          batch-url
                                          {:tenant-id tenant-id})}
                   {:keys [api-response status]} (a/<! (iu/api-request polling-request true))
                   {:keys [results]} api-response
                   batch-topic (p/get-topic :batch-response)]
               (when (= status 200)
                 (p/publish {:topics batch-topic
                             :response results
                             :callback callback})))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.removeStatSubscription({
;;   statId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::remove-statistics-params
  (s/keys :req-un [::specs/stat-id]
          :opt-un [::specs/callback]))

(def-sdk-fn remove-stat-subscription
   ::remove-statistics-params
   (p/get-topic :remove-stat)
   [params]
   (let [{:keys [stat-id topic callback]} params]
       (let [new-stats (dissoc (:statistics @stat-subscriptions) stat-id)]
         (swap! @stat-subscriptions assoc :statistics new-stats)
         (p/publish {:topics topic
                     :response new-stats
                     :callback callback}))))


;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.getCapacity({
;;   resourceId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-capacity-params
  (s/keys :req-un []
          :opt-un [::specs/callback ::specs/resource-id]))

(def-sdk-fn get-capacity
   ::get-capacity-params
   (p/get-topic :get-capacity-response)
   [params]
   (let [tenant-id (st/get-active-tenant-id)
         {:keys [resource-id topic callback]} params
         url (if resource-id
                 "tenants/tenant-id/users/resource-id/realtime-statistics/resource-capacity"
                 "tenants/tenant-id/realtime-statistics/resource-capacity")]
         ;; If resource-id is passed to the function, it will return the Capacity
         ;; for the specified resource-id. If no arguments are passed to the function
         ;; it will instead return the capacity for the active user's selected Tenant
       (go (let [capacity-url (str (st/get-base-api-url) url)
                 url-params (if resource-id {:tenant-id tenant-id :resource-id resource-id} {:tenant-id tenant-id})
                 capacity-request {:method :get
                                   :url (iu/build-api-url-with-params
                                         capacity-url
                                         url-params)}
                 {:keys [api-response status]} (a/<! (iu/api-request capacity-request))
                 {:keys [results]} api-response]
             (when (= status 200)
               (p/publish {:topics topic
                           :response results
                           :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; SDK Reporting Module
;; -------------------------------------------------------------------------- ;;

(defrecord ReportingModule []
  pr/SDKModule
  (start [this]
    (let [module-name :reporting]
      (ih/register {:api {module-name {:add-stat-subscription add-stat-subscription
                                       :remove-stat-subscription remove-stat-subscription
                                       :get-capacity get-capacity}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name}))
      (start-polling this))
  (stop [this])
  (refresh-integration [this]))
