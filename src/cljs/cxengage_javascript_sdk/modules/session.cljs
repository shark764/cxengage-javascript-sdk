(ns cxengage-javascript-sdk.modules.session
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]))

(defn start-heartbeats*
  [module]
  (log :info "Sending heartbeats...")
  (let [session-id (state/get-session-id)
        tenant-id (state/get-active-tenant-id)
        resource-id (state/get-active-user-id)
        module-state @(:state module)
        api-url (get-in module [:config :api-url])
        heartbeat-url (str api-url (get-in module-state [:urls :heartbeat]))
        heartbeat-request {:method :post
                           :body {:session-id session-id}
                           :url (iu/build-api-url-with-params
                                 heartbeat-url
                                 {:tenant-id tenant-id
                                  :resource-id resource-id})}
        topic (p/get-topic :presence-heartbeats-response)]
    (go-loop []
      (if (= "offline" (state/get-user-session-state))
        (do (log :info "Session is now offline; ceasing future heartbeats.")
            nil)
        (let [{:keys [api-response status]} (a/<! (iu/api-request heartbeat-request))
              {:keys [result]} api-response
              next-heartbeat-delay (* 1000 (or (:heartbeatDelay api-response) 30))]
          (if (not= status 200)
            (do (log :error "Heartbeat failed; ceasing future heartbeats.")
                (state/set-session-expired! true)
                (p/publish {:topics topic
                            :error (e/api-error "no more heartbeats")})
                nil)
            (do (log :debug "Heartbeat sent!")
                (p/publish {:topics topic
                            :response result})
                (a/<! (a/timeout next-heartbeat-delay))
                (recur))))))))

