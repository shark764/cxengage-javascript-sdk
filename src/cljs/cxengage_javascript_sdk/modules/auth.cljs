(ns cxengage-javascript-sdk.modules.auth
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-cljs-utils.core :as u]))

(def module-state (atom {}))

(defn login
  [message]
  (let [{:keys [token resp-chan]} message
        request-map {:method :post
                     :url (str (state/get-base-api-url) "/login")
                     :token token
                     :resp-chan resp-chan}]
    (u/api-request request-map)))

(defn config [message]
  [message]
  (let [{:keys [token resp-chan tenant-id user-id]} message
        request-map {:method :get
                     :url (str (state/get-base-api-url) "/tenants/" tenant-id "/users/" user-id "/config")
                     :token token
                     :resp-chan resp-chan}]
    (u/api-request request-map)))

(defn token [message]
  [message]
  (let [{:keys [username password resp-chan]} message
        request-map {:method :post
                     :url (str (state/get-base-api-url) "/tokens")
                     :body {:username username :password password}
                     :resp-chan resp-chan}]
    (u/api-request request-map)))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :AUTH/GET_TOKEN token
                      :AUTH/GET_CONFIG config
                      :AUTH/LOGIN login
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in Auth SDK module." (:type message)))))

(defn module-shutdown-handler [message]
  (log :info "Received shutdown message from Core - Authentication Module shutting down...."))

(defn init []
  (log :debug "Initializing SDK module: Auth")
  (let [module-inputs< (a/chan 1024)
        module-shutdown< (a/chan 1024)]
    (u/start-simple-consumer! module-inputs< module-router)
    (u/start-simple-consumer! module-shutdown< module-shutdown-handler)
    {:messages module-inputs<
     :shutdown module-shutdown<}))
