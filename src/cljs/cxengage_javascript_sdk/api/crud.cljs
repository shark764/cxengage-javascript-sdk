(ns cxengage-javascript-sdk.api.crud
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.module-gateway :as mg]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.pubsub :refer [sdk-response sdk-error-response]]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.errors :as err]))

(s/def ::get-entity-params
  (s/keys :req-un []
          :opt-un []))

(defn get-entity
  ([entity-type params callback]
   (get-entity entity-type (merge (iu/extract-params params) {:callback callback})))
  ([entity-type params]
   (let [params (iu/extract-params params)
         {:keys [entityId callback]} params
         pubsub-topic (str "cxengage/crud/get-" (.slice entity-type 0 -1) "-response")]
     (if-not (s/valid? ::get-entity-params params)
       (sdk-error-response pubsub-topic (err/invalid-params-err) callback)
       (let [get-entity-msg (iu/base-module-request
                                        :CRUD/GET_ENTITY
                                        {:tenant-id (state/get-active-tenant-id)
                                         :entity entity-type
                                         :entity-id entityId})]
         (go (let [get-entity-response (a/<! (mg/send-module-message get-entity-msg))]
               (sdk-response pubsub-topic get-entity-response callback))))))))

(s/def ::get-entities-params
  (s/keys :req-un []
          :opt-un []))

(defn get-entities
   ([entity-type params callback]
    (get-entities entity-type (merge (iu/extract-params params) {:callback callback})))
   ([entity-type params]
    (let [params (iu/extract-params params)
          {:keys [callback]} params
          pubsub-topic (str "cxengage/crud/get-" entity-type "-response")]
      (if-not (s/valid? ::get-entities-params params)
        (sdk-error-response pubsub-topic (err/invalid-params-err) callback)
        (let [get-entities-msg (iu/base-module-request
                                         :CRUD/GET_ENTITIES
                                         {:tenant-id (state/get-active-tenant-id)
                                          :entity entity-type})]
          (go (let [get-entities-response (a/<! (mg/send-module-message get-entities-msg))]
                (sdk-response pubsub-topic get-entities-response callback))))))))
