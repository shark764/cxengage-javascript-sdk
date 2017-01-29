(ns cxengage-javascript-sdk.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.domain.errors :as err]
            [clojure.string :as str]
            [lumbajack.core :as logging]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.module-gateway :as mg]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.interaction-management :as intmgmt]
            [cxengage-javascript-sdk.api :as api]
            [devtools.core :as devtools]))

(devtools/install!)
(enable-console-print!)

(defn shutdown! []
  (let [channels (reduce
                  (fn [acc modules]
                    (let [module (get modules :shutdown)]
                      (conj acc module)))
                  []
                  (vals (get @(state/get-state) :module-channels)))]
    (doseq [shutdown-channel channels] (a/put! shutdown-channel :shutdown))))

(s/def ::env #{"dev" "qe" "staging" "production"})
(s/def ::cljs boolean?)
(s/def ::terseLogs boolean?)
(s/def ::logLevel #{"debug" "info" "warn" "error" "fatal" "off"})
(s/def ::init-params
  (s/keys :req-un []
          :opt-un [::env ::cljs ::terseLogs ::logLevel]))

(defn init
  ([] (init {}))
  ([params]
   (let [params (iu/extract-params params)]
     (if-not (s/valid? ::init-params params)
       (iu/format-response (err/invalid-params-err))
       (let [{:keys [env cljs terseLogs logLevel]} params
             logLevel (or (keyword logLevel) :debug)
             env (or (keyword env) :production)]
         (state/set-consumer-type! (or cljs :js))
         (state/set-env! env)
         (mg/start-modules env terseLogs logLevel intmgmt/twilio-msg-router intmgmt/mqtt-msg-router intmgmt/sqs-msg-router)
         (api/assemble-api))))))
