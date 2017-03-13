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
            [cxengage-javascript-sdk.domain.specs :as specs]))

(s/def ::polling-params
  (s/keys :req-un []
          :opt-un [:specs/callback]))

(defn start-polling
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (first others))
     (e/wrong-number-of-args-error)
     (start-polling module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         module-state (:state module)
         api-url (get-in module [:config :api-url])
         {:keys [stats interval callback]} params
         tenant-id (st/get-active-tenant-id)
         params (merge params {:tenant-id tenant-id})
         topic (p/get-topic :batch-response)]
     (if (not (s/valid? ::polling-params params))
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::polling-params params))
                   :callback callback})
       (let [api-url (str api-url (get-in @module-state [:urls :batch]))
             polling-request {:method :post
                              :body {:requests stats}
                              :url (iu/build-api-url-with-params
                                    api-url
                                    params)}]
         (p/publish {:topics (p/get-topic :polling-started)
                     :response true
                     :callback callback})
         (swap! module-state assoc :polling-started? true)
         (go-loop []
           (if (not (:polling-started? @module-state))
             (do (p/publish {:topics (p/get-topic :polling-stopped)
                             :response true
                             :callback callback})
                 nil)
             (let [{:keys [api-response status]} (a/<! (iu/api-request polling-request))
                   {:keys [results]} api-response
                   next-polling-delay (or interval 3000)]
               (if (not= status 200)
                 (do (log :error "Batch request failed.")
                     (p/publish {:topics topic
                                 :error (e/api-error api-response)
                                 :callback callback}))
                 (do (log :info "Batch request received!")
                     (p/publish {:topics topic
                                 :response results
                                 :callback callback})
                     (a/<! (a/timeout next-polling-delay))
                     (recur))))))
         nil)))))

(defn stop-polling
  ([module]
   (stop-polling module {}))
  ([module params & others]
   (if-not (fn? (first others))
     (e/wrong-number-of-args-error)
     (stop-polling module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [module-state (:state module)
         {:keys [callback]} params]
     (swap! module-state assoc :polling-started? false)
     nil)))

(def initial-state
  {:module-name :reporting
   :urls {:batch "tenants/tenant-id/realtime-statistics/batch"}
   :polling-started? false})

(defrecord ReportingModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)]
      (register {:api {module-name {:start-polling (partial start-polling this)
                                    :stop-polling (partial stop-polling this)}}
                 :module-name module-name})
      (a/put! core-messages< {:module-registration-status :success :module module-name})
      (log :info (str "<----- Started " (name module-name) " SDK module! ----->"))))
  (stop [this]))
