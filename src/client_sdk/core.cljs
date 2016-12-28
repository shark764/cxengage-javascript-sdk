(ns client-sdk.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :as a]
            [clojure.string :as str]
            [lumbajack.core :as logging :refer [log]]
            [auth-sdk.core :as auth]
            [presence-sdk.core :as presence]
            [client-sdk-utils.core :as u]
            [client-sdk.state :as state]
            [client-sdk.api :as api]
            [client-sdk.pubsub :as pubsub]
            [client-sdk.sqs :as sqs]
            [client-sdk.reporting :as reporting]
            [client-sdk.flow-interrupts :as flow]
            [client-sdk.crud :as crud]))

(enable-console-print!)

(defn register-module!
  [module-name module]
  (swap! (state/get-state) assoc-in [:module-channels module-name] module))

(defn register-module-async!
  [done-registry< module]
  (let [{:keys [name config]} module]
    (case name
      :sqs (register-module! :sqs (sqs/init (state/get-env) done-registry< config)))
    (go (let [registration-status (a/<! done-registry<)]
          (log :debug (str "Async module registration status for module `" name "`:") registration-status)))))

(defn ^:export init
  ([env cljs?]
   (state/set-consumer-env! :cljs)
   (init env))
  ([env]
   (let [env (keyword env)]
     (swap! (state/get-state) assoc :env env)
     (register-module! :sub (sub/init env))
     (register-module! :logging (logging/init env {:terse? false :level :debug}))
     (register-module! :auth (auth/init env))
     (register-module! :reporting (reporting/init env))
     (register-module! :presence (presence/init env))
     (u/start-simple-consumer! (state/get-async-module-registration)
                               (partial register-module-async! (a/promise-chan)))
     (api/assemble-api))))
