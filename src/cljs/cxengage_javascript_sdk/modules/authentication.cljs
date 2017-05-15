(ns cxengage-javascript-sdk.modules.authentication
  (:require-macros [cxengage-javascript-sdk.macros :refer [def-sdk-fn]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.interop-helpers :as ih]))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.authentication.logout();
;; -------------------------------------------------------------------------- ;;

(s/def ::logout-spec
  (s/keys :req-un []
          :opt-un []))

(def-sdk-fn logout
  {:validation ::logout-spec
   :topic-key :presence-state-change-request-acknowledged}
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
                                     :state "offline"}}
        {:keys [status api-response]} (a/<! (iu/api-request change-state-request))
        new-state-data (:result api-response)]
    (if (= status 200)
      (do (state/set-session-expired! true)
          (p/publish {:topics topic
                      :response new-state-data
                      :callback callback}))
      (p/publish {:topic topic
                  :callback callback
                  :error (e/logout-failed-err)}))))

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
        token-request {:method :post
                       :url (iu/api-url "tokens")
                       :body {:username username
                              :password password}}
        {:keys [status api-response]} (a/<! (iu/api-request token-request))]
    (when (= status 200)
      (state/set-token! (:token api-response))
      (let [login-request {:method :post
                           :url (iu/api-url "login")}
            {:keys [status api-response]} (a/<! (iu/api-request login-request))]
        (if (= status 200)
          (let [user-identity (:result api-response)
                tenants (:tenants user-identity)]
            (state/set-user-identity! user-identity)
            (p/publish {:topics (p/get-topic :tenant-list)
                        :response tenants})
            (p/publish {:topics topic
                        :response user-identity
                        :callback callback}))
          (p/publish {:topics topic
                      :callback callback
                      :error (e/login-failed-err)}))))))

;; -------------------------------------------------------------------------- ;;
;; SDK Authentication Module
;; -------------------------------------------------------------------------- ;;

(defrecord AuthenticationModule []
  pr/SDKModule
  (start [this]
    (let [module-name :authentication]
      (ih/register {:api {module-name {:login login
                                       :logout logout}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
