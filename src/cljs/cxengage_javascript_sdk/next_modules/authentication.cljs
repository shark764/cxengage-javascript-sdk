(ns cxengage-javascript-sdk.next-modules.authentication
  (:require-macros [cxengage-javascript-sdk.macros :refer [def-sdk-fn]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [cxengage-javascript-sdk.interop-helpers :as ih]))

(defn str-long-enough?
  [len st]
  (>= (.-length st) len))

(s/def ::min-8-len-string (and string?
                               (partial str-long-enough? 8)))
(s/def ::username ::min-8-len-string)
(s/def ::password ::min-8-len-string)
(s/def ::callback #(or (fn? %) (nil? %)))
(s/def ::login-spec
  (s/keys :req-un [::username ::password]
          :opt-un [::callback]))

(def-sdk-fn logout
  ::logout-spec
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
                                     :state "offline"}}
        {:keys [status api-response]} (a/<! (iu/api-request change-state-request))
        new-state-data (:result api-response)]
    (when (= status 200)
      (state/set-session-expired! true)
      (p/publish {:topics topic
                  :response new-state-data
                  :callback callback}))))

(def-sdk-fn login
  ::login-spec
  (p/get-topic :login-response)
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
        (when (= status 200)
          (let [user-identity (:result api-response)
                tenants (:tenants user-identity)]
            (state/set-user-identity! user-identity)
            (p/publish {:topics (p/get-topic :tenant-list)
                        :response tenants})
            (p/publish {:topics topic
                        :response user-identity
                        :callback callback})))))))

(defrecord AuthenticationModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (let [module-name :authentication]
      (ih/register {:api {module-name {:login login
                                       :logout logout}}
                    :module-name module-name})
      (a/put! core-messages< {:module-registration-status :success
                              :module module-name})
      (log :info (str "<----- Started " (name module-name) " SDK module! ----->"))))
  (stop [this]))
