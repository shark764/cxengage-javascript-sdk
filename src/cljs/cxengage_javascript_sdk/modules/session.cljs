(ns cxengage-javascript-sdk.modules.session
  (:require-macros [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.spec.alpha :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.specs :as specs]))

(s/def ::go-not-ready-spec
  (s/keys :req-un []
          :opt-un [::specs/callback ::specs/reason-info]))

(def-sdk-fn go-not-ready
  "The goNotReady function is used to set an agent's state to one that is unable
  to receive new work offers. This function can be used one of two ways.

  The first way is to call the function without any parameters. This will set an
  agent to the default not ready state, without any additional context provided.

  ``` javascript
  CxEngage.session.goNotReady();
  ```

  The second way to call this function is to pass in a set of data containing
  three properties: reason, reasonId, and reasonListId. These are used to provide
  context to the state change.

  ``` javascript
  CxEngage.session.goNotReady({
    reasonInfo: {
       reason: '{{string}}',
       reasonId: '{{uuid}}',
       reasonListId: '{{uuid}}'
    }
  });
  ```

  Possible Errors:

  - [Session: 2004](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-change-state-err)
  - [Session: 2007](/cxengage-javascript-sdk.domain.errors.html#var-invalid-reason-info-err)
  "
  {:validation ::go-not-ready-spec
   :topic-key :presence-state-change-request-acknowledged}
  [params]
  (let [{:keys [callback topic reason-info]} params
        {:keys [reason reason-id reason-list-id]} reason-info
        session-id (state/get-session-id)]
    (if (and
         (or reason reason-id reason-list-id)
         (not (state/valid-reason-codes? reason reason-id reason-list-id)))
      (p/publish {:topics topic
                  :error (e/invalid-reason-info-err (state/get-all-reason-lists))
                  :callback callback})
      (let [change-state-body (cond-> {:session-id session-id
                                       :state "notready"}
                                reason-info (assoc :reason reason
                                                   :reason-id reason-id
                                                   :reason-list-id reason-list-id))
            resp (a/<! (rest/change-state-request change-state-body nil))
            {:keys [status api-response]} resp
            new-state-data (:result api-response)]
        (if (= status 200)
          (p/publish {:topics topic
                      :response new-state-data
                      :callback callback})
          (p/publish {:topics topic
                      :error (e/failed-to-change-state-err resp)
                      :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.session.setActiveTenant({ tenantId: "{{uuid}}" });
;; -------------------------------------------------------------------------- ;;

(defn- start-heartbeats* []
  (log :debug "Sending heartbeats...")
  (go-loop []
    (if (= "offline" (state/get-user-session-state))
      (do (log :info "Session is now offline; ceasing future heartbeats.")
          (state/set-session-expired! true)
          (state/set-user-session-state! {:state nil})
          nil)
      (let [topic (topics/get-topic :presence-heartbeats-response)
            resp (a/<! (rest/heartbeat-request))
            {:keys [api-response status]} resp
            {:keys [result]} api-response
            next-heartbeat-delay (* 1000 (or (:heartbeat-delay result)
                                             (do (log :warn "There was no heartbeat delay on the result of the request, delaying the next heartbeat with the default value")
                                                 16.6)))]
        (if (= status 200)
          (do (log :debug "Heartbeat sent!")
              (p/publish {:topics topic
                          :response result})
              (a/<! (a/timeout next-heartbeat-delay))
              (recur))
          (do (log :error "Heartbeat failed; ceasing future heartbeats.")
              (state/set-session-expired! true)
              (p/publish {:topics topic
                          :error (e/session-heartbeats-failed-err resp)})
              nil))))))

(defn- start-session* [silent-monitoring]
  (go (let [resp (a/<! (rest/start-session-request silent-monitoring))
            {:keys [status api-response]} resp
            topic (topics/get-topic :session-started)
            session-details (assoc (:result api-response) :resource-id (state/get-active-user-id))]
        (if (= status 200)
          (do (state/set-session-details! (assoc session-details :expired? false))
              (p/publish {:topics topic
                          :response session-details})
              (go-not-ready)
              (start-heartbeats*))
          (p/publish {:topics topic
                      :error (e/failed-to-start-agent-session-err resp)}))
        nil)))

(defn- get-config* [no-session silent-monitoring callback]
  (go (let [topic (topics/get-topic :config-response)
            config-response (a/<! (rest/get-config-request))
            {:keys [api-response status]} config-response
            user-config (:result api-response)
            extensions (:extensions user-config)
            active-extension (:active-extension user-config)]
        (if (= status 200)
          (do (state/set-config! user-config)
              (state/set-session-expired! false)
              (p/publish {:topics topic
                          :response user-config})
              (p/publish {:topics (topics/get-topic :extension-list)
                          :response (select-keys user-config [:active-extension :extensions])})
              (if (nil? active-extension)
                (let [extension (first (state/get-all-extensions))
                      resp (a/<! (rest/update-user-request (state/get-active-user-id) {:activeExtension extension}))
                      status (:status resp)]
                  (if-not (= status 200)
                    (p/publish {:topics topic
                                :error (e/failed-to-update-extension-err resp)
                                :callback callback}))))
              (when-not no-session
                (start-session* silent-monitoring)
                (ih/send-core-message {:type :config-ready})))
          (p/publish {:topics topic
                      :error (e/failed-to-get-session-config-err config-response)}))))
  nil)

(s/def ::set-active-tenant-spec
  (s/keys :req-un [::specs/tenant-id]
          :opt-un [::specs/callback ::specs/no-session ::specs/silent-monitoring]))

(def-sdk-fn set-active-tenant
  "The setActiveTenant function is used to bootstrap necessary user and tenant
  data, integrations, and sessions. Tenant ID is it's only required parameter, but
  it allows for two other option parameters that impact how your session will
  behave.

  ``` javascript
  CxEngage.session.setActiveTenant({
    tenantId: '{{uuid}}',
    noSession: '{{bool}}', <Optional>
    silentMonitoring: '{{bool}}', <Optional>
  });
  ```
  The 'noSession' flag will set the user's active tenant as normal, but will
  forgo the session creation and heartbeat polling.

  The 'silentMonitoring' flag will set the user's active tenant as normal, and
  will start a session as normal, with the caveat that the session will be omitted
  from reporting data.


  Possible Errors:

  - [Session: 2001](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-session-config-err)
  - [Session: 2002](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-start-agent-session-err)
  - [Session: 2003](/cxengage-javascript-sdk.domain.errors.html#var-session-heartbeats-failed-err)
  - [Session: 2006](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-extension-err)
  - [Session: 2010](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-tenant-err)
  - [Session: 2011](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-region-err)
  "
  {:validation ::set-active-tenant-spec
   :topic-key :active-tenant-set}
  [params]
  (let [{:keys [callback topic tenant-id no-session silent-monitoring]} params
        tenant-permissions (state/get-tenant-permissions tenant-id)
        {:keys [status api-response] :as resp} (a/<! (rest/get-tenant-request tenant-id))
        region-id (get-in api-response [:result :region-id])]
    (if (not= status 200)
      (p/publish {:topics topic
                  :error (e/failed-to-get-tenant-err api-response)})
      (do
        (state/set-tenant-data! (:result api-response))
        (state/set-active-tenant! tenant-id)
        (let [{:keys [status api-response] :as resp} (a/<! (rest/get-region-request region-id))]
          (if (not= status 200)
            (p/publish {:topics topic
                        :error (e/failed-to-get-region-err api-response)})
            (do
              (state/set-region! (get-in api-response [:result :name]))
              (p/publish {:topics topic
                          :response {:tenant-id tenant-id}
                          :callback callback})
              (get-config* no-session silent-monitoring callback))))))))

;; ---------------------------------------------------------------------------------- ;;
;; CxEngage.session.setDirection({ direction: "{{inbound/outbound/agent-initiated}}" });
;; ---------------------------------------------------------------------------------- ;;

(s/def ::set-direction-spec
  (s/keys :req-un [::specs/direction]
          :opt-un [::specs/callback ::specs/agent-id ::specs/session-id]))

(def-sdk-fn set-direction
  "
  ``` javascript
  CxEngage.session.setDirection({
    direction: {{'inbound' / 'outbound' / 'agent-initiated'}}
    agentId: {{ uuid }} (Optional)
    sessionId: {{ uuid }} (Optional)
  });
  ```

  Used to set an Agent's direction to one of three available options:

  - inbound
  - outbound
  - agent-initiated

  Please refer to our [documentation](https://docs.cxengage.net/Help/Content/Skylight/Tutorials/SkylightCRMNavigation.htm?Highlight=direction) for a detailed description of the differences
  between each direction.

  This function will receive agentId and sessionId from Config-UI when changing direction
  from Agent State Monitoring.

  Possible Errors:

  - [Session: 2008](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-set-direction-err)
  "
  {:validation ::set-direction-spec
   :topic-key :set-direction-response}
  [params]
  (let [{:keys [callback topic direction agent-id session-id]} params
        resp (a/<! (rest/set-direction-request direction agent-id session-id))
        {:keys [status api-response]} resp
        direction-details {:direction direction
                           :session-id (get-in api-response [:result :session-id])}
        response-error (if-not (= status 200) (e/failed-to-set-direction-err resp))]
      (p/publish {:topics topic
                  :response direction-details
                  :error response-error
                  :callback callback})))

(s/def ::set-presence-state-spec
  (s/keys :req-un [::specs/state]
          :opt-un [::specs/callback ::specs/agent-id ::specs/session-id ::specs/reason ::specs/reason-id ::specs/reason-list-id ::specs/force-logout]))

(def-sdk-fn set-presence-state
  "
  ``` javascript
  CxEngage.session.setPresenceState({
    agentId: {{ uuid }} (Optional), (Required just when function is called from Agent Monitoring)
    sessionId: {{ uuid }} (Optional),
    state: {{'ready' / 'notready' / 'offline'}},
    reason: {{string}} (Optional),
    reasonId: {{uuid}} (Optional),
    reasonListId: {{uuid}} (Optional)
    forceLogout: {{boolean}} (Optional)
  });
  ```

  Used to set an Agent's presence state to one of three available options:

  - ready
  - notready
  - offline

  Please refer to our [documentation](https://docs.cxengage.net/Help/Content/Skylight/Tutorials/SkylightCRMNavigation.htm?Highlight=state) for a detailed description of the differences
  between each presence state.
  This function will receive agentId and sessionId from Config-UI when changing state
  from Agent State Monitoring.

  Possible Errors:

  - [Session: 2013](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-set-presence-state-err)
  "
  {:validation ::set-presence-state-spec
   :topic-key :set-presence-state-response}
  [params]
  (let [{:keys [callback topic agent-id session-id state reason reason-id reason-list-id force-logout]} params
        change-state-body {:session-id (or session-id (state/get-session-id))
                           :state state
                           :reason reason
                           :reason-id reason-id
                           :reason-list-id reason-list-id
                           :force-logout force-logout}
        resp (a/<! (rest/change-state-request change-state-body agent-id))
        {:keys [status api-response]} resp
        state-details (:result api-response)
        response-error (if-not (= status 200) (e/failed-to-set-presence-state-err resp))]
      (p/publish {:topics topic
                  :response state-details
                  :error response-error
                  :callback callback})))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.session.goReady({ extensionValue: "{{uuid/extension}}" });
;; -------------------------------------------------------------------------- ;;

(defn- go-ready* [topic callback]
  (go (let [session-id (state/get-session-id)
            resp (a/<! (rest/change-state-request {:session-id session-id
                                                   :state "ready"}
                                                  nil))
            {:keys [status api-response]} resp
            new-state-data (:result api-response)]
        (if (= status 200)
          (p/publish {:topics topic
                      :response new-state-data
                      :callback callback})
          (p/publish {:topics topic
                      :error (e/failed-to-change-state-err resp)
                      :callback callback})))))

(s/def ::go-ready-spec
  (s/keys :req-un [::specs/extension-value]
          :opt-un [::specs/callback]))

(def-sdk-fn go-ready
  "
  ``` javascript
  CxEngage.session.goReady({
    extensionValue: {{uuid}}
  });
  ```

  Used to put the Agent in a 'ready' state. Requires a UUID or value string to
  identity which extension to initalize.

  Possible Errors:

  - [Session: 2001](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-session-config-err)
  - [Session: 2005](/cxengage-javascript-sdk.domain.errors.html#var-invalid-extension-provided-err)
  - [Session: 2006](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-extension-err)
  - [Session: 2008](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-user-extensions-err)
  "
  {:validation ::go-ready-spec
   :topic-key :presence-state-change-request-acknowledged}
  [params]
  (let [{:keys [callback topic extension-value]} params
        resp (a/<! (rest/get-user-request))
        {:keys [status api-response]} resp
        {:keys [result]} api-response
        extensions (get-in api-response [:result :extensions])]
    (if-not (= status 200)
      (p/publish {:topics topic
                  :error (e/failed-to-get-user-extensions-err resp)
                  :callback callback})
      (do (state/set-extensions! extensions)
          (let [new-extension (state/get-extension-by-value extension-value)
                active-extension (state/get-active-extension)]
            (if-not new-extension
              (p/publish {:topics topic
                          :error (e/invalid-extension-provided-err (state/get-all-extensions))
                          :callback callback})
              (if-not (= active-extension new-extension)
                ;; Active extension was either nil (this user has never had an
                ;; active extension, E.G. they're a new user), or they *do* have an
                ;; active extension, but it didn't match the one they passed for the
                ;; session they're starting. Update their user prior to changing
                ;; state, so they go ready with the correct extension.
                (let [resp (a/<! (rest/update-user-request (state/get-active-user-id) {:activeExtension new-extension}))
                      {:keys [status api-response]} resp]
                  (if-not (= status 200)
                    (p/publish {:topics topic
                                :error (e/failed-to-update-extension-err resp)
                                :callback callback}))
                  (let [resp (a/<! (rest/get-config-request))
                        {:keys [status api-response]} resp
                        user-config (:result api-response)]
                    (if (not (= status 200))
                      (p/publish {:topics topic
                                  :error (e/failed-to-get-session-config-err resp)
                                  :callback callback})
                      (do (state/set-config! user-config)
                          (p/publish {:topics (topics/get-topic :extension-list)
                                      :response (select-keys user-config [:active-extension :extensions])})
                          (ih/send-core-message {:type :config-ready})
                          (p/publish {:topics (topics/get-topic :config-response)
                                      :response user-config
                                      :callback callback})
                          (go-ready* topic callback)))))

                ;; Their active extension and the extension they passed in are the
                ;; same, no user update request is necessary, simply go ready.
                (go-ready* topic callback))))))))

(defn get-active-user-id
  "``` javascript
  CxEngage.session.getActiveUserId();
  ```
  Used to fetch the active user ID currently stored in state. Will return null
  otherwise."
  [& params]
  (let [callback (first params)
        callback (if (fn? callback) callback nil)
        user-id (state/get-active-user-id)]
    (p/publish {:topic (topics/get-topic :get-active-user-id-response)
                :response user-id
                :callback callback})
    user-id))

(defn get-active-tenant-id
  "``` javascript
  CxEngage.session.getActiveTenantId();
  ```
  Used to fetch the active tenant ID currently stored in state. Will return null
  otherwise."
  [& params]
  (let [callback (first params)
        callback (if (fn? callback) callback nil)
        tenant-id (state/get-active-tenant-id)]
    (p/publish {:topic (topics/get-topic :get-active-tenant-id-response)
                :response tenant-id
                :callback callback})
    tenant-id))

(defn get-token
  "``` javascript
  CxEngage.session.getToken();
  ```
  Used to fetch the auth token currently stored in state. Will return null
  otherwise."
  [& params]
  (let [callback (first params)
        callback (if (fn? callback) callback nil)
        token (state/get-token)]
    (p/publish {:topic (topics/get-topic :get-token-response)
                :response token
                :callback callback})
    token))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.session.setToken();
;; -------------------------------------------------------------------------- ;;

(defn set-token
  "``` javascript
  CxEngage.session.setToken('{{string}}');
  ```
  Used to set the token property in the SDK's internal state. Useful for SSO as
  well as refreshing tokens."
  [& params]
  (let [token (first params)
        callback (second params)
        callback (if (fn? callback) callback nil)
        _ (state/set-token! token)]
    (p/publish {:topic (topics/get-topic :set-token-response)
                :response token
                :callback callback})
    token))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.session.setUserIdentity();
;; -------------------------------------------------------------------------- ;;

(defn set-user-identity
  "``` javascript
  CxEngage.session.setUserIdentity('{{uuid}}');
  ```
  Used to set the userIdentity property in the SDK's internal state."
  [& params]
  (let [user (first params)
        callback (second params)
        callback (if (fn? callback) callback nil)
        _ (state/set-user-identity! {:user-id user})]
    (p/publish {:topic (topics/get-topic :set-user-identity-response)
                :response user
                :callback callback})
    user))

(defn get-sso-token
  "``` javascript
  CxEngage.session.getSSOToken();
  ```
  Used to fetch the SSO token currently stored in state. Will return null
  otherwise."
  [& params]
  (let [callback (first params)
        callback (if (fn? callback) callback nil)
        token (state/get-sso-token)]
    (p/publish {:topic (topics/get-topic :get-sso-token-response)
                :response token
                :callback callback})
    token))

(defn set-locale
  "``` javascript
  CxEngage.session.setLocale('{{string}}');
  ```
  Used to set the locale property in the SDK's internal state."
  [& params]
  (let [param-obj (js->clj (first params) :keywordize-keys true)
        {:keys [locale]} param-obj
        callback (second params)
        callback (if (fn? callback) callback nil)]
    (state/set-locale! locale)
    (p/publish {:topic (topics/get-topic :set-locale-response)
                :response locale
                :callback callback})
    locale))

(defn get-default-extension
  "``` javascript
  CxEngage.session.getDefaultExtension();
  ```
  Used to fetch the user's default extension currently stored in state. Will
  return null otherwise."
  [& params]
  (let [callback (first params)
        callback (if (fn? callback) callback nil)
        extension (clj->js (state/get-default-extension))]
    (p/publish {:topic (topics/get-topic :get-default-extension-response)
                :response extension
                :callback callback})
    extension))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.session.getTenantDetails();
;; -------------------------------------------------------------------------- ;;

(s/def ::get-tenant-details-spec
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn get-tenant-details
  "``` javascript
  CxEngage.session.getTenantDetails();
  ```

  This function is used to retrieve details of all tenants associated with the
  actively authenticated user.

  Possible Errors:

  - [Session: 2012](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-tenant-details-err)"
  {:validation ::get-tenant-details-spec
   :topic-key :get-tenant-details}
  [params]
  (let [{:keys [callback topic]} params
        resp (a/<! (rest/get-tenant-details-request))
        {:keys [status api-response]} resp
        tenant-details {:details (:result api-response)}]
    (if (= status 200)
      (p/publish {:topics topic
                  :response tenant-details
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-tenant-details-err resp)
                  :callback callback}))))

(defn get-monitored-interaction
  "``` javascript
  CxEngage.session.getMonitoredInteraction();
  ```
  Used to fetch the interaction ID of a call being silent monitored. Will return null
  otherwise."
  [& params]
  (let [callback (first params)
        callback (if (fn? callback) callback nil)
        interaction (state/get-monitored-interaction)]
    (p/publish {:topic (topics/get-topic :get-monitored-interaction-response)
                :response interaction
                :callback callback})
    interaction))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.session.clearMonitoredInteraction();
;; -------------------------------------------------------------------------- ;;

(defn clear-monitored-interaction
  "``` javascript
  CxEngage.session.clearMonitoredInteraction();
  ```
  Used to clear the interaction ID once a call is no longer being monitored."
  [& params]
  (let [callback (first params)
        callback (if (fn? callback) callback nil)
        _ (state/set-monitored-interaction! nil)]
    (p/publish {:topic (topics/get-topic :set-monitored-interaction-response)
                :response nil
                :callback callback})
    nil))

;; -------------------------------------------------------------------------- ;;
;; SDK Presence Session Module
;; -------------------------------------------------------------------------- ;;

(defrecord SessionModule []
  pr/SDKModule
  (start [this]
    (let [module-name :session]
      (ih/register {:api {module-name {:set-active-tenant set-active-tenant
                                       :go-ready go-ready
                                       :go-not-ready go-not-ready
                                       :set-direction set-direction
                                       :set-presence-state set-presence-state
                                       :get-active-user-id get-active-user-id
                                       :get-active-tenant-id get-active-tenant-id
                                       :get-token get-token
                                       :get-sso-token get-sso-token
                                       :set-locale set-locale
                                       :set-token set-token
                                       :set-user-identity set-user-identity
                                       :get-default-extension get-default-extension
                                       :get-tenant-details get-tenant-details
                                       :get-monitored-interaction get-monitored-interaction
                                       :clear-monitored-interaction clear-monitored-interaction}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
