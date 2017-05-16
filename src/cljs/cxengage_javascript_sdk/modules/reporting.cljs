(ns cxengage-javascript-sdk.modules.reporting
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
    (go-loop []
      (a/<! (a/timeout polling-delay))
      (if (empty? (:statistics @stat-subscriptions))
        (recur)
        (let [polling-request {:method :post
                               :body {:requests (:statistics @stat-subscriptions)}
                               :url (iu/api-url
                                     "tenants/tenant-id/realtime-statistics/batch"
                                     {:tenant-id tenant-id})}
              {:keys [api-response status]} (a/<! (iu/api-request polling-request true))
              {:keys [results]} api-response]
          (if (not= status 200)
            (do (js/console.error "Batch request failed.")
                (p/publish {:topics topic
                            :error (e/reporting-batch-request-failed-err)}))
            (do (js/console.info "Batch request received!")
                (p/publish {:topics topic
                            :response results})

                (recur))))))
    nil))

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
  {:validation ::add-statistic-params
   :topic-key :add-stat}
  [params]
  (let [{:keys [topic callback]} params
        tenant-id (st/get-active-tenant-id)
        stat-bundle (dissoc params :callback)
        stat-id (str (uuid/make-random-uuid))]
    (swap! stat-subscriptions assoc-in [:statistics stat-id] stat-bundle)
    (p/publish {:topics topic
                :response {:stat-id stat-id}
                :callback callback})
    (let [polling-request {:method :post
                           :body {:requests (:statistics @stat-subscriptions)}
                           :url (iu/api-url
                                 "tenants/tenant-id/realtime-statistics/batch"
                                 {:tenant-id tenant-id})}
          {:keys [api-response status]} (a/<! (iu/api-request polling-request true))
          {:keys [results]} api-response
          batch-topic (p/get-topic :batch-response)]
      (when (= status 200)
        (p/publish {:topics batch-topic
                    :response results
                    :callback callback}
                   true)))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.removeStatSubscription({
;;   statId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::remove-statistics-params
  (s/keys :req-un [::specs/stat-id]
          :opt-un [::specs/callback]))

(def-sdk-fn remove-stat-subscription
  {:validation ::remove-statistics-params
   :topic-key :remove-stat
   :preserve-casing? true}
  [params]
  (let [{:keys [stat-id topic callback]} params
        new-stats (dissoc (:statistics @stat-subscriptions) stat-id)]
    (swap! stat-subscriptions assoc :statistics new-stats)
    (p/publish {:topics topic
                :response new-stats
                :callback callback})))


;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.getCapacity({
;;   resourceId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-capacity-params
  (s/keys :req-un []
          :opt-un [::specs/callback ::specs/resource-id]))

(def-sdk-fn get-capacity
  {:validation ::get-capacity-params
   :topic-key :get-capacity-response}
  [params]
  (let [tenant-id (st/get-active-tenant-id)
        {:keys [resource-id topic callback]} params
        url (if resource-id
              "tenants/tenant-id/users/resource-id/realtime-statistics/resource-capacity"
              "tenants/tenant-id/realtime-statistics/resource-capacity")
        ;; If resource-id is passed to the function, it will return the Capacity
        ;; for the specified resource-id. If no arguments are passed to the function
        ;; it will instead return the capacity for the active user's selected Tenant
        url-params (if resource-id {:tenant-id tenant-id :resource-id resource-id} {:tenant-id tenant-id})
        capacity-request {:method :get
                          :url (iu/api-url
                                url
                                url-params)}
        {:keys [api-response status]} (a/<! (iu/api-request capacity-request))
        {:keys [results]} api-response]
    (when (= status 200)
      (p/publish {:topics topic
                  :response results
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.statQuery({
;;   statistic: "{{string}}",
;;   resourceId: "{{uuid}}",
;;   queueId: "{{queueId}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::stat-query-params
  (s/keys :req-un [::specs/statistic]
          :opt-un [::specs/callback ::specs/queue-id ::specs/resource-id]))

(def-sdk-fn stat-query
  {:validation ::stat-query-params
   :topic-key :get-stat-query-response
   :preserve-casing? true}
  [params]
  (let [{:keys [statistic topic callback]} params
        tenant-id (st/get-active-tenant-id)
        stat-bundle (dissoc params :callback :topic)
        stat-id (str (uuid/make-random-uuid))
        stat-body (assoc {} stat-id stat-bundle)
        polling-request {:method :post
                         :body {:requests stat-body}
                         :url (iu/api-url
                               "tenants/tenant-id/realtime-statistics/batch"
                               {:tenant-id tenant-id})}
        {:keys [api-response status]} (a/<! (iu/api-request polling-request))
        {:keys [results]} api-response]
    (when (= status 200)
      (p/publish {:topics topic
                  :response results
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.getAvailableStats();
;; -------------------------------------------------------------------------- ;;

(s/def ::get-available-stats-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn get-available-stats
  {:validation ::get-available-stats-params
   :topic-key :get-available-stats-response}
  [params]
  (let [{:keys [callback topic]} params
        tenant-id (st/get-active-tenant-id)
        get-available-stats-request {:method :get
                                     :url (iu/api-url
                                           "tenants/tenant-id/realtime-statistics/available?client=toolbar"
                                           {:tenant-id tenant-id})}
        {:keys [status api-response]} (a/<! (iu/api-request get-available-stats-request))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.getContactInteractionHistory({
;;   contactId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-contact-interaction-history-params
  (s/keys :req-un [::specs/contact-id]
          :opt-un [::specs/callback ::specs/page]))

(def-sdk-fn get-contact-interaction-history
  {:validation ::get-contact-interaction-history-params
   :topic-key :get-contact-interaction-history-response}
  [params]
  (let [{:keys [callback topic contact-id page]} params
        tenant-id (st/get-active-tenant-id)
        url (if page
              (str "tenants/tenant-id/contacts/contact-id/interactions?page=" page)
              "tenants/tenant-id/contacts/contact-id/interactions")
        get-contact-interaction-history-request {:method :get
                                                 :url (iu/api-url
                                                       url
                                                       {:tenant-id tenant-id
                                                        :contact-id contact-id})}
        {:keys [status api-response]} (a/<! (iu/api-request get-contact-interaction-history-request))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.getInteraction({
;;   interactionId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-interaction-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-interaction
  {:validation ::get-interaction-params
   :topic-key :get-interaction-response}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        tenant-id (st/get-active-tenant-id)
        get-interaction-request {:method :get
                                 :url (iu/api-url
                                       "tenants/tenant-id/interactions/interaction-id"
                                       {:tenant-id tenant-id
                                        :interaction-id interaction-id})}
        {:keys [status api-response]} (a/<! (iu/api-request get-interaction-request))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; SDK Reporting Module
;; -------------------------------------------------------------------------- ;;

(defrecord ReportingModule []
  pr/SDKModule
  (start [this]
    (let [module-name :reporting]
      (ih/register {:api {module-name {:add-stat-subscription add-stat-subscription
                                       :remove-stat-subscription remove-stat-subscription
                                       :get-capacity get-capacity
                                       :stat-query stat-query
                                       :get-available-stats get-available-stats
                                       :get-contact-interaction-history get-contact-interaction-history
                                       :get-interaction get-interaction}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name}))
    (start-polling this))
  (stop [this])
  (refresh-integration [this]))
