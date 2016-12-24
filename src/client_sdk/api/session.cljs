(ns client-sdk.api.session
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [lumbajack.core :refer [log]]
            [client-sdk.state :as state]
            [client-sdk.api.helpers :as h]))

(defn start-session [module-chan response-chan params]
  (let [{:keys [callback]} (h/extract-params params)
        msg (merge (h/base-module-request :PRESENCE/START_SESSION response-chan (state/get-token))
                   {:tenant-id (state/get-active-tenant-id)
                    :user-id (state/get-active-user-id)})]
    (a/put! module-chan msg)
    (go (let [session-result (a/<! response-chan)]
          (state/set-session-details! session-result)
          (log :info "Successfully initiated presence session")
          (when callback (callback))))))

(defn set-active-tenant-handler [module-chan response-chan params]
  (let [{:keys [tenantId callback]} (h/extract-params params)
        msg (merge (h/base-module-request :AUTH/GET_CONFIG response-chan (state/get-token))
                   {:tenant-id tenantId
                    :user-id (state/get-active-user-id)})]
    (state/set-active-tenant! tenantId)
    (a/put! (state/get-module-chan :auth) msg)
    (go (let [{:keys [result]} (a/<! response-chan)]
          (a/put! (state/get-async-module-registration) {:name :sqs :config result})
          (state/set-config! result)
          (start-session module-chan (a/promise-chan) {:callback callback})))))

(defn set-direction-handler [module-chan response-chan params]
  (let [{:keys [direction callback]} (h/extract-params params)
        msg (merge (h/base-module-request :PRESENCE/SET_DIRECTION response-chan (state/get-token))
                   {:tenant-id (state/get-active-tenant-id)
                    :sessionId (state/get-session-id)
                    :user-id (state/get-active-user-id)
                    :initiatorId (state/get-active-user-id)
                    :direction direction})]
    (a/put! module-chan msg)
    (go (let [{:keys [result]} (a/<! response-chan)]
          (state/set-direction! direction)
          (when callback (callback))))))

(defn change-state-handler [module-chan response-chan params]
  (let [{:keys [state callback]} (h/extract-params params)
        msg (merge (h/base-module-request :PRESENCE/CHANGE_STATE response-chan (state/get-token))
                   {:tenant-id (state/get-active-tenant-id)
                    :user-id (state/get-active-user-id)
                    :sessionId (state/get-session-id)
                    :state state})]
    (a/put! module-chan msg)
    (go (let [{:keys [result]} (a/<! response-chan)]
          (state/set-user-state! result)
          (when callback (callback))))))

(defn api []
  (let [module-chan (state/get-module-chan :presence)]
    {:setActiveTenant (partial set-active-tenant-handler module-chan (a/promise-chan))
     :changeState (partial change-state-handler module-chan (a/promise-chan))
     :setDirection (partial set-direction-handler module-chan (a/promise-chan))}))
