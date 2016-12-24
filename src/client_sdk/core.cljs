(ns client-sdk.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :as a]
            [clojure.string :as str]
            [client-sdk-utils.core :as u]
            [lumbajack.core :as logging :refer [log]]
            [client-sdk.state :as state]
            [client-sdk.api :as api]
            [client-sdk.pubsub :as pubsub]
            [auth-sdk.core :as auth]
            [client-sdk.sqs :as sqs]
            [presence-sdk.core :as presence]
            [client-sdk.flow-interrupts :as flow]
            [client-sdk.reporting :as reporting]))

(enable-console-print!)

(defn register-module!
  [module-name module]
  (swap! (state/get-state) assoc-in [:module-channels module-name] module))

(defn register-module-async! [done-registry< module]
  (let [{:keys [name config]} module]
    (case name
      :sqs (register-module! :sqs (sqs/init done-registry< config)))
    (go (let [registration-status (a/<! done-registry<)]
          (log :debug (str "Async module registration status for module `" name "`:") registration-status)))))

(defn ^:export init
  []
  (register-module! :pubsub (pubsub/init))
  (register-module! :logging (logging/init {:terse? false :level :debug}))
  (register-module! :auth (auth/init))
  (register-module! :flow (flow/init))
  (register-module! :reporting (reporting/init))
  (register-module! :presence (presence/init))
  (u/start-simple-consumer! (state/get-async-module-registration)
                            (partial register-module-async! (a/promise-chan)))
  (log :debug "state" @(state/get-state))
  (api/assemble-api))
