(ns cxengage-javascript-sdk.modules.reporting
  (:require-macros [lumbajack.macros :refer [log]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
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
         module-state @(:state module)
         api-url (get-in module [:config :api-url])
         {:keys [stats interval callback]} params
         tenant-id (st/get-active-tenant-id)
         params (merge params {:tenant-id tenant-id})
         publish-fn (fn [r] (p/publish (str "reporting/polling-response") r callback))]
     (if (not (s/valid? ::polling-params params))
       (publish-fn (e/invalid-args-error (s/explain-data ::polling-params params)))
       (let [api-url (str api-url (get-in module-state [:urls :batch]))
             polling-request {:method :post
                                 :body {:requests stats}
                                 :url (iu/build-api-url-with-params
                                       api-url
                                       params)}]
         (go-loop []
           (let [{:keys [api-response status]} (a/<! (iu/api-request polling-request))
                 {:keys [results]} api-response
                 next-polling-delay (or interval 3000)]
             (if (not= status 200)
               (do (js/console.error "Batch request failed.")
                   (publish-fn (e/api-error api-response)))
               (do (js/console.info "Batch request received!")
                   (publish-fn results)
                   (a/<! (a/timeout next-polling-delay))
                   (recur))))
          nil))))))

(def initial-state
  {:module-name :reporting
   :urls {:batch "tenants/tenant-id/realtime-statistics/batch"}})

(defrecord ReportingModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)]
      (register {:api {module-name {:start-polling (partial start-polling this)}}
                 :module-name module-name})
      (a/put! core-messages< {:module-registration-status :success :module module-name})
      (js/console.info "<----- Started " (name module-name) " module! ----->")))
  (stop [this]))
