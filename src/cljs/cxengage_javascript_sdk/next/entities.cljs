(ns cxengage-javascript-sdk.next.entities
  (:require-macros [lumbajack.macros :refer [log]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.next.protocols :as pr]
            [cxengage-javascript-sdk.next.errors :as e]
            [cxengage-javascript-sdk.next.pubsub :as p]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]))

(s/def ::get-all-entity-params
  (s/keys :req-un []
          :opt-un [:specs/callback]))

(s/def ::not-empty-string #(not= 0 (.-length %)))
(s/def ::entity-id (s/and string? ::not-empty-string))
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
         publish-fn (fn [r] (p/publish (str "entities/get-" (name entity-type) "-response") r callback))]
     (if (not (s/valid? validation params))
       (publish-fn (e/invalid-args-error (s/explain-data validation params)))
       (let [api-url (str api-url (get-in module-state [:urls entity-type]))
             entity-get-request {:method :get
                                 :url (iu/build-api-url-with-params
                                       api-url
                                       params)}]
         (go (let [entity-get-response (a/<! (iu/api-request entity-get-request))
                   {:keys [status api-response]} entity-get-response
                   {:keys [result]} api-response]
               (if (not= status 200)
                 (publish-fn (e/api-error api-response))
                 (publish-fn result))))
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
         publish-fn (fn [r] (p/publish (str "entities/put-" (name entity-type) "-response") r callback))]
     (if (not (s/valid? ::put-entity-params params))
       (publish-fn (e/invalid-args-error (s/explain-data ::put-entity-params params)))
       (let [api-url (str api-url (get-in module-state [:urls entity-type]))
             entity-get-request {:method :put
                                 :body body
                                 :url (iu/build-api-url-with-params
                                       api-url
                                       params)}]
         (go (let [entity-get-response (a/<! (iu/api-request entity-get-request))
                   {:keys [status api-response]} entity-get-response
                   {:keys [result]} api-response]
               (if (not= status 200)
                 (publish-fn (e/api-error api-response))
                 (publish-fn result))))
        nil)))))

(def initial-state
  {:module-name :entities
   :urls {:user "tenants/tenant-id/users/entity-id"
          :users "tenants/tenant-id/users"}})

(defrecord EntitiesModule [config state]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")]
      (register {:api {:get-users (partial get-entity this :users ::get-all-entity-params)
                       :get-user (partial get-entity this :user ::get-single-entity-params)
                       :update-user (partial put-entity this :user)}
                 :module-name (get @(:state this) :module-name)})))
  (stop [this]))
