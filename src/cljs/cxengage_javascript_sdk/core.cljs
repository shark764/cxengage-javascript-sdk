(ns cxengage-javascript-sdk.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.domain.errors :as err]
            [clojure.string :as str]
            [lumbajack.core :as logging]
            [cxengage-cljs-utils.core :as u]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.module-gateway :as mg]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.interaction-management :as intmgmt]
            [cxengage-javascript-sdk.api :as api]
            [cxengage-javascript-sdk.shutdown :as shutdown]))
(s/def ::baseUrl string?)
(s/def ::cljs boolean?)
(s/def ::terseLogs boolean?)
(s/def ::logLevel #{"debug" "info" "warn" "error" "fatal" "off"})
(s/def ::env #{"dev" "qe" "staging" "prod"})
(s/def ::init-params
  (s/keys :req-un []
          :opt-un [::env ::baseUrl ::cljs ::terseLogs ::logLevel]))

(defn init
  ([] (init {}))
  ([params]
   (let [params (iu/extract-params params)]
     (if-not (s/valid? ::init-params params)
       (iu/format-response (err/invalid-params-err))
       (let [{:keys [env baseUrl cljs terseLogs logLevel blastSqsOutput]} params
             logLevel (or (keyword logLevel) :debug)
             env (or (keyword env) :prod)
             core-chan (a/chan)
             publication (mg/start-modules terseLogs logLevel intmgmt/twilio-msg-router intmgmt/mqtt-msg-router intmgmt/sqs-msg-router)]
         (state/set-consumer-type! (or cljs :js))
         (state/set-blast-sqs-output! (or blastSqsOutput false))
         (state/set-base-api-url! baseUrl)
         (state/set-env! env)
         (a/sub publication :core/SHUTDOWN core-chan)
         (u/start-simple-consumer! core-chan shutdown/msg-router)
         (api/assemble-api))))))
