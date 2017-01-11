(ns client-sdk.api.session
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [client-sdk.state :as state]
            [client-sdk.api.helpers :as h]))

(defn change-state [params]
  (let [module-chan (state/get-module-chan :presence)
        response-chan (a/promise-chan)
        _ (log :debug "params in change state" (h/extract-params params))
        _ (log :debug "session id in change state:" (state/get-session-id))
        {:keys [state callback]} nil
        msg (merge (h/base-module-request :SESSION/CHANGE_STATE response-chan (state/get-token))
                   {:tenant-id (state/get-active-tenant-id)
                    :user-id (state/get-active-user-id)
                    :sessionId (state/get-session-id)
                    :state state})]
    (a/put! module-chan msg)
    (go (log :debug "resp chan in go block:" response-chan)
        (log :debug "module-chan in go block:" module-chan)
        (let [response (a/<! response-chan)
              {:keys [result]} response]
          (state/set-user-session-state! result)
          (when callback (callback))))))

(defn start-session [params]
  (log :debug "ACTIVE TID" (state/get-active-tenant-id))
  (log :debug "ACTIVE UID" (state/get-active-user-id))
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
          (log :warn (state/get-session-id))
          (change-state {:state "notready"
                         :sessionId (state/get-session-id)})
          (a/put! (state/get-async-module-registration) {:module-name :sqs :config (state/get-session-details)})
          (when callback (callback))))))

(defn set-active-tenant [params]
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
          (start-session {:callback callback})))))

#_(defn set-direction [params]
    (let [module-chan (state/get-module-chan :presence)
          response-chan (a/promise-chan)
          {:keys [direction callback]} (h/extract-params params)
          msg (merge (h/base-module-request :SESSION/SET_DIRECTION response-chan (state/get-token))
                     {:tenant-id (state/get-active-tenant-id)
                      :sessionId (state/get-session-id)
                      :user-id (state/get-active-user-id)
                      :initiatorId (state/get-active-user-id)
                      :direction direction})]
      (a/put! module-chan msg)
      (go (let [response (a/<! response-chan)
                {:keys [result]} response]
            (state/set-direction! direction)
            (a/put! (state/get-module-chan :pubsub)
                    (merge {:msg-type :SESSION/SET_DIRECTION_RESPONSE} response {:sessionId (state/get-session-id)}))
            (when callback (callback))))))
