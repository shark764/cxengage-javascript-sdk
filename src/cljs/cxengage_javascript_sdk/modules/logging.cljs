(ns cxengage-javascript-sdk.modules.logging
  (:require-macros [cljs-sdk-utils.macros :refer [def-sdk-fn]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cljs-sdk-utils.protocols :as pr]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs-sdk-utils.interop-helpers :as ih]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cljs-sdk-utils.specs :as specs]
            [cljs-sdk-utils.errors :as e]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
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

;; -------------------------------;;
;; CxEngage.logging.setLevel({ level: "info" });
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
    (ih/set-log-level! level)
    (p/publish {:topics topic
                :response level
                :callback callback})))

;; -------------------------------;;
;; CxEngage.logging.saveLogs();
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
                                   (conj acc log))) [] (jack/get-unsaved-logs))
        logs-body {:logs logs
                   :device "client"
                   :app-id (str (uuid/make-random-squuid))
                   :app-name "CxEngage Agent Front-end"}
        {:keys [status api-response]} (a/<! (rest/save-logs-request logs-body))]
    (if (= status 200)
      (do (jack/wipe-logs!)
          (p/publish {:topics topic
                      :response api-response
                      :callback callback}))
      (p/publish {:topics topic
                  :error (e/failed-to-save-logs-err)
                  :callback callback}))))

(defn log* [level & args]
  (doseq [arg args]
    (log level arg)))

;; -------------------------------------------------------------------------- ;;
;; SDK Logging Module
;; -------------------------------------------------------------------------- ;;

(defrecord LoggingModule []
  pr/SDKModule
  (start [this]
    (let [module-name :logging]
      (ih/register {:api {module-name {:set-level set-level
                                       :save-logs save-logs
                                       :debug (partial log* :debug)
                                       :info (partial log* :info)
                                       :warn (partial log* :warn)
                                       :error (partial log* :error)
                                       :fatal (partial log* :fatal)}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