(s/def ::state #{"ready" "notready" "offline"})

(s/def ::go-ready-params
  (s/keys :req-un [::specs/extension-value]
          :opt-un [::specs/callback]))

(s/def ::reason-info
  (s/keys :req-un [::specs/reason ::specs/reason-id ::specs/reason-list-id]
          :opt-un []))

(s/def ::go-not-ready-params
  (s/keys :req-un []
          :opt-un [::reason-info ::specs/callback]))

(s/def ::go-offline-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(defn go-not-ready
  ([module] (go-not-ready module {}))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (go-not-ready module (merge (iu/extract-params params)
                                 {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         api-url (get-in module [:config :api-url])
         module-state @(:state module)
         session-id (state/get-session-id)
         resource-id (state/get-active-user-id)
         tenant-id (state/get-active-tenant-id)
         {:keys [reason-info callback]} params
         {:keys [reason-id reason-list-id reason]} reason-info
         topic (p/get-topic :presence-state-change-request-acknowledged)
         state-lists (state/get-all-reason-codes-by-list reason-list-id)
         valid-reasons? (cond
                          (empty? state-lists) false
                          (empty? (filterv #(= (:reason-id reason-id)) state-lists)) false
                          :else true)]
     (if-not (s/valid? ::go-not-ready-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error "Invalid args. All three reason code parameters must be supplied.")
                   :callback (if (fn? callback) callback nil)})
       (if (and (or reason-id reason-list-id reason) (not (state/valid-reason-codes? reason reason-id reason-list-id)))
         (p/publish {:topics topic
                     :error (e/invalid-args-error "Reason, Reason-id, and Reason-list-id must be correct values.")})
         (let [change-state-url (str api-url (get-in module-state [:urls :change-state]))
               body {:session-id session-id
                     :state "notready"}
               change-state-request {:method :post
                                     :url (iu/build-api-url-with-params
                                           change-state-url
                                           {:tenant-id tenant-id
                                            :resource-id resource-id})
                                     :body (cond-> body
                                             reason-info (assoc :reason reason :reason-id reason-id :reason-list-id reason-list-id))}]

           (go
             (let [change-state-response (a/<! (iu/api-request change-state-request))
                   {:keys [api-response status]} change-state-response
                   {:keys [result]} api-response]
               (if (not= status 200)
                 (p/publish {:topics topic
                             :error (e/api-error api-response)
                             :callback callback})
                 (p/publish {:topics topic
                             :response result
                             :callback callback}))))
           nil))))))

(defn go-offline
  ([module] (go-offline module {}))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (go-offline module (merge (iu/extract-params params)
                               {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         api-url (get-in module [:config :api-url])
         module-state @(:state module)
         session-id (state/get-session-id)
         resource-id (state/get-active-user-id)
         tenant-id (state/get-active-tenant-id)
         {:keys [callback]} params
         topic (p/get-topic :presence-state-change-request-acknowledged)]
     (if (not (s/valid? ::go-offline-params params))
       (p/publish {:topics topic
                   :error (e/invalid-args-error "Invalid args.")
                   :callback (if (fn? callback) callback nil)})
       (let [change-state-url (str api-url (get-in module-state [:urls :change-state]))
             change-state-request {:method :post
                                   :url (iu/build-api-url-with-params
                                         change-state-url
                                         {:tenant-id tenant-id
                                          :resource-id resource-id})
                                   :body {:session-id session-id
                                          :state "offline"}}]
         (go
           (let [change-state-response (a/<! (iu/api-request change-state-request))
                 {:keys [api-response status]} change-state-response
                 {:keys [result]} api-response]
             (state/set-session-expired! true)
             (if (not= status 200)
               (p/publish {:topics topic
                           :error (e/api-error api-response)
                           :callback callback})
               (p/publish {:topics topic
                           :response result
                           :callback callback}))))
         nil)))))

(defn go-ready
  ([module]
   (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (first others))
     (e/wrong-number-of-args-error)
     (go-ready module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         api-url (get-in module [:config :api-url])
         module-state @(:state module)
         session-id (state/get-session-id)
         resource-id (state/get-active-user-id)
         tenant-id (state/get-active-tenant-id)
         {:keys [extension-value callback]} params
         active-extension-value (state/get-active-extension)
         topic (p/get-topic :presence-state-change-request-acknowledged)]
     (if-not (s/valid? ::go-ready-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error "Invalid args.")
                   :callback (if (fn? callback) callback nil)})
       (let [change-state-url (str api-url (get-in module-state [:urls :change-state]))
             change-state-request {:method :post
                                   :url (iu/build-api-url-with-params
                                         change-state-url
                                         {:tenant-id tenant-id
                                          :resource-id resource-id})
                                   :body {:session-id session-id
                                          :state "ready"}}
             config-url (str api-url (get-in module-state [:urls :config]))
             config-request {:method :get
                             :url (iu/build-api-url-with-params
                                   config-url
                                   {:tenant-id tenant-id
                                    :resource-id resource-id})}]
         (go (let [config-response (a/<! (iu/api-request config-request))
                   {:keys [api-response status]} config-response
                   {:keys [result]} api-response]
               (if (not= status 200)
                 (p/publish {:topics topic
                             :error (e/api-error api-response)
                             :callback callback})
                 (do (state/set-config! result)
                     (let [update-user-url (str api-url (get-in module-state [:urls :update-user]))
                           new-extension (state/get-extension-by-value extension-value)
                           extensions (state/get-all-extensions)
                           user-update-request {:method :put
                                                :url (iu/build-api-url-with-params
                                                      update-user-url
                                                      {:tenant-id tenant-id
                                                       :resource-id resource-id})
                                                :body {:activeExtension new-extension}}]
                       (if (and extension-value
                                (not= active-extension-value extension-value))
                         (if (nil? new-extension)
                           (p/publish {:topics topic
                                       :error (e/not-a-valid-extension)
                                       :callback callback})
                           (do (go (let [user-update-response (a/<! (iu/api-request user-update-request))
                                         {:keys [api-response status]} user-update-response]
                                     (if (not= status 200)
                                       (p/publish {:topics topic
                                                   :error (e/api-error "failed to set user extension")
                                                   :callback callback})
                                       (let [change-state-response (a/<! (iu/api-request change-state-request))
                                             {:keys [api-response status]} change-state-response
                                             {:keys [result]} api-response]
                                         (if (not= status 200)
                                           (p/publish {:topics topic
                                                       :error (e/api-error api-response)
                                                       :callback callback})
                                           (p/publish {:topics topic
                                                       :response result
                                                       :callback callback}))))))
                               nil))
                         (do (go (let [change-state-response (a/<! (iu/api-request change-state-request))
                                       {:keys [api-response status]} change-state-response
                                       {:keys [result]} api-response]
                                   (if (not= status 200)
                                     (p/publish {:topics topic
                                                 :error (e/api-error api-response)
                                                 :callback callback})
                                     (p/publish {:topics topic
                                                 :response result
                                                 :callback callback}))))
                             nil))))))))))))

(defn start-session* [module]
  (let [api-url (get-in module [:config :api-url])
        module-state @(:state module)
        resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        topic (p/get-topic :session-started)
        start-session-url (str api-url (get-in module-state [:urls :start-session]))
        start-session-request {:method :post
                               :url (iu/build-api-url-with-params
                                     start-session-url
                                     {:tenant-id tenant-id
                                      :resource-id resource-id})}]
    (go (let [start-session-response (a/<! (iu/api-request start-session-request))
              {:keys [status api-response]} start-session-response
              {:keys [result]} api-response
              pubsub-response (assoc result :resource-id (state/get-active-user-id))]
          (if (not= status 200)
            (p/publish {:topics topic
                        :error (e/api-error api-response)})
            (do (state/set-session-details! result)
                (p/publish {:topics topic
                            :response pubsub-response})
                (go-not-ready module)
                (state/set-session-expired! false)
                (start-heartbeats* module)))))
    nil))

