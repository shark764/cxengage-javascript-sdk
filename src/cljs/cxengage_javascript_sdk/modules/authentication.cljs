(ns cxengage-javascript-sdk.modules.authentication
  (:require-macros [cljs-sdk-utils.macros :refer [def-sdk-fn]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.spec.alpha :as s]
            [cljs.core.async :as a]
            [cljs-sdk-utils.specs :as specs]
            [cljs-sdk-utils.errors :as e]
            [cljs-sdk-utils.topics :as topics]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs-sdk-utils.protocols :as pr]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cljs-sdk-utils.interop-helpers :as ih]))

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
