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
;; -------------------------------------------------------------------------- ;;

(s/def ::login-spec
  (s/keys :req-un [::specs/username ::specs/password]
          :opt-un [::specs/callback]))

(def-sdk-fn login
  {:validation ::login-spec
   :topic-key :login-response}
  [params]
  (let [{:keys [callback topic username password]} params
        token-body {:username username
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
                          :callback callback}))))))))


;; -------------------------------------------------------------------------- ;;
;; Cognito Initialization Function
;; -------------------------------------------------------------------------- ;;

(defn cognito-auth-sdk-init
  [client-details]
  (let [{:keys [client domain]} client-details
        region (state/get-region)
        auth-data (clj->js {:ClientId client
                            :AppWebDomain (str domain ".auth." region ".amazoncognito.com")
                            :TokenScopesArray ["email"]
                            :RedirectUriSignIn (str js/window.location.origin "/")
                            :RedirectUriSignOut (str js/window.location.origin "/")})
        auth (new js/AWSCognito.CognitoIdentityServiceProvider.CognitoAuth
                  auth-data)
        _ (aset auth "userhandler" (clj->js {:onSuccess (fn [result]
                                                          (let [token (aget (.getAccessToken result) "jwtToken")]
                                                            (state/set-sso-token! token)
                                                            (p/publish {:topics (topics/get-topic :cognito-initialized-response)
                                                                        :response token})))
                                             :onFailure (fn [error]
                                                          (p/publish {:topics (topics/get-topic :cognito-initialized-response)
                                                                      :error (e/failed-to-init-cognito-sdk-err auth-data)}))}))]
     auth))

(defn attach-script
  [integration]
  (let [script (js/document.createElement "script")
        body (.-body js/document)]
    (.setAttribute script "type" "text/javascript")
    (.setAttribute script "src" integration)
    (.appendChild body script)))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.authentication.getAuthInfo({
;;   username: "{{string}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::auth-info-spec
  (s/keys :req-un [::specs/username]
          :opt-un [::specs/callback]))

(def-sdk-fn get-auth-info
  {:validation ::auth-info-spec
   :topic-key :auth-info-response}
  [params]
  (let [{:keys [callback topic username]} params
        _ (state/reset-state)
        resp (a/<! (rest/get-sso-details-request username))
        {:keys [status api-response]} resp]
    (if (not (= status 200))
      (p/publish {:topics topic
                  :callback callback
                  :error (e/login-failed-token-request-err resp)})
      (let [{:keys [domain client]} api-response
            _ (js/localStorage.setItem "ssoClient" (js/JSON.stringify
                                                      (clj->js {:client client
                                                                :domain domain})))
            auth (cognito-auth-sdk-init api-response)]
        (.getSession auth)))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.authentication.SSOLogin({
;;   token: "{{string}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::sso-login-spec
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn sso-login
  {:validation ::sso-login-spec
   :topic-key :login-response}
  [params]
  (let [{:keys [callback topic token]} params
        resp (a/<! (rest/login-request))
        {:keys [status api-response]} resp]
    (if (not (= status 200))
      (p/publish {:topics topic
                  :callback callback
                  :error (e/login-failed-token-request-err resp)})
      (let [user-identity (:result api-response)
            tenants (:tenants user-identity)]
        (state/set-user-identity! user-identity)
        (p/publish {:topics (topics/get-topic :tenant-list)
                    :response tenants})
        (p/publish {:topics topic
                    :response user-identity
                    :callback callback})))))

;; -------------------------------------------------------------------------- ;;
;; SDK Authentication Module
;; -------------------------------------------------------------------------- ;;

(defrecord AuthenticationModule []
  pr/SDKModule
  (start [this]
    (let [module-name :authentication
          client-details (js->clj (js/JSON.parse (js/localStorage.getItem "ssoClient")) :keywordize-keys true)]
      (attach-script "https://sdk.cxengage.net/js/aws/aws-cognito-sdk.min.js")
      (go-loop []
        (if (ih/cognito-ready?)
          (attach-script "https://sdk.cxengage.net/js/aws/amazon-cognito-auth.min.js")
          (do (a/<! (a/timeout 250))
              (recur))))
      (go-loop []
        (if (and (ih/cognito-ready?) (ih/cognito-auth-ready?))
          (when client-details
            (let [auth (cognito-auth-sdk-init client-details)
                  _ (.parseCognitoWebResponse auth js/window.location.href)]))
          (do (a/<! (a/timeout 250))
              (recur))))
      (ih/register {:api {module-name {:login login
                                       :logout logout
                                       :sso-login sso-login
                                       :get-auth-info get-auth-info}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
