(ns cxengage-javascript-sdk.modules.crud
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cxengage-cljs-utils.core :as u]))

(def module-state (atom {}))

(def valid-entities {:users "/tenants/%s/users"})

(defn get-entity
  [result-chan message]
  (let [{:keys [token resp-chan tenant-id entity entity-id]} message
        request-map {:method :get
                     :url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/" entity "/" entity-id))
                     :token token
                     :resp-chan resp-chan}]
    (u/api-request request-map)
    (go (let [result (a/<! result-chan)]
          (a/put! resp-chan result)))))

(defn get-entities
  [result-chan message]
  (let [{:keys [token resp-chan tenant-id entity]} message
        request-map {:method :get
                     :url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/" entity))
                     :token token
                     :resp-chan resp-chan}]
    (u/api-request request-map)
    (go (let [result (a/<! result-chan)]
          (a/put! resp-chan result)))))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :CRUD/GET_ENTITY (partial get-entity (a/promise-chan))
                      :CRUD/GET_ENTITIES (partial get-entities (a/promise-chan))
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in CRUD SDK module." (:type message)))))

(defn module-shutdown-handler [message]
  (log :info "Received shutdown message from Core - CRUD Module shutting down...."))

(defn init [env]
  (log :debug "Initializing SDK module: CRUD")
  (swap! module-state assoc :env env)
  (let [module-inputs< (a/chan 1024)
        module-shutdown< (a/chan 1024)]
    (u/start-simple-consumer! module-inputs< module-router)
    (u/start-simple-consumer! module-shutdown< module-shutdown-handler)
    {:messages module-inputs<
     :shutdown module-shutdown<}))
