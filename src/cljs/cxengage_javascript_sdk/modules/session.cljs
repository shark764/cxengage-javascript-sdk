(ns cxengage-javascript-sdk.modules.session
  (:require-macros [lumbajack.macros :refer [log]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]))

(defn start-heartbeats*
  [module]
  (js/console.info "Sending heartbeats...")
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
        (do (js/console.info "Session is now offline; ceasing future heartbeats.")
            nil)
        (let [{:keys [api-response status]} (a/<! (iu/api-request heartbeat-request))
              {:keys [result]} api-response
              next-heartbeat-delay (* 1000 (or (:heartbeatDelay api-response) 30))]
          (if (not= status 200)
            (do (js/console.error "Heartbeat failed; ceasing future heartbeats.")
                (p/publish {:topics topic
                            :error (e/api-error "no more heartbeats")})
                nil)
            (do (js/console.info "Heartbeat sent!")
                (p/publish {:topics topic
                            :response result})
                (a/<! (a/timeout next-heartbeat-delay))
                (recur))))))))

(s/def ::state #{"ready" "notready" "offline"})

(s/def ::go-ready-params
  (s/keys :req-un [::specs/extension-value ::state]
          :opt-un [::specs/callback]))

(s/def ::go-not-ready-params
  (s/keys :req-un [::state]
          :opt-un [::specs/callback]))

(s/def ::go-offline-params
  (s/keys :req-un [::state]
          :opt-un [::specs/callback]))

(defn change-presence-state-impl* [request topic callback]
  (go
    (let [change-state-response (a/<! (iu/api-request request))
          {:keys [api-response status]} change-state-response
          {:keys [result]} api-response]
      (if (not= status 200)
        (p/publish {:topics topic
                    :error (e/api-error api-response)
                    :callback callback})
        (p/publish {:topics topic
                    :response result
                    :callback callback})))))

(defn change-presence-state
  ([module state] (change-presence-state module state {}))
  ([module state params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (change-presence-state module state (merge (iu/extract-params params)
                                                {:callback (first others)}))))
  ([module state params]
   (let [params (assoc (iu/extract-params params) :state state)
         api-url (get-in module [:config :api-url])
         module-state @(:state module)
         session-id (state/get-session-id)
         resource-id (state/get-active-user-id)
         tenant-id (state/get-active-tenant-id)
         {:keys [extension-value callback]} params
         active-extension-value (state/get-active-extension)
         extensions (state/get-all-extensions)
         topic (p/get-topic :presence-state-change-request-acknowledged)
         new-extension (state/get-extension-by-value extension-value)
         validation-spec (cond
                           (= state "ready") ::go-ready-params
                           (= state "notready") ::go-not-ready-params
                           (= state "offline") ::go-offline-params
                           :else ::go-ready-params)]
     (if (not (s/valid? validation-spec params))
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data validation-spec params))
                   :callback callback})
       (let [change-state-url (str api-url (get-in module-state [:urls :change-state]))
             change-state-request {:method :post
                                   :url (iu/build-api-url-with-params
                                         change-state-url
                                         {:tenant-id tenant-id
                                          :resource-id resource-id})
                                   :body {:session-id session-id
                                          :state state}}]
         (if (and (= state "ready")
                  extension-value
                  (not= active-extension-value extension-value))
           (let [update-user-url (str api-url (get-in module-state [:urls :update-user]))
                 user-update-request {:method :put
                                      :url (iu/build-api-url-with-params
                                            update-user-url
                                            {:tenant-id tenant-id
                                             :resource-id resource-id})
                                      :body {:activeExtension new-extension}}]
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
                           (change-presence-state-impl* change-state-request topic callback))))
                   nil)))
           (do (change-presence-state-impl* change-state-request topic callback)
               nil)))))))

(defn start-session* [module]
  (let [api-url (get-in module [:config :api-url])
        module-state @(:state module)
        resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        topic (p/get-topic :session-start-response)
        start-session-url (str api-url (get-in module-state [:urls :start-session]))
        start-session-request {:method :post
                               :url (iu/build-api-url-with-params
                                     start-session-url
                                     {:tenant-id tenant-id
                                      :resource-id resource-id})}]
    (go (let [start-session-response (a/<! (iu/api-request start-session-request))
              {:keys [status api-response]} start-session-response
              {:keys [result]} api-response]
          (if (not= status 200)
            (p/publish {:topics topic
                        :error (e/api-error api-response)})
            (do (state/set-session-details! result)
                (p/publish {:topics topic
                            :response result})
                (change-presence-state module "notready" {})
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
    "CONTACTS_INTERACTION_HISTORY_READ"})

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
         topic (p/get-topic :asdf)
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
                   :error (e/invalid-args-error (s/explain-data ::dial-params params))
                   :callback callback})
       (do (go (let [set-direction-response (a/<! (iu/api-request direction-request))
                     {:keys [status api-response]} set-direction-response
                     {:keys [result]} api-response]
                 (if (not= status 200)
                   (p/publish {:topics topic
                               :error (e/api-error "api error")
                               :callback callback})
                   (p/publish {:topics topic
                               :response (select-keys result [:session-id])
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
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)]
      (register {:api {module-name {:set-active-tenant (partial set-active-tenant this)
                                    :go-ready (partial change-presence-state this "ready")
                                    :go-not-ready (partial change-presence-state this "notready")
                                    :go-offline (partial change-presence-state this "offline")
                                    :set-direction (partial set-direction this)}}
                 :module-name module-name})
      (a/put! core-messages< {:module-registration-status :success :module module-name})
      (js/console.info "<----- Started " (name module-name) " module! ----->")))
  (stop [this]))
