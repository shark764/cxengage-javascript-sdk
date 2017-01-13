(ns client-sdk.api.session
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [client-sdk.state :as state]
            [client-sdk.api.helpers :as h]
            [client-sdk.domain.specs :as specs]
            [client-sdk.domain.errors :as err]))

(s/def ::change-state-params
    (s/keys :req-un [::specs/state]
            :opt-un [::specs/callback]))

(defn change-state [params]
  (if-not (s/valid? ::change-state-params (js->clj params :keywordize-keys true))
      (err/invalid-params-err)
      (let [module-chan (state/get-module-chan :presence)
            response-chan (a/promise-chan)
            {:keys [state callback]} (h/extract-params params)
            msg (merge (h/base-module-request :SESSION/CHANGE_STATE response-chan (state/get-token))
                       {:tenant-id (state/get-active-tenant-id)
                        :user-id (state/get-active-user-id)
                        :sessionId (state/get-session-id)
                        :state state})]
        (a/put! module-chan msg)
        (go (let [response (a/<! response-chan)
                  {:keys [result]} response]
              (state/set-user-session-state! result)
              (when callback (callback)))))))

(s/def ::start-session-params
    (s/keys :opt-un [::specs/callback]))

(defn start-session [params]
  (if-not (s/valid? ::start-session-params (js->clj params :keywordize-keys true))
      (err/invalid-params-err)
      (let [module-chan (state/get-module-chan :presence)
            response-chan (a/promise-chan)
            {:keys [callback]} (h/extract-params params)
            msg (merge (h/base-module-request :SESSION/START_SESSION response-chan (state/get-token))
                       {:tenant-id (state/get-active-tenant-id)
                        :user-id (state/get-active-user-id)})]
        (a/put! module-chan msg)
        (go (let [session-result (a/<! response-chan)]
              (state/set-session-details! session-result)
              (log :info "Successfully initiated presence session")
              (change-state {:state "notready"
                             :sessionId (state/get-session-id)})
              (a/put! (state/get-async-module-registration) {:module-name :sqs :config (state/get-session-details)})
              (when callback (callback)))))))

(s/def ::set-active-tenant-params
    (s/keys :req-un [::specs/tenantId]
            :opt-un [::specs/callback]))

(defn set-active-tenant [params]
  (if-not (s/valid? ::set-active-tenant-params (js->clj params :keywordize-keys true))
      (err/invalid-params-err)
      (let [module-chan (state/get-module-chan :presence)
            response-chan (a/promise-chan)
            {:keys [tenantId callback]} (h/extract-params params)
            msg (merge (h/base-module-request :AUTH/GET_CONFIG response-chan (state/get-token))
                       {:tenant-id tenantId
                        :user-id (state/get-active-user-id)})]
        (state/set-active-tenant! tenantId)
        (a/put! (state/get-module-chan :authentication) msg)
        (go (let [response (a/<! response-chan)
                  {:keys [result]} response]
              (a/put! (state/get-async-module-registration) {:module-name :mqtt :config result})
              (a/put! (state/get-async-module-registration) {:module-name :twilio :config result})
              (state/set-config! result)
              (start-session {:callback callback}))))))