(defn get-config* [module]
  (let [api-url (get-in module [:config :api-url])
        module-state @(:state module)
        resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        config-url (str api-url (get-in module-state [:urls :config]))
        config-request {:method :get
                        :url (iu/build-api-url-with-params
                              config-url
                              {:tenant-id tenant-id
                               :resource-id resource-id})}
        topic (p/get-topic :config-response)]
    (go (let [config-response (a/<! (iu/api-request config-request))
              {:keys [api-response status]} config-response
              {:keys [result]} api-response]
          (if (not= status 200)
            (p/publish {:topics topic
                        :error (e/api-error api-response)})
            (do (state/set-config! result)
                (a/put! (:core-messages< module) :config-ready)
                (p/publish {:topics topic
                            :response result})
                (p/publish {:topics (p/get-topic :extension-list)
                            :response (select-keys result [:active-extension :extensions])})
                (start-session* module)))))
    nil))

(s/def ::tenant-id string?)
(s/def ::set-active-tenant-params
  (s/keys :req-un [::tenant-id]
          :opt-un [:specs/callback]))

(def required-desktop-permissions
  #{"CONTACTS_CREATE"
    "CONTACTS_UPDATE"
    "CONTACTS_READ"
    "CONTACTS_ATTRIBUTES_READ"
    "CONTACTS_LAYOUTS_READ"
    "CONTACTS_ASSIGN_INTERACTION"
    "CONTACTS_INTERACTION_HISTORY_READ"
    "ARTIFACTS_CREATE_ALL"})

(s/def ::set-direction-params
  (s/keys :req-un [::specs/direction]
          :opt-un [::specs/callback]))

(defn set-direction
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (set-direction module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         {:keys [direction callback]} params
         topic (p/get-topic :set-direction-response)
         api-url (get-in module [:config :api-url])
         tenant-id (state/get-active-tenant-id)
         resource-id (state/get-active-user-id)
         session-id (state/get-session-id)
         direction-url (str api-url "tenants/tenant-id/presence/resource-id/direction")
         direction-body {:session-id session-id
                         :direction direction
                         :initiator-id resource-id}
         direction-request {:method :post
                            :url (iu/build-api-url-with-params
                                  direction-url
                                  {:tenant-id tenant-id
                                   :resource-id resource-id})
                            :body direction-body}]
     (if-not (s/valid? ::set-direction-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::set-direction-params params))
                   :callback callback})
       (do (go (let [set-direction-response (a/<! (iu/api-request direction-request))
                     {:keys [status api-response]} set-direction-response
                     {:keys [result]} api-response]
                 (if (not= status 200)
                   (p/publish {:topics topic
                               :error (e/api-error "api error")
                               :callback callback})
                   (p/publish {:topics topic
                               :response (assoc (select-keys result [:session-id]) :direction direction)
                               :callback callback}))))
           nil)))))

(defn set-active-tenant
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (set-active-tenant module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         module-state @(:state module)
         {:keys [tenant-id callback]} params
         tenant-permissions (state/get-tenant-permissions tenant-id)
         topic (p/get-topic :active-tenant-set)]
     (if-let [error (cond
                      (not (s/valid? ::set-active-tenant-params params))
                      (e/invalid-args-error (s/explain-data ::set-active-tenant-params params))

                      (not (state/has-permissions? tenant-permissions required-desktop-permissions))
                      (e/missing-required-permissions-error)

                      :else false)]
       (p/publish {:topics topic
                   :error error
                   :callback callback})
       (do (state/set-active-tenant! tenant-id)
           (p/publish {:topics topic
                       :response {:tenant-id tenant-id}
                       :callback callback})
           (get-config* module)
           nil)))))

(def initial-state
  {:module-name :session
   :topics ["active-tenant-set" "start-session"]
   :urls {:config "tenants/tenant-id/users/resource-id/config"
          :start-session "tenants/tenant-id/presence/resource-id/session"
          :change-state "tenants/tenant-id/presence/resource-id"
          :update-user "tenants/tenant-id/users/resource-id"
          :heartbeat "tenants/tenant-id/presence/resource-id/heartbeat"}})

(defrecord SessionModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [module-name (get @(:state this) :module-name)]
      (ih/register {:api {module-name {:set-active-tenant (partial set-active-tenant this)
                                    :go-ready (partial go-ready this)
                                    :go-not-ready (partial go-not-ready this)
                                    :end (partial go-offline this)
                                    :set-direction (partial set-direction this)}}
                 :module-name module-name})
      (a/put! core-messages< {:module-registration-status :success :module module-name})
      (log :info (str "<----- Started " (name module-name) " SDK module! ----->"))))
  (stop [this])
  (refresh-integration [this]))
