(ns client-sdk.api.crud
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [client-sdk.state :as state]
            [lumbajack.core :refer [log]]))

(defn get-entity-handler
  [module-chan result-chan params callback]
  (let [{:keys [entity entityId]} (js->clj params :keywordize-keys true)
        entitiy-msg {:resp-chan result-chan
                       :type :CRUD/GET_ENTITY
                       :token (state/get-token)
                       :tenant-id (state/get-active-tenant-id)
                       :entity entity
                       :entity-id entityId}]
       (a/put! module-chan entitiy-msg)
       (go (let [{:keys [results]} (a/<! result-chan)]
            (callback (clj->js results))))))

(defn get-entities-handler
  [module-chan result-chan params callback]
  (let [{:keys [entity]} (js->clj params :keywordize-keys true)
        entity-msg {:resp-chan result-chan
                       :type :CRUD/GET_ENTITIES
                       :token (state/get-token)
                       :tenant-id (state/get-active-tenant-id)
                       :entity entity}]
       (a/put! module-chan entity-msg)
       (go (callback (clj->js (a/<! result-chan))))))

(defn api []
  (let [module-chan (state/get-module-chan :crud)]
    {:getEntity (partial get-entity-handler module-chan (a/promise-chan))
     :getEntities (partial get-entities-handler module-chan (a/promise-chan))}))
