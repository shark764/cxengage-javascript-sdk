(ns cxengage-javascript-sdk.modules.authentication
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn]]
                   [lumbajack.macros :refer [log]])
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.authentication.logout();
;; -------------------------------------------------------------------------- ;;

(s/def ::logout-spec
  (s/keys :req-un []))

(def-sdk-fn logout
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.authentication.login({
;;   username: "{{string}}",
;;   password: "{{string}}"
;; });
;;
;; OR
;;
;; CxEngage.authentication.login({
;;   token: "{{string}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::login-spec
  (s/or
    :credentials (s/keys :req-un [::specs/username ::specs/password]
                         :opt-un [::specs/callback])
    :token (s/keys :req-un [::specs/token]
                   :opt-un [::specs/callback])))

(def-sdk-fn login
  {:validation ::login-spec
   :topic-key :login-response}
  [params]
  (let [{:keys [callback topic username password token]} params]
    (if token
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
                          :callback callback}))))
      (let [token-body {:username username
                        :password password}
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.authentication.getAuthInfo({
;;   username: "{{string}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::auth-info-spec
  (s/or
    :tenant (s/keys :req-un [::specs/tenant-id]
                    :opt-un [::specs/idp-id ::specs/callback])
    :email (s/keys :req-un [::specs/username]
                   :opt-un [::specs/callback])))

(def-sdk-fn get-auth-info
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


(defn post-message-handler [window event]
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
  {:validation ::identity-window-spec
   :topic-key :identity-window-response}
  [params]
  (let [client-details (state/get-sso-client-details)
        api (state/get-base-api-url)
        env (name (state/get-env))
        {:keys [client domain]} (state/get-sso-client-details)
        url (if (= env "prod")
              "https://identity.cxengage.net"
              "https://identity.cxengagelabs.net")
        window (js/window.open (str url "?env=" api "&domain=" domain "&clientid=" client) "cxengageSsoWindow" "width=500,height=500")
        _ (js/window.addEventListener "message" (partial post-message-handler window))]
    (go-loop []
      (if (= (.-closed window) false)
        (do
          (.postMessage window "Polling for Token" "*")
          (do (a/<! (a/timeout 1000))
              (recur)))
        (p/publish {:topics (topics/get-topic :identity-window-response)
                    :response true})))))

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
                                       :pop-identity-page pop-identity-page}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
