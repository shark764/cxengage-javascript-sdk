(ns cxengage-javascript-sdk.modules.entities
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]))

(s/def ::get-all-entity-params
  (s/keys :req-un []
          :opt-un [:specs/callback]))

(s/def ::entity-sub-id (s/and string? ::not-empty-string))
(s/def ::not-empty-string #(not= 0 (.-length %)))
(s/def ::entity-id (s/and string? ::not-empty-string))
(s/def ::get-sub-entity-params
  (s/keys :req-un [::entity-id ::entity-sub-id]
          :opt-un [:specs/callback]))

(s/def ::get-single-entity-params
  (s/keys :req-un [::entity-id]
          :opt-un [:specs/callback]))

(defn get-entity
  ([module entity-type validation] (get-entity module entity-type validation {}))
  ([module entity-type validation params & others]
   (if-not (fn? (first others))
     (e/wrong-number-of-args-error)
     (get-entity module entity-type validation (merge (iu/extract-params params) {:callback (first others)}))))
  ([module entity-type validation params]
   (let [params (iu/extract-params params)
         module-state @(:state module)
         api-url (get-in module [:config :api-url])
         {:keys [callback]} params
         session-id (st/get-session-id)
         resource-id (st/get-active-user-id)
         tenant-id (st/get-active-tenant-id)
         params (merge params {:tenant-id tenant-id
                               :resource-id resource-id
                               :session-id session-id})
         topic (p/get-topic (keyword (str "get-" (name entity-type) "-response")))]
     (if (not (s/valid? validation params))
       (p/publish {:topics topic
                   :error (e/invalid-args-error "Arguments are invalid")
                   :callback callback})
       (let [api-url (str api-url (get-in module-state [:urls entity-type]))
             entity-get-request {:method :get
                                 :url (iu/build-api-url-with-params
                                       api-url
                                       params)}]
         (go (let [entity-get-response (a/<! (iu/api-request entity-get-request))
                   {:keys [status api-response]} entity-get-response]
               (if (not= status 200)
                 (p/publish {:topics topic
                             :error (e/api-error api-response)
                             :callback callback})
                 (p/publish {:topics topic
                             :response api-response
                             :callback callback}))))
         nil)))))

(s/def ::put-entity-params
  (s/keys :req-un [::entity-id]
          :opt-un [:specs/callback]))

(defn put-entity
  ([module entity-type] (get-entity module entity-type {}))
  ([module entity-type params & others]
   (if-not (fn? (first others))
     (e/wrong-number-of-args-error)
     (get-entity module entity-type (merge (iu/extract-params params) {:callback (first others)}))))
  ([module entity-type params]
   (let [params (iu/extract-params params)
         module-state @(:state module)
         api-url (get-in module [:config :api-url])
         {:keys [body callback]} params
         session-id (st/get-session-id)
         resource-id (st/get-active-user-id)
         tenant-id (st/get-active-tenant-id)
         params (merge params {:tenant-id tenant-id
                               :resource-id resource-id
                               :session-id session-id})
         topic (p/get-topic (keyword (str "update-" (name entity-type) "-response")))]
     (if (not (s/valid? ::put-entity-params params))
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::put-entity-params params))
                   :callback callback})
       (let [api-url (str api-url (get-in module-state [:urls entity-type]))
             entity-get-request {:method :put
                                 :body body
                                 :url (iu/build-api-url-with-params
                                       api-url
                                       params)}]
         (go (let [entity-get-response (a/<! (iu/api-request entity-get-request))
                   {:keys [status api-response]} entity-get-response]
               (if (not= status 200)
                 (p/publish {:topics topic
                             :error (e/api-error api-response)
                             :callback callback})
                 (p/publish {:topics topic
                             :response api-response
                             :callback callback}))))
         nil)))))

(def initial-state
  {:module-name :entities
   :urls {:user "tenants/tenant-id/users/entity-id"
          :users "tenants/tenant-id/users"
          :queue "tenants/tenant-id/queues/entity-id"
          :queues "tenants/tenant-id/queues"
          :transfer-list "tenants/tenant-id/transfer-lists/entity-id"
          :transfer-lists "tenants/tenant-id/transfer-lists"
          :capacity "tenants/tenant-id/users/entity-id/realtime-statistics/capacity"
          :available-stats "tenants/tenant-id/realtime-statistics/available?client=toolbar"
          :contact-history "tenants/tenant-id/contacts/entity-id/interactions"
          :contact-interaction "tenants/tenant-id/interactions/entity-id"}})

(defrecord EntitiesModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)]
      (register {:api {module-name {:get {:users (partial get-entity this :users ::get-all-entity-params)
                                          :user (partial get-entity this :user ::get-single-entity-params)
                                          :queues (partial get-entity this :queues ::get-all-entity-params)
                                          :queue (partial get-entity this :queue ::get-single-entity-params)
                                          :notes (partial get-entity this :interactions ::get-all-entity-params)
                                          :note (partial get-entity this :interaction ::get-single-entity-params)
                                          :transfer-lists (partial get-entity this :transfer-lists ::get-all-entity-params)
                                          :transfer-list (partial get-entity this :transfer-list ::get-single-entity-params)}
                                    :update {:user (partial put-entity this :user)}}
                       :reporting  {:get-capacity (partial get-entity this :capacity ::get-single-entity-params)
                                    :get-available-stats (partial get-entity this :available-stats ::get-all-entity-params)
                                    :get-contact-history (partial get-entity this :contact-history ::get-single-entity-params)
                                    :get-contact-interaction (partial get-entity this :contact-interaction ::get-single-entity-params)}}
                 :module-name module-name})
      (a/put! core-messages< {:module-registration-status :success :module module-name})
      (log :info (str "<----- Started " (name module-name) " SDK module! ----->"))))
  (stop [this]))
