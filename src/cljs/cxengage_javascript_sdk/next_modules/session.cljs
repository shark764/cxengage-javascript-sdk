(ns cxengage-javascript-sdk.next-modules.session
  (:require-macros [cxengage-javascript-sdk.macros :refer [def-sdk-fn]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.specs :as specs]))

(s/def ::set-direction-spec
  (s/keys :req-un [::specs/direction]
          :opt-un [::specs/callback]))

(s/def ::go-ready-spec
  (s/keys :req-un [::specs/extension-value]
          :opt-un [::specs/callback]))

(s/def ::go-not-ready-spec
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(s/def ::set-active-tenant-spec
  (s/keys :req-un [::specs/tenant-id]
          :opt-un [::specs/callback]))

(def required-desktop-permissions
  #{"CONTACTS_CREATE"
    "CONTACTS_UPDATE"
    "CONTACTS_READ"
    "CONTACTS_ATTRIBUTES_READ"
    "CONTACTS_LAYOUTS_READ"
    "CONTACTS_ASSIGN_INTERACTION"
    "CONTACTS_INTERACTION_HISTORY_READ"
    "ARTIFACTS_CREATE_ALL"})

(def-sdk-fn go-not-ready
  ::go-not-ready-spec
  (p/get-topic :presence-state-change-request-acknowledged)
  [params]
  (let [{:keys [callback topic]} params
        session-id (state/get-session-id)
        resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        change-state-request {:method :post
                              :url (iu/api-url
                                    "tenants/tenant-id/presence/resource-id"
                                    {:tenant-id tenant-id
                                     :resource-id resource-id})
                              :body {:session-id session-id
                                     :state "notready"}}
        {:keys [status api-response]} (a/<! (iu/api-request change-state-request))
        new-state-data (:result api-response)]
    (if (= status 200)
      (p/publish {:topics topic
                  :response new-state-data
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-change-state-err)
                  :callback callback}))))

(defn- start-heartbeats []
  (log :info "Sending heartbeats...")
  (let [session-id (state/get-session-id)
        tenant-id (state/get-active-tenant-id)
        resource-id (state/get-active-user-id)
        heartbeat-request {:method :post
                           :body {:session-id session-id}
                           :url (iu/api-url
                                 "tenants/tenant-id/presence/resource-id/heartbeat"
                                 {:tenant-id tenant-id
                                  :resource-id resource-id})}
        topic (p/get-topic :presence-heartbeats-response)]
    (go-loop []
      (if (= "offline" (state/get-user-session-state))
        (do (log :info "Session is now offline; ceasing future heartbeats.")
            (state/set-session-expired! true)
            nil)
        (let [{:keys [api-response status]} (a/<! (iu/api-request heartbeat-request))
              {:keys [result]} api-response
              next-heartbeat-delay (* 1000 (or (:heartbeatDelay result) 30))]
          (if (= status 200)
            (do (log :debug "Heartbeat sent!")
                (p/publish {:topics topic
                            :response result})
                (a/<! (a/timeout next-heartbeat-delay))
                (recur))
            (do (log :fatal "Heartbeat failed; ceasing future heartbeats.")
                (state/set-session-expired! true)
                (p/publish {:topics topic
                            :error (e/session-heartbeats-failed-err)})
                nil)))))))

(defn- start-session []
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        start-session-request {:method :post
                               :url (iu/api-url
                                     "tenants/tenant-id/presence/resource-id/session"
                                     {:tenant-id tenant-id
                                      :resource-id resource-id})}]
    (go (let [start-session-response (a/<! (iu/api-request start-session-request))
              {:keys [status api-response]} start-session-response
              topic (p/get-topic :session-started)
              session-details (assoc (:result api-response) :resource-id (state/get-active-user-id))]
          (if (= status 200)
            (do (state/set-session-details! session-details)
                (p/publish {:topics topic
                            :response session-details})
                (go-not-ready)
                (state/set-session-expired! false)
                (start-heartbeats*))
            (p/publish {:topics topic
                        :error (e/failed-to-start-agent-session-err)}))
          nil))))

(defn- get-config []
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        config-request {:method :get
                        :url (iu/api-url
                              "tenants/tenant-id/users/resource-id/config"
                              {:tenant-id tenant-id
                               :resource-id resource-id})}
        topic (p/get-topic :config-response)]
    (go (let [config-response (a/<! (iu/api-request config-request))
              {:keys [api-response status]} config-response
              user-config (:result api-response)]
          (if (= status 200)
            (do (state/set-config! user-config)
                (ih/send-core-message {:type :config-ready})
                (p/publish {:topics topic
                            :response user-config})
                (p/publish {:topics (p/get-topic :extension-list)
                            :response (select-keys user-config [:active-extension :extensions])})
                (start-session*))
            (p/publish {:topics topic
                        :error (e/failed-to-get-session-config-err)}))))
    nil))

