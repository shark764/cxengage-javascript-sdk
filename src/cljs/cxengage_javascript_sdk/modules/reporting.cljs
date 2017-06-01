(ns cxengage-javascript-sdk.modules.reporting
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]]
                   [cljs-sdk-utils.macros :refer [def-sdk-fn]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cljs-sdk-utils.interop-helpers :as ih]
            [cljs-sdk-utils.protocols :as pr]
            [cljs-sdk-utils.errors :as e]
            [cljs-sdk-utils.topics :as topics]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cljs-sdk-utils.specs :as specs]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs-uuid-utils.core :as uuid]))

(def stat-subscriptions (atom {}))

(defn start-polling
  [module]
  (let [topic (topics/get-topic :batch-response)
        polling-delay (st/get-reporting-refresh-rate)]
    (go-loop []
      (a/<! (a/timeout polling-delay))
      (if (empty? (:statistics @stat-subscriptions))
        (recur)
        (let [batch-body {:requests (:statistics @stat-subscriptions)}
              {:keys [api-response status]} (a/<! (rest/batch-request batch-body))
              {:keys [results]} api-response]
          (if (not= status 200)
            (do (log :error "Batch request failed.")
                (p/publish {:topics topic
                            :error (e/reporting-batch-request-failed-err)}))
            (do (log :info "Batch request received!")
                (p/publish {:topics topic
                            :response results
                            :preserve-casing? true})
                (recur))))))
    nil))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.reporting.addStatSubscription({
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
        stat-bundle (dissoc params :callback)
        stat-id (str (uuid/make-random-uuid))]
    (swap! stat-subscriptions assoc-in [:statistics stat-id] stat-bundle)
    (p/publish {:topics topic
                :response {:statId stat-id}
                :callback callback
                :preserve-casing? true})
    (let [batch-body {:requests (:statistics @stat-subscriptions)}
          {:keys [api-response status]} (a/<! (rest/batch-request batch-body))
          {:keys [results]} api-response
          batch-topic (topics/get-topic :batch-response)]
      (when (= status 200)
        (p/publish {:topics batch-topic
                    :response results
                    :callback callback
                    :preserve-casing? true})))))

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
   :topic-key :remove-stat}
  [params]
  (let [{:keys [stat-id topic callback]} params
        new-stats (dissoc (:statistics @stat-subscriptions) stat-id)]
    (swap! stat-subscriptions assoc :statistics new-stats)
    (p/publish {:topics topic
                :response new-stats
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
  {:validation ::get-capacity-params
   :topic-key :get-capacity-response}
  [params]
  (let [{:keys [resource-id topic callback]} params
        {:keys [api-response status]} (a/<! (rest/get-capacity-request resource-id))
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
        stat-bundle (dissoc params :callback :topic)
        stat-id (str (uuid/make-random-uuid))
        stat-body {stat-id stat-bundle}
        {:keys [api-response status]} (a/<! (rest/batch-request stat-body))
        {:keys [results]} api-response]
    (when (= status 200)
      (p/publish {:topics topic
                  :response results
                  :callback callback
                  :preserve-casing? true}))))

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
        {:keys [status api-response]} (a/<! (rest/get-available-stats-request))]
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
        {:keys [status api-response]} (a/<! (rest/get-contact-interaction-history-request contact-id page))]
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
        {:keys [status api-response]} (a/<! (rest/get-interaction-history-request interaction-id))]
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
