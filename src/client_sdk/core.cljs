(ns client-sdk.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [clojure.string :as str]
            [lumbajack.core :as logging]
            [client-sdk-utils.core :as u]
            [client-sdk.modules.auth :as auth]
            [client-sdk.modules.presence :as presence]
            [client-sdk.modules.flow :as flow]
            [client-sdk.modules.sqs :as sqs]
            [client-sdk.modules.mqtt :as mqtt]
            [client-sdk.modules.messaging :as msg]
            [client-sdk.modules.reporting :as reporting]
            [client-sdk.modules.crud :as crud]
            [client-sdk.interaction-management :as intmgmt]
            [client-sdk.state :as state]
            [client-sdk.api :as api]
            [client-sdk.pubsub :as pubsub]
            [client-sdk.modules.twilio :as twilio]))

(enable-console-print!)

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
      :twilio (register-module! :twilio (twilio/init (state/get-env) done-registry< config pubsub/twilio-msg-router))
      :sqs (register-module! :sqs (sqs/init (state/get-env) done-registry< config pubsub/sqs-msg-router))
      :mqtt (register-module! :mqtt (mqtt/init (state/get-env) done-registry< (state/get-active-user-id) config pubsub/mqtt-msg-router))
      (log :error "Unrecognized asynchronous module registration attempt."))
    (go (a/<! done-registry<)
        (log :info (str "SDK Module `" (str/upper-case (name module-name)) "` succesfully registered (async).")))))

(defn ^:export init
  [opts]
  (let [opts (js->clj opts :keywordize-keys true)
        {:keys [env cljs terseLogs logLevel]} opts
        logLevel (if logLevel (keyword logLevel) :debug)
        env (or (keyword env) "production")]
    (state/set-consumer-type! (if cljs :cljs :js))
    (state/set-env! env)
    (register-module! :logging (logging/init env {:terse? (or terseLogs false) :level logLevel}))
    (register-module! :messaging (msg/init env))
    (register-module! :pubsub (pubsub/init env))
    (register-module! :interactions (flow/init env))
    (register-module! :authentication (auth/init env))
    (register-module! :reporting (reporting/init env))
    (register-module! :presence (presence/init env))

    (u/start-simple-consumer! (state/get-async-module-registration)
                              (partial register-module-async! (a/promise-chan)))
    (log :info "Oh shit waddup!")
    (api/assemble-api)))
