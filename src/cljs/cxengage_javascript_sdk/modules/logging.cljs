(ns cxengage-javascript-sdk.modules.logging
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-cljs-utils.core :as u]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.state :as state]
            [cljs-uuid-utils.core :as uuid]
            [clojure.string :as str]
            [lumbajack.core :as jack]))

(defn format-request-logs
  [log]
  (let [{:keys [level data]} log
        date-time (js/Date.)]
    (assoc {}
           :level "info"
           :message (js/JSON.stringify (iu/camelify {:data (clojure.string/join " " data) :original-client-log-level (name level)}))
           :timestamp (.toISOString date-time))))

(defn log*
  [level & data]
  (if (some #{level} (state/get-valid-log-levels))
    (let [level (keyword level)]
      (doseq [d data]
        (apply (partial jack/log* level) d))
      (when (state/get-unsaved-logs)
        (state/append-logs! {:data data :level level}))
      nil)
    nil))

(s/def ::dump-logs-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(defn dump-logs
  ([module]
   (dump-logs module {}))
  ([module params & others]
   (if-not (fn? (first others))
     (e/wrong-number-of-args-error)
     (dump-logs module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [module-state @(:state module)
         {:keys [callback] :as params} (iu/extract-params params)
         topic (p/get-topic :logs-dumped)]
     (if-not (s/valid? ::dump-logs-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::dump-logs-params params))
                   :callback callback})
       (p/publish {:topics topic
                   :response (state/get-unsaved-logs)
                   :callback callback})))))

(s/def ::set-level-params
  (s/keys :req-un [::specs/level]
          :opt-un [::specs/callback]))

(defn set-level
  ([module]
   (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (first others))
     (e/wrong-number-of-args-error)
     (set-level module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [{:keys [level callback] :as params} (iu/extract-params params)
         topic (p/get-topic :log-level-set)
         level (keyword level)]
     (if-not (s/valid? ::set-level-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::set-level-params params))
                   :callback callback}))
     (state/set-log-level! level jack/levels))))

(s/def ::save-logs-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(defn save-logs
  ([module]
   (save-logs module {}))
  ([module params & others]
   (if-not (fn? (first others))
     (e/wrong-number-of-args-error)
     (save-logs module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [{:keys [callback] :as params} (iu/extract-params params)
         module-state @(:state module)
         api-url (get-in module [:config :api-url])
         routes (get-in module-state [:urls :save-logs])
         base-url (str api-url routes)
         logs (reduce (fn [acc x] (let [log (format-request-logs x)]
                                    (conj acc log))) [] (state/get-unsaved-logs))
         request-map {:url (iu/build-api-url-with-params base-url {:tenant-id (state/get-active-tenant-id)
                                                                   :resource-id (state/get-active-user-id)})
                      :method :post
                      :body {:logs logs
                             :device "client"
                             :app-id (str (uuid/make-random-squuid))
                             :app-name "CxEngage Javascript SDK"}}
         topic (p/get-topic :logs-saved)]
     (if-not (s/valid? ::save-logs-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::save-logs-params params))
                   :callback callback})
       (do (go (let [save-response (a/<! (iu/api-request request-map true))
                     {:keys [status api-response]} save-response
                     {:keys [result]} api-response]
                 (if (not= status 200)
                   (p/publish {:topics topic
                               :error (e/api-error api-response)
                               :callback callback})
                   (do (p/publish {:topics topic
                                   :response result
                                   :callback callback})
                       (state/save-logs)))))
           nil)))))

(def initial-state
  {:module-name :logging
   :topics {:save-logs "logs/logs-saved"
            :dump-logs "logs/logs-dump"
            :set-level "logs/level-set"}
   :urls {:save-logs "tenants/tenant-id/users/resource-id/logs"}})

(defrecord LoggingModule [config state]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [module-name (get @(:state this) :module-name)]
      (ih/register {:api {module-name {:set-level (partial set-level this)
                                    :save-logs (partial save-logs this)
                                    :dump-logs (partial dump-logs this)}}
                 :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
