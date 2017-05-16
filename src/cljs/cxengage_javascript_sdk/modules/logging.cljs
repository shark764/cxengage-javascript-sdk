(ns cxengage-javascript-sdk.modules.logging
  (:require-macros [cxengage-javascript-sdk.macros :refer [def-sdk-fn]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.state :as state]
            [cljs-uuid-utils.core :as uuid]
            [lumbajack.core :as jack]))

(defn format-request-logs
  [log]
  (let [{:keys [level data]} log
        date-time (js/Date.)]
    (assoc {}
           :level "info"
           :message (js/JSON.stringify (ih/camelify {:data (clojure.string/join " " data) :original-client-log-level (name level)}))
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

;; -------------------------------;;
;;
;; CxEngage.logging.dumpLogs();
;;
;; -------------------------------;;

(s/def ::dump-logs-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn dump-logs
  {:validation ::dump-logs-params
   :topic-key :logs-dumped}
  [params]
  (let [{:keys [topic callback]} params]
    (p/publish {:topics topic
                :response (state/get-unsaved-logs)
                :callback callback})))

;; -------------------------------;;
;;
;; CxEngage.logging.setLevel({level: "info"});
;;
;; -------------------------------;;

(s/def ::set-level-params
  (s/keys :req-un [::specs/level]
          :opt-un [::specs/callback]))

(def-sdk-fn set-level
  {:validation ::set-level-params
   :topic-key :log-level-set}
  [params]
  (let [{:keys [level topic callback]} params
        level (keyword level)]
    (state/set-log-level! level jack/levels)
    (p/publish {:topics topic
                :response (state/get-log-level)
                :callback callback})))

;; -------------------------------;;
;;
;; CxEngage.logging.saveLogs();
;;
;; -------------------------------;;

(s/def ::save-logs-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn save-logs
  {:validation ::save-logs-params
   :topic-key :logs-saved}
  [params]
  (let [{:keys [topic callback]} params
        logs (reduce (fn [acc x] (let [log (format-request-logs x)]
                                   (conj acc log))) [] (state/get-unsaved-logs))
        request-map {:url (iu/api-url
                           "tenants/:tenant-id/users/:resource-id/logs"
                           {:tenant-id (state/get-active-tenant-id)
                            :resource-id (state/get-active-user-id)})
                     :method :post
                     :body {:logs logs
                            :device "client"
                            :app-id (str (uuid/make-random-squuid))
                            :app-name "CxEngage Javascript SDK"}}
        {:keys [status api-response]} (a/<! (iu/api-request request-map true))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (state/save-logs))))

(defrecord LoggingModule []
  pr/SDKModule
  (start [this]
    (let [module-name :logging]
      (ih/register {:api {module-name {:set-level set-level
                                       :save-logs save-logs
                                       :dump-logs dump-logs}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
