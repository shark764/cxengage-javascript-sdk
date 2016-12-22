(ns client-sdk.api.session
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [client-sdk.state :as state]))

(defn set-active-tenant-handler [tenant-id callback]
  (state/set-active-tenant! tenant-id)
  (let [config-result-chan (a/promise-chan)
        config-msg {:resp-chan config-result-chan
                    :token (state/get-token)
                    :tenant-id (state/get-active-tenant)
                    :user-id (state/get-active-user-id)}
        config-chan (state/get-module-chan :auth :config)]
    (a/put! config-chan config-msg)
    (go (let [{:keys [result]} (a/<! config-result-chan)]
          (state/set-config! result)
          (let [session-result-chan (a/promise-chan)
                session-msg {:resp-chan session-result-chan
                             :token (state/get-token)
                             :tenant-id (state/get-active-tenant)
                             :user-id (state/get-active-user-id)}
                session-chan (state/get-module-chan :presence :start-session)]
            (a/put! session-chan session-msg)
            (go (let [{:keys [result]} (a/<! session-result-chan)]
                  (log :debug "Got result from setting session from presence module")
                  (state/set-session-details! result)
                  (callback))))))))

(defn set-direction-handler [direction callback]
  (let [direction-result-chan (a/promise-chan)
        direction-msg {:resp-chan direction-result-chan
                       :token (state/get-token)
                       :session-id (state/get-session-id)}
        direction-chan (state/get-module-chan :presence :set-direction)]
    (a/put! direction-chan direction-msg)
    (go (let [{:keys [result]} (a/<! direction-result-chan)]
          (state/set-direction! result)
          (callback)))))


(defn check-capacity-handler [callback]
  (let [capacity-result-chan (a/promise-chan)
        capacity-msg {:resp-chan capacity-result-chan
                      :token (state/get-token)
                      :tenant-id (state/get-active-tenant)
                      :user-id (state/get-active-user-id)}
        capacity-chan (state/get-module-chan :presence :check-capacity)]
    (a/put! capacity-chan capacity-msg)
    (go (let [{keys [result]} (a/<! capacity-result-chan)]
          (state/set-capacity! result)
          (callback)))))

(defn change-state-handler [state reason-details callback]
  (let [state-result-chan (a/promise-chan)
        {:keys [reason reasonId reasonListId]} reason-details
        state-msg {:resp-chan state-result-chan
                   :token (state/get-token)
                   :tenant-id (state/get-active-tenant)
                   :user-id (state/get-active-user-id)
                   :state state
                   :reason reason
                   :reason-id reasonId
                   :reason-list-id reasonListId}
        state-chan (state/get-module-chan :presence :change-state)]
    (a/put! state-chan state-msg)
    (go (let [{keys [result]} (a/<! state-result-chan)]
          (state/set-user-state! result)
          (callback)))))

(def api {:setActiveTenant set-active-tenant-handler
          :changeState change-state-handler
          :getCapacity check-capacity-handler
          :setDirection set-direction-handler})
