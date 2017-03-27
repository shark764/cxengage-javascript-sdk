(ns cxengage-javascript-sdk.modules.reporting
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.helpers :refer [log]]
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
              (do (log :error "Batch request failed.")
                  (p/publish {:topics topic
                              :error (e/api-error api-response)}))
              (do (log :info "Batch request received!")
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
         api-url (get-in module [:config :api-url])
         {:keys [callback]} params
         tenant-id (st/get-active-tenant-id)
         topic (p/get-topic :add-stat)
         stat-bundle (dissoc params :callback)]
     (if-not (s/valid? ::add-statistic-params params)
       (do (log :debug (s/explain-data ::add-statistic-params params))
           (p/publish {:topics topic
                       :error (e/invalid-args-error "invalid args passed to sdk fn")
                       :callback callback}))
       (let [stat-id (str (uuid/make-random-uuid))]
         (swap! module-state assoc-in [:statistics stat-id] stat-bundle)
         (p/publish {:topics topic
                     :response {:stat-id stat-id}
                     :callback callback})
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

(def initial-state
  {:module-name :reporting
   :urls {:batch "tenants/tenant-id/realtime-statistics/batch"}
   :polling-started? false
   :statistics {}})

(defrecord ReportingModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)]
      (register {:api {module-name {:add-stat-subscription (partial add-stat-subscription this)
                                    :remove-stat-subscription (partial remove-stat-subscription this)}}
                 :module-name module-name})
      (a/put! core-messages< {:module-registration-status :success :module module-name})
      (start-polling this)
      (log :info (str "<----- Started " (name module-name) " SDK module! ----->"))))
  (stop [this]))