(def-sdk-fn set-active-tenant
  ::set-active-tenant-spec
  (p/get-topic :active-tenant-set)
  [params]
  (let [{:keys [callback topic tenant-id]} params
        tenant-permissions (state/get-tenant-permissions tenant-id)]
    (if-not (state/has-permissions? tenant-permissions required-desktop-permissions)
      (p/publish {:topics topic
                  :error (e/insufficient-permissions-err)
                  :callback callback})
      (do (state/set-active-tenant! tenant-id)
          (p/publish {:topics topic
                      :response {:tenant-id tenant-id}
                      :callback callback})
          (get-config*)))))

(def-sdk-fn set-direction
  ::set-direction-spec
  (p/get-topic :set-direction-response)
  [params]
  (let [{:keys [callback topic direction]} params
        tenant-id (state/get-active-tenant-id)
        resource-id (state/get-active-user-id)
        session-id (state/get-session-id)
        set-direction-request {:method :post
                               :url (iu/api-url
                                     "tenants/tenant-id/presence/resource-id/direction"
                                     {:tenant-id tenant-id
                                      :resource-id resource-id})
                               :body {:session-id session-id
                                      :direction direction
                                      :initiator-id resource-id}}
        {:keys [status api-response]} (a/<! (iu/api-request set-direction-request))
        direction-details {:direction direction
                           :session-id (get-in api-response [:result :session-id])}]
    (when (= status 200)
      (p/publish {:topics topic
                  :response direction-details
                  :callback callback}))))

(defn- go-ready [topic callback]
  (go (let [session-id (state/get-session-id)
            resource-id (state/get-active-user-id)
            tenant-id (state/get-active-tenant-id)
            change-state-request {:method :post
                                  :url (iu/api-url
                                        "tenants/tenant-id/presence/resource-id"
                                        {:tenant-id tenant-id
                                         :resource-id resource-id})
                                  :body {:session-id session-id
                                         :state "ready"}}
            {:keys [status api-response]} (a/<! (iu/api-request change-state-request))
            new-state-data (:result api-response)]
        (if (= status 200)
          (p/publish {:topics topic
                      :response new-state-data
                      :callback callback})
          (p/publish {:topics topic
                      :error (e/failed-to-change-state-err)
                      :callback callback})))))

(def-sdk-fn go-ready
  ::go-ready-spec
  (p/get-topic :presence-state-change-request-acknowledged)
  [params]
  (let [session-id (state/get-session-id)
        resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        {:keys [callback topic extension-value]} params
        config-request {:method :get
                        :url (iu/api-url
                              "tenants/tenant-id/users/resource-id/config"
                              {:tenant-id tenant-id
                               :resource-id resource-id})}
        {:keys [status api-response]} (a/<! (iu/api-request config-request))
        user-config (:result api-response)]
    (when (= status 200)
      (state/set-config! user-config)
      (let [new-extension (state/get-extension-by-value extension-value)
            active-extension (state/get-active-extension)]
        (if-not new-extension
          (p/publish {:topics topic
                      :error (e/invalid-extension-provided-err)
                      :callback callback})
          (if-not (= active-extension new-extension)

            ;; Active extension was either nil, or didn't match the one they passed.
            ;; Update their user prior to changing state, so they go ready with the
            ;; correct extension.
            (let [update-user-request {:method :put
                                       :url (iu/api-url
                                             "tenants/tenant-id/users/resource-id"
                                             {:tenant-id tenant-id
                                              :resource-id resource-id})
                                       :body {:activeExtension new-extension}}
                  {:keys [status api-response]} (a/<! (iu/api-request update-user-request))]
              (if (= status 200)
                (go-ready* topic callback)
                (p/publish {:topics topic
                            :error (e/failed-to-update-extension-err)
                            :callback callback})))

            ;;Their active extension and the extension they passed in are the same,
            ;;no user update is necessary, simply go ready.
            (go-ready* topic callback)))))))

(defrecord SessionModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (let [module-name :session]
      (ih/register {:api {module-name {:set-active-tenant set-active-tenant
                                       :go-ready go-ready
                                       :go-not-ready go-not-ready
                                       :set-direction set-direction}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
