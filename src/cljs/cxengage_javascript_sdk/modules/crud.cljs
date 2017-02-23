(ns cxengage-javascript-sdk.modules.crud
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-cljs-utils.core :as u]))

(def module-state (atom {}))

(def valid-entities {:users "/tenants/%s/users"})

(defn get-entity
  [result-chan message]
  (let [{:keys [token resp-chan tenant-id entity entity-id]} message
        request-map {:method :get
                     :url (str (state/get-base-api-url) "/tenants/" tenant-id "/" entity "/" entity-id)
                     :token token
                     :resp-chan resp-chan}]
    (u/api-request request-map)
    (go (let [result (a/<! result-chan)]
          (a/put! resp-chan result)))))

(defn get-entities
  [result-chan message]
  (let [{:keys [token resp-chan tenant-id entity]} message
        request-map {:method :get
                     :url (str (state/get-base-api-url) "/tenants/" tenant-id "/" entity)
                     :token token
                     :resp-chan resp-chan}]
    (u/api-request request-map)
    (go (let [result (a/<! result-chan)]
          (a/put! resp-chan result)))))

(defn set-active-extension [message]
  (let [{:keys [resp-chan token tenant-id resource-id extension]} message
        extension-body {:activeExtension extension}
        request-map {:method :put
                     :body extension-body
                     :url (str (state/get-base-api-url) "/tenants/" tenant-id "/users/" resource-id)
                     :token token
                     :resp-chan resp-chan}]
    (u/api-request request-map)))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :CRUD/GET_ENTITY (partial get-entity (a/promise-chan))
                      :CRUD/GET_ENTITIES (partial get-entities (a/promise-chan))
                      :CRUD/SET_ACTIVE_EXTENSION set-active-extension
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in CRUD SDK module." (:type message)))))

(defn module-shutdown-handler [message]
  (log :info "Received shutdown message from Core - CRUD Module shutting down...."))

(defn init []
  (log :debug "Initializing SDK module: CRUD")
  (let [module-inputs< (a/chan 1024)
        module-shutdown< (a/chan 1024)]
    (u/start-simple-consumer! module-inputs< module-router)
    (u/start-simple-consumer! module-shutdown< module-shutdown-handler)
    {:messages module-inputs<
     :shutdown module-shutdown<}))
