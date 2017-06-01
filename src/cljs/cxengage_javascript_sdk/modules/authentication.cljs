(ns cxengage-javascript-sdk.modules.authentication
  (:require-macros [cljs-sdk-utils.macros :refer [def-sdk-fn]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.spec :as s]
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
                  :error (e/active-interactions-err)
                  :callback callback})
      (let [session-id (state/get-session-id)
            change-state-body {:session-id session-id
                               :state "offline"}
            {:keys [status api-response]} (a/<! (rest/change-state-request change-state-body))
            new-state-data (:result api-response)]
        (if (= status 200)
          (do (state/set-session-expired! true)
              (p/publish {:topics topic
                          :response new-state-data
                          :callback callback}))
          (p/publish {:topic topic
                      :callback callback
                      :error (e/logout-failed-err)}))))))

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
        {:keys [status api-response]} (a/<! (rest/token-request token-body))]
    (if (not (= status 200))
      (p/publish {:topics topic
                  :callback callback
                  :error (e/login-failed-err)})
      (do
        (state/set-token! (:token api-response))
        (let [{:keys [status api-response]} (a/<! (rest/login-request))]
          (if (not (= status 200))
            (p/publish {:topics topic
                        :callback callback
                        :error (e/login-failed-err)})
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
