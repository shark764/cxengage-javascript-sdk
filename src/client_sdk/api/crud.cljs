(ns client-sdk.api.crud
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [client-sdk.state :as state]))

(defn get-entity
  [params]
  (let [module-chan (state/get-module-chan :crud)
        response-chan (a/promise-chan)
        {:keys [entity entityId callback]} (js->clj params :keywordize-keys true)
        entitiy-msg {:resp-chan response-chan
                     :type :CRUD/GET_ENTITY
                     :token (state/get-token)
                     :tenant-id (state/get-active-tenant-id)
                     :entity entity
                     :entity-id entityId}]
    (a/put! module-chan entitiy-msg)
    (go (let [{:keys [results]} (a/<! response-chan)]
          (callback (clj->js results))))))

(defn get-entities
  [params]
  (let [module-chan (state/get-module-chan :crud)
        response-chan (a/promise-chan)
        {:keys [entity callbackx]} (js->clj params :keywordize-keys true)
        entity-msg {:resp-chan response-chan
                    :type :CRUD/GET_ENTITIES
                    :token (state/get-token)
                    :tenant-id (state/get-active-tenant-id)
                    :entity entity}]
    (a/put! module-chan entity-msg)
    (go (callback (clj->js (a/<! response-chan))))))
