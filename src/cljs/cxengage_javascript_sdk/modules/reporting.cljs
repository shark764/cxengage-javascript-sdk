(ns cxengage-javascript-sdk.modules.reporting
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
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

(defn start-polling
  [module]
  (let [module-state (:state module)
        api-url (get-in module [:config :api-url])
        tenant-id (st/get-active-tenant-id)
        topic (p/get-topic :batch-response)
        polling-delay (get-in module [:config :reporting-refresh-rate])]
    (let [api-url (str api-url (get-in @module-state [:urls :batch]))]
      (go-loop []
        (a/<! (a/timeout polling-delay))
        (if (empty? (:statistics @module-state))
          (recur)
          (let [polling-request {:method :post
                                 :body {:requests (:statistics @module-state)}
                                 :url (iu/build-api-url-with-params
                                       api-url
                                       {:tenant-id tenant-id})}
                {:keys [api-response status]} (a/<! (iu/api-request polling-request true))
                {:keys [results]} api-response]
            (if (not= status 200)
              (do (js/console.error "Batch request failed.")
                  (p/publish {:topics topic
                              :error (e/api-error api-response)}))
              (do (js/console.info "Batch request received!")
                  (p/publish {:topics topic
                              :response results})

                  (recur))))))
      nil)))

(s/def statistic string?)

(s/def ::add-statistic-params
  (s/keys :req-un [::statistic]
          :opt-un [::specs/callback ::specs/queue-id ::specs/resource-id]))

(defn add-stat-subscription
  ([module]
   (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (add-stat-subscription module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         module-state (:state module)
         {:keys [callback]} params
         tenant-id (st/get-active-tenant-id)
         topic (p/get-topic :add-stat)
         stat-bundle (dissoc params :callback)]
     (if-not (s/valid? ::add-statistic-params params)
       (do (js/console.log (s/explain-data ::add-statistic-params params))
           (p/publish {:topics topic
                       :error (e/invalid-args-error "invalid args passed to sdk fn")
                       :callback callback}))
       (let [stat-id (str (uuid/make-random-uuid))]
         (swap! module-state assoc-in [:statistics stat-id] stat-bundle)
         (p/publish {:topics topic
                     :response {:stat-id stat-id}
                     :callback callback})
         (go (let [batch-url (str (st/get-base-api-url) (get-in @module-state [:urls :batch]))
                   polling-request {:method :post
                                    :body {:requests (:statistics @module-state)}
                                    :url (iu/build-api-url-with-params
                                          batch-url
                                          {:tenant-id tenant-id})}
                   {:keys [api-response status]} (a/<! (iu/api-request polling-request true))
                   {:keys [results]} api-response
                   batch-topic (p/get-topic :batch-response)]
               (if (not= status 200)
                 (p/publish {:topics batch-topic
                             :error (e/api-error "api returned an error")})
                 (p/publish {:topics batch-topic
                             :response results}))))
         nil)))))

(s/def ::remove-statistics-params
  (s/keys :req-un [::specs/stat-id]
          :opt-un [::specs/callback]))

(defn remove-stat-subscription
  ([module]
   (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (remove-stat-subscription module (merge (iu/extract-params params {:callback (first others)})))))
  ([module params]
   (let [params (iu/extract-params params)
         module-state (:state module)
         api-url (get-in module [:config :api-url])
         {:keys [stat-id callback]} params
         topic (p/get-topic :remove-stat)]
     (if-not (s/valid? ::remove-statistics-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::remove-statistics-params params))
                   :callback callback})
       (let [new-stats (dissoc (:statistics @module-state) stat-id)]
         (swap! module-state assoc :statistics new-stats)
         (p/publish {:topics topic
                     :response new-stats
                     :callback callback})
         nil)))))


(s/def ::get-capacity-params
  (s/keys :req-un []
          :opt-un [::specs/callback ::specs/resource-id]))

(defn get-capacity
  ([module]
   (get-capacity module {}))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (get-capacity module (merge (iu/extract-params params {:callback (first others)})))))
  ([module params]
   (let [params (iu/extract-params params)
         module-state (:state module)
         api-url (get-in module [:config :api-url])
         tenant-id (st/get-active-tenant-id)
         {:keys [resource-id callback]} params
         url (if resource-id :capacity-user :capacity-tenant)
         topic (p/get-topic :get-capacity-response)]
     (if-not (s/valid? ::get-capacity-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::get-capacity-params params))
                   :callback callback})
       (go (let [capacity-url (str (st/get-base-api-url) (get-in @module-state [:urls url]))
                 url-params (if resource-id {:tenant-id tenant-id :resource-id resource-id} {:tenant-id tenant-id})
                 capacity-request {:method :get
                                   :url (iu/build-api-url-with-params
                                         capacity-url
                                         url-params)}
                 {:keys [api-response status]} (a/<! (iu/api-request capacity-request))
                 {:keys [results]} api-response]
             (if (not= status 200)
               (p/publish {:topics topic
                           :error (e/api-error "api returned an error")})
               (p/publish {:topics topic
                           :response results})))))
     nil)))

(def initial-state
  {:module-name :reporting
   :urls {:batch "tenants/tenant-id/realtime-statistics/batch"
          :capacity-user "tenants/tenant-id/users/resource-id/realtime-statistics/resource-capacity"
          :capacity-tenant "tenants/tenant-id/realtime-statistics/resource-capacity"}
   :polling-started? false
   :statistics {}})

(defrecord ReportingModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [module-name (get @(:state this) :module-name)]
      (ih/register {:api {module-name {:add-stat-subscription (partial add-stat-subscription this)
                                    :remove-stat-subscription (partial remove-stat-subscription this)
                                    :get-capacity (partial get-capacity this)}}
                 :module-name module-name})
      (start-polling this)
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
