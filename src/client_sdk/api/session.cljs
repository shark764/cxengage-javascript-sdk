(ns client-sdk.api.session
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [lumbajack.core :refer [log]]
            [client-sdk.state :as state]))

(defn start-session [module-chan callback]
  (let [session-result-chan (a/promise-chan)
        session-msg {:type :PRESENCE/START_SESSION
                     :resp-chan session-result-chan
                     :token (state/get-token)
                     :tenant-id (state/get-active-tenant-id)
                     :user-id (state/get-active-user-id)}]
    (a/put! module-chan session-msg)
    (go (let [session-result (a/<! session-result-chan)]
          (state/set-session-details! session-result)
          (log :info "Successfully initiated presence session")
          (callback)))))

(defn set-active-tenant-handler [module-chan tenant-id callback]
  (state/set-active-tenant! tenant-id)
  (let [config-result-chan (a/promise-chan)
        config-msg {:type :AUTH/GET_CONFIG
                    :resp-chan config-result-chan
                    :token (state/get-token)
                    :tenant-id (state/get-active-tenant-id)
                    :user-id (state/get-active-user-id)}]
    (a/put! (state/get-module-chan :auth) config-msg)
    (go (let [{:keys [result]} (a/<! config-result-chan)]
          (state/set-config! result)
          (start-session module-chan callback)))))

(defn set-direction-handler [module-chan params callback]
  (let [{:keys [direction]} (js->clj params :keywordize-keys true)
        direction-result-chan (a/promise-chan)
        direction-msg {:type :PRESENCE/SET_DIRECTION
                       :resp-chan direction-result-chan
                       :token (state/get-token)
                       :tenant-id (state/get-active-tenant-id)
                       :sessionId (state/get-session-id)
                       :user-id (state/get-active-user-id)
                       :initiatorId (state/get-active-user-id)
                       :direction direction}]
    (a/put! module-chan direction-msg)
    (go (let [{:keys [result]} (a/<! direction-result-chan)]
          (state/set-direction! direction)
          (callback)

(defn change-state-handler [module-chan params callback]
  (let [state-change-chan (a/promise-chan)
        {:keys [state]} (js->clj params :keywordize-keys true)
        state-msg {:type :PRESENCE/CHANGE_STATE
                   :resp-chan state-change-chan
                   :token (state/get-token)
                   :tenant-id (state/get-active-tenant-id)
                   :user-id (state/get-active-user-id)

                   :sessionId (state/get-session-id)
                   :state state}]
    (a/put! module-chan state-msg)
    (go (let [{:keys [result]} (a/<! state-change-chan)]
          (state/set-user-state! result)
          (callback)))

(defn api []
  (let [module-chan (state/get-module-chan :presence)]
    {:setActiveTenant (partial set-active-tenant-handler module-chan)
     :changeState (partial change-state-handler module-chan)
     :setDirection (partial set-direction-handler module-chan)}))
