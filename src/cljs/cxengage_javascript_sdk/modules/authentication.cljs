(ns cxengage-javascript-sdk.modules.authentication
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn]])
  (:require [cljs.spec.alpha :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]))

(s/def ::logout-spec
  (s/keys :req-un []))

(def-sdk-fn logout
  "``` javascript
  CxEngage.authentication.logout();
  ```
  Logs the active Agent out, and sets their state to 'Offline'. This function
  is unable to be called if there are any active interactions.

  Possible Errors:

  - [Interaction:    4000](/cxengage-javascript-sdk.domain.errors.html#var-active-interactions-err)
  - [Authentication: 3001](/cxengage-javascript-sdk.domain.errors.html#var-logout-failed-err)"
  {:validation ::logout-spec
   :topic-key :presence-state-change-request-acknowledged}
  [params]
  (let [{:keys [callback topic]} params]
    (if (state/active-interactions?)
      (p/publish {:topics topic
                  :error (e/active-interactions-err (state/get-all-active-interactions))
                  :callback callback})
      (let [session-id (state/get-session-id)
            change-state-body {:session-id session-id
                               :state "offline"}
            resp (a/<! (rest/change-state-request change-state-body))
            {:keys [status api-response]} resp
            new-state-data (:result api-response)]
        (if (= status 200)
          (do (state/set-session-expired! true)
              (p/publish {:topics topic
                          :response new-state-data
                          :callback callback}))
          (p/publish {:topic topic
                      :error (e/logout-failed-err resp)
                      :callback callback}))))))

(s/def ::login-spec
  (s/or
    :credentials (s/keys :req-un [::specs/username ::specs/password]
                         :opt-un [::specs/ttl ::specs/callback])
    :token (s/keys :req-un [::specs/token]
                   :opt-un [::specs/callback])))

(def-sdk-fn login
  "The login function is able to be called two different ways. Either with a
   username and password, or a token. When logging in with a username and password
   you can optionally pass a TTL as well.
  ``` javascript
  CxEngage.authentication.login({
    username: '{{string}}',
    password: '{{string}}'
    ttl: '{{number}}'
  });
  ```
  ``` javascript
  CxEngage.authentication.login({
    token: '{{string}}'
  });
  ```
  Possible Errors:

  - [Authentication: 3000](/cxengage-javascript-sdk.domain.errors.html#var-login-failed-token-request-err)
  - [Authentication: 3002](/cxengage-javascript-sdk.domain.errors.html#var-login-failed-login-request-err)"
  {:validation ::login-spec
   :topic-key :login-response}
  [params]
  (let [{:keys [callback topic username password ttl token]} params]
    (if token
      (let [_ (state/set-token! token)
            login-response (a/<! (rest/login-request))
            {:keys [status api-response]} login-response
            error (if-not (= status 200) (e/login-failed-login-request-err login-response))]
          (if-not (= status 200)
            (p/publish {:topics topic
                        :callback callback
                        :error error})
            (let [user-identity (:result api-response)
                  tenants (:tenants user-identity)]
              (state/set-user-identity! user-identity)
              (p/publish {:topics (topics/get-topic :tenant-list)
                          :response tenants})
              (p/publish {:topics topic
                          :response user-identity
                          :callback callback}))))
      (let [token-body {:username username
                        :password password
                        :ttl ttl}
            _ (state/reset-state)
            resp (a/<! (rest/token-request token-body))
            {:keys [status api-response]} resp]
        (if (not (= status 200))
          (p/publish {:topics topic
                      :callback callback
                      :error (e/login-failed-token-request-err resp)})
          (do
            (state/set-token! (:token api-response))
            (let [resp (a/<! (rest/login-request))
                  {:keys [status api-response]} resp]
              (if (not (= status 200))
                (p/publish {:topics topic
                            :callback callback
                            :error (e/login-failed-login-request-err resp)})
                (let [user-identity (:result api-response)
                      tenants (:tenants user-identity)]
                  (state/set-user-identity! user-identity)
                  (p/publish {:topics (topics/get-topic :tenant-list)
                              :response tenants})
                  (p/publish {:topics topic
                              :response user-identity
                              :callback callback}))))))))))

(s/def ::auth-info-spec
  (s/or
    :tenant (s/keys :req-un [::specs/tenant-id]
                    :opt-un [::specs/idp-id ::specs/callback])
    :email (s/keys :req-un [::specs/username]
                   :opt-un [::specs/callback])))

(def-sdk-fn get-auth-info
  "The getAuthInfo function is used to retrieve a user's Single Sign On details,
   and when used in conjunction with the popIdentityPage function - will open a
   window for a user to sign into their third party SAML provider. There are
   multiple ways to call this function to get a particular identity provider for
   a user. The first way is to simply use their email - which will grab their
   default tenant's 'client' and 'domain' fields.

  ``` javascript
  CxEngage.authentication.getAuthInfo({
      username: '{{string}}'
  })
  ```
  The second way to use this function is to specify a tenant ID - which will be
  used to retrieve that tenant's default identity provider information in order
  to open the third party sign on window.

  ``` javascript
  CxEngage.authentication.getAuthInfo({
      tenantId: '{{uuid}}'
  })
  ```
  The third way is to specify an identity provider ID in addition to a tenant ID
  in order to retrieve the information if it is not the default identity provider
  on the tenant.

  ``` javascript
  CxEngage.authentication.getAuthInfo({
      tenantId: '{{uuid}}',
      idpId: '{{uuid}}'
  })
  ```

  Possible Errors:

  - [Authentication: 3005](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-auth-info-err)
  "
  {:validation ::auth-info-spec
   :topic-key :auth-info-response}
  [params]
  (let [{:keys [callback topic username tenant-id idp-id]} params
        _ (state/reset-state)
        tenant-details (if idp-id
                         {:idp-id idp-id
                          :tenant-id tenant-id}
                         {:tenant-id tenant-id})
        resp (a/<! (rest/get-sso-details-request (or username tenant-details)))
        {:keys [status api-response]} resp
        _ (state/set-sso-client-details! api-response)]
    (if (not (= status 200))
      (p/publish {:topics topic
                  :callback callback
                  :error (e/failed-to-get-auth-info-err resp)})
      (p/publish {:topics topic
                  :callback callback
                  :response api-response}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.authentication.popIdentityPage();
;; -------------------------------------------------------------------------- ;;

(defn- post-message-handler [event]
  (let [token (aget event "data" "token")
        error (aget event "data" "error")]
    (if error
      (p/publish {:topics (topics/get-topic :cognito-auth-response)
                  :error (e/failed-cognito-auth-err error)})
      (do
        (state/set-token! token)
        (p/publish {:topics (topics/get-topic :cognito-auth-response)
                    :response token})))))

(s/def ::identity-window-spec
  (s/keys :req-un []
          :opt-un [::specs/callback]))

;; cxengageSsoWindow opened here will be blocked by popup blockers by default.
;; The window must be previously opened by a on-click handler to not be blocked.
(def-sdk-fn pop-identity-page
  "The popIdentityPage function is used to open a third party Single Sign On
   provider using the information gathered from the getAuthInfo function. This
   information is stored in the SDK's internal state, and does not require it
   passed in as parameters. We require this to be a separate function in order
   to bypass the browser's popup blocker.

   ``` javascript
   CxEngage.authentication.popIdentityPage();
   ```"
  {:validation ::identity-window-spec
   :topic-key :identity-window-response}
  [params]
  (let [client-details (state/get-sso-client-details)
        api (state/get-base-api-url)
        env (name (state/get-env))
        details (state/get-sso-client-details)
        {:keys [client domain]} (or (:result details) details)
        url (if (= env "prod")
              "https://identity.cxengage.net"
              "https://identity.cxengagelabs.net")
        window (js/window.open (str url "?env=" api "&domain=" domain "&clientid=" client) "cxengageSsoWindow" "width=500,height=500")
        _ (js/window.addEventListener "message" post-message-handler (clj->js {:once true}))]
    (go-loop []
      (if (= (.-closed window) false)
        (do
          (.postMessage window "Polling for Token" "*")
          (do (a/<! (a/timeout 1000))
              (recur)))
        (p/publish {:topics (topics/get-topic :identity-window-response)
                    :response true})))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.authentication.updateDefaultTenant({
;;  tenantId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

;; This function is here rather than say, entities, because it's purpose is to
;; be used before tenant selection, in order to update the default SSO tenant

(s/def ::update-default-tenant-spec
  (s/keys :req-un [::specs/tenant-id]
          :opt-un [::specs/callback]))

(def-sdk-fn update-default-tenant
  ""
  {:validation ::update-default-tenant-spec
   :topic-key :update-default-tenant-response}
  [params]
  (let [{:keys [callback topic tenant-id]} params
        resp (a/<! (rest/update-default-tenant-request tenant-id))
        {:keys [status api-response]} resp]
    (if (not (= status 200))
      (p/publish {:topics topic
                  :callback callback
                  :error (e/failed-to-update-default-tenant-err resp)})
      (p/publish {:topics topic
                  :callback callback
                  :response api-response}))))

;; -------------------------------------------------------------------------- ;;
;; SDK Authentication Module
;; -------------------------------------------------------------------------- ;;

(defrecord AuthenticationModule []
  pr/SDKModule
  (start [this]
    (let [module-name :authentication]
      (ih/register {:api {module-name {:login login
                                       :logout logout
                                       :get-auth-info get-auth-info
                                       :pop-identity-page pop-identity-page
                                       :update-default-tenant update-default-tenant}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
