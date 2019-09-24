(ns cxengage-javascript-sdk.modules.reporting
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]])
  (:require [cljs.spec.alpha :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs-uuid-utils.core :as uuid]))

(def ^:no-doc stat-subscriptions (atom {}))

(defn- start-polling
  [module]
  (let [topic (topics/get-topic :batch-response)
        polling-delay (st/get-reporting-refresh-rate)]
    (go-loop [polling-delay-param polling-delay]
      (a/<! (a/timeout polling-delay-param))
      (if (empty? (:statistics @stat-subscriptions))
        (recur polling-delay)
        (let [batch-body (:statistics @stat-subscriptions)
              {:keys [api-response status]} (a/<! (rest/batch-request batch-body))
              {:keys [results]} api-response]
          (if (not= status 200)
            (do (log :error "Batch request failed.")
                (p/publish {:topics topic
                            :error (e/reporting-batch-request-failed-err batch-body batch-body)})
                (recur (min (* polling-delay-param 2) 60000)))
            (do (log :info "Batch request received!")
                (p/publish {:topics topic
                            :response results
                            :preserve-casing? true})
                (recur polling-delay))))))
    nil))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.addStatSubscription({
;;   statistic: "{{string}}",
;;   resourceId: "{{uuid}}", [Optional]
;;   queueId: "{{uuid}}", [Optional]
;;   statId: "{{uuid}}", [Optional]
;;   triggerBatch: "{{boolean}}" [Optional, default true]
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::add-statistic-params
  (s/keys :req-un [::specs/statistic]
          :opt-un [::specs/callback ::specs/queue-id ::specs/resource-id ::specs/stat-id ::specs/trigger-batch]))

(def-sdk-fn add-stat-subscription
  ""
  {:validation ::add-statistic-params
   :topic-key :add-stat}
  [params]
  (let [{:keys [topic stat-id trigger-batch callback]} params
        stat-bundle (dissoc params :callback :stat-id :topic)
        stat-id (or stat-id (str (uuid/make-random-uuid)))
        trigger-batch (if (nil? trigger-batch)
                        true
                        trigger-batch)]
    (swap! stat-subscriptions assoc-in [:statistics stat-id] stat-bundle)
    (p/publish {:topics topic
                :response {:statId stat-id}
                :callback callback
                :preserve-casing? true})
    (when 
      trigger-batch
      (let [batch-body (:statistics @stat-subscriptions)
            {:keys [api-response status] :as batch-response} (a/<! (rest/batch-request batch-body))
            {:keys [results]} api-response
            batch-topic (topics/get-topic :batch-response)]
        (if (= status 200)
          (p/publish {:topics batch-topic
                      :response results
                      :preserve-casing? true})
          (p/publish {:topics batch-topic
                      :error (e/reporting-batch-request-failed-err batch-body batch-response)}))))))

(s/def ::bulk-stat-params
  (s/keys :req-un [::specs/queries]
          :opt-un [::specs/callback]))

(def-sdk-fn bulk-add-stat-subscription
  "Adds multiple statisitics to the polling batch requests and automatically initiates a batch request
   when the stats have been added.
   
   Returns the list the ids for the stats that were added (in the same order they were passed in)
   
   ``` javascript
   CxEngage.reporting.bulkAddStatSubscription({
     queries: {{array of stat objects}}
   });
   ```
  
   Stat Objects:
   ``` javascript
   {statistic: {{stat-name}},
    queueId: {{uuid}}, <Optional>
    resourceId: {{uuid}}, <Optional>
    statId: {{uuid}} <Optional>}
   ```
   
   Possible errors:
   - [Reporting: 12000](/cxengage-javascript-sdk.domain.errors.html#var-reporting-batch-request-failed-err) (on cxengage/reporting/batch-response topic)"
  {:validation ::bulk-stat-params
   :topic-key :bulk-add-stat}
  [params]
  (let [{:keys [queries topic callback]} params
         stat-id-list (reduce #(let [stat-bundle (dissoc %2 :stat-id)
                                     stat-id (or (:stat-id %2) (str (uuid/make-random-uuid)))]
                                  (swap! stat-subscriptions assoc-in [:statistics stat-id] stat-bundle)
                                  (conj %1 stat-id))
                              []
                              queries)]
    (p/publish {:topics topic
                :response {:statIds stat-id-list}
                :callback callback
                :preserve-casing? true})
    (let [batch-body (:statistics @stat-subscriptions)
          {:keys [api-response status] :as batch-response} (a/<! (rest/batch-request batch-body))
          {:keys [results]} api-response
          batch-topic (topics/get-topic :batch-response)]
      (p/publish {:topics batch-topic
                  :response results
                  :error (when (not= status 200) (e/reporting-batch-request-failed-err batch-body batch-response))
                  :preserve-casing? true}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.triggerBatch();
;; -------------------------------------------------------------------------- ;;

(s/def ::trigger-batch-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn trigger-batch
  ""
  {:validation ::trigger-batch-params
    :topic-key :batch-response}
  [params]
  (let [{:keys [topic callback]} params
        batch-body (:statistics @stat-subscriptions)
        {:keys [api-response status] :as batch-response} (a/<! (rest/batch-request batch-body))
        {:keys [results]} api-response]
    (if (= status 200)
      (p/publish {:topics topic
                  :response results
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/reporting-batch-request-failed-err batch-body batch-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.removeStatSubscription({
;;   statId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::remove-statistics-params
  (s/keys :req-un [::specs/stat-id]
          :opt-un [::specs/callback]))

(def-sdk-fn remove-stat-subscription
  ""
  {:validation ::remove-statistics-params
   :topic-key :remove-stat}
  [params]
  (let [{:keys [stat-id topic callback]} params]
    (swap! stat-subscriptions iu/dissoc-in [:statistics stat-id])
    (p/publish {:topics topic
                :response (:statistics @stat-subscriptions)
                :callback callback
                :preserve-casing? true})))

(s/def ::bulk-remove-statistics-params
  (s/keys :req-un [::specs/stat-ids]
          :opt-un [::specs/callback]))

(def-sdk-fn bulk-remove-stat-subscription
  "Removed multiple statistics from the polling batch requests.
   
   Returns all of the stats that are being polled on after the removal.
   
   ``` javascript
   CxEngage.reporting.bulkRemoveStatSubscription({
     statIds: {{array of statId}}
   });
   ```"
  {:validation ::bulk-remove-statistics-params
   :topic-key :bulk-remove-stat}
  [params]
  (let [{:keys [stat-ids topic callback]} params]
    (doseq [id stat-ids] (swap! stat-subscriptions iu/dissoc-in [:statistics id]))
    (p/publish {:topics topic
                :response (:statistics @stat-subscriptions)
                :callback callback
                :preserve-casing? true})))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.getCapacity({
;;   resourceId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-capacity-params
  (s/keys :req-un []
          :opt-un [::specs/callback ::specs/resource-id]))

(def-sdk-fn get-capacity
  ""
  {:validation ::get-capacity-params
   :topic-key :get-capacity-response}
  [params]
  (let [{:keys [resource-id topic callback]} params
        {:keys [api-response status] :as capacity-response} (a/<! (rest/get-capacity-request resource-id))
        {:keys [results]} api-response]
    (if (= status 200)
      (p/publish {:topics topic
                  :response results
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-capacity-err capacity-response)
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
  ""
  {:validation ::stat-query-params
   :topic-key :get-stat-query-response
   :preserve-casing? true}
  [params]
  (let [{:keys [statistic topic callback]} params
        stat-bundle (dissoc params :callback :topic)
        stat-id (str (uuid/make-random-uuid))
        stat-body {stat-id stat-bundle}
        {:keys [api-response status] :as batch-response} (a/<! (rest/batch-request stat-body))
        {:keys [results]} api-response]
    (if (= status 200)
      (p/publish {:topics topic
                  :response results
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-perform-stat-query-err stat-bundle batch-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.bulkStatQuery({
;;   queries: {{array of stat objects}}
;; });
;;
;; Stat Objects:
;; {statistic: {{stat-name}},
;;  queueId: {{uuid}}, [Optional]
;;  resourceId: {{uuid}} [Optional]}
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn bulk-stat-query
  ""
  {:validation ::bulk-stat-params
   :topic-key :get-bulk-stat-query-response
   :preserve-casing? true}
  [params]
  (let [{:keys [queries topic callback]} params
        stat-body (reduce #(assoc %1 (str (or (get %2 :statId) (uuid/make-random-uuid))) %2)
                          {}
                          queries)
        {:keys [api-response status] :as batch-response} (a/<! (rest/batch-request stat-body))
        {:keys [results]} api-response]
    (if (= status 200)
      (p/publish {:topics topic
                  :response results
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-perform-bulk-stat-query-err stat-body batch-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.getAvailableStats();
;; -------------------------------------------------------------------------- ;;

(s/def ::get-available-stats-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn get-available-stats
  ""
  {:validation ::get-available-stats-params
   :topic-key :get-available-stats-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as stats-response} (a/<! (rest/get-available-stats-request))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-available-stats-err stats-response)
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
  ""
  {:validation ::get-contact-interaction-history-params
   :topic-key :get-contact-interaction-history-response}
  [params]
  (let [{:keys [callback topic contact-id page]} params
        {:keys [status api-response] :as contact-response} (a/<! (rest/get-contact-interaction-history-request contact-id page))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-contact-interaction-history-err contact-id contact-response)
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
  ""
  {:validation ::get-interaction-params
   :topic-key :get-interaction-response}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        {:keys [status api-response] :as history-response} (a/<! (rest/get-interaction-history-request interaction-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-interaction-reporting-err interaction-id history-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.getCrmInteraction({
;;   id: {{number}},
;;   crm: {{string}},
;;   subType: {{string}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-crm-interactions-params
  (s/keys :req-un [::specs/id ::specs/crm ::specs/sub-type]
          :opt-un [::specs/callback ::specs/page]))

(def-sdk-fn get-crm-interactions
  ""
  {:validation ::get-crm-interactions-params
   :topic-key :get-crm-interactions-response}
  [params]
  (let [{:keys [callback topic id crm sub-type page]} params
        {:keys [status api-response] :as history-response} (a/<! (rest/get-crm-interactions-request id crm sub-type page))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-crm-interactions-err id crm sub-type history-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; SDK Reporting Module
;; -------------------------------------------------------------------------- ;;

(defrecord ReportingModule []
  pr/SDKModule
  (start [this]
    (let [module-name :reporting]
      (ih/register {:api {module-name {:add-stat-subscription add-stat-subscription
                                       :bulk-add-stat-subscription bulk-add-stat-subscription
                                       :remove-stat-subscription remove-stat-subscription
                                       :bulk-remove-stat-subscription bulk-remove-stat-subscription
                                       :trigger-batch trigger-batch
                                       :get-capacity get-capacity
                                       :stat-query stat-query
                                       :bulk-stat-query bulk-stat-query
                                       :get-available-stats get-available-stats
                                       :get-contact-interaction-history get-contact-interaction-history
                                       :get-interaction get-interaction
                                       :get-crm-interactions get-crm-interactions}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name}))
    (start-polling this))
  (stop [this])
  (refresh-integration [this]))
