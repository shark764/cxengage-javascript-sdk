(ns cxengage-javascript-sdk.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.domain.errors :as err]
            [clojure.string :as str]
            [lumbajack.core :as logging]
            [cxengage-cljs-utils.core :as u]
            [cxengage-javascript-sdk.modules.auth :as auth]
            [cxengage-javascript-sdk.modules.presence :as presence]
            [cxengage-javascript-sdk.modules.flow :as flow]
            [cxengage-javascript-sdk.modules.sqs :as sqs]
            [cxengage-javascript-sdk.modules.mqtt :as mqtt]
            [cxengage-javascript-sdk.modules.messaging :as msg]
            [cxengage-javascript-sdk.modules.reporting :as reporting]
            [cxengage-javascript-sdk.modules.crud :as crud]
            [cxengage-javascript-sdk.modules.contacts :as contacts]
            [cxengage-javascript-sdk.interaction-management :as intmgmt]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.api :as api]
            [cxengage-javascript-sdk.pubsub :as pubsub]
            [cxengage-javascript-sdk.modules.twilio :as twilio]
            [cxengage-javascript-sdk.internal-utils :as iu]))

(defn shutdown! []
  (let [channels (reduce
                  (fn [acc modules]
                    (let [module (get modules :shutdown)]
                      (conj acc module)))
                  []
                  (vals (get @(state/get-state) :module-channels)))]
    (doseq [shutdown-channel channels] (a/put! shutdown-channel :shutdown))))

(defn register-module!
  [module-name module]
  (swap! (state/get-state) assoc-in [:module-channels module-name] module))

(defn register-module-async!
  [done-registry< module]
  (let [{:keys [module-name config]} module]
    (case module-name
      :twilio (register-module! :twilio (twilio/init (state/get-env) done-registry< config intmgmt/twilio-msg-router))
      :sqs (register-module! :sqs (sqs/init (state/get-env) done-registry< config intmgmt/sqs-msg-router))
      :mqtt (register-module! :mqtt (mqtt/init (state/get-env) done-registry< (state/get-active-user-id) config intmgmt/mqtt-msg-router))
      (log :error "Unrecognized asynchronous module registration attempt."))
    (go (a/<! done-registry<)
        (log :debug (str "SDK Module `" (str/upper-case (name module-name)) "` succesfully registered (async).")))))

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
         (register-module! :logging (logging/init env {:terse? (or terseLogs false) :level logLevel}))
         (register-module! :messaging (msg/init env))
         (register-module! :pubsub (pubsub/init env))
         (register-module! :interactions (flow/init env))
         (register-module! :authentication (auth/init env))
         (register-module! :reporting (reporting/init env))
         (register-module! :presence (presence/init env))
         (register-module! :contacts (contacts/init env))
         (u/start-simple-consumer! (state/get-async-module-registration)
                                   (partial register-module-async! (a/promise-chan)))
         (api/assemble-api))))))
