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
                    :user-id (state/get-active-user)}
        config-chan (state/get-module-chan :auth :config)]
    (a/put! config-chan config-msg)
    (go (let [{:keys [result]} (a/<! config-result-chan)]
          (state/set-config! result)
          (let [session-result-chan (a/promise-chan)
                session-msg {:resp-chan session-result-chan
                             :token (state/get-token)
                             :tenant-id (state/get-active-tenant)
                             :user-id (state/get-active-user)}
                session-chan (state/get-module-chan :presence :start-session)]
            (a/put! session-chan session-msg)
            (go (let [{:keys [result]} (a/<! session-result-chan)]
                 (state/set-session-details! result)
                 (callback))))))))


(defn set-direction-handler [direction callback]
   (state/set-direction! direction)
   (let [direction-result-chan (a/promise-chan)
         direction-msg {:resp-chan direction-result-chan
                        :token (state/get-token)
                        :session-id (state/get-session-id)}
         direction-chan (state/get-module-chan :presence :set-direction)]
      (a/put! direction-chan direction-msg)
      (go (let [{:keys [result]} (a/<! direction-result-chan)]
           (state/set-direction! result)
           (callback)))))


(def api {:setActiveTenant set-active-tenant-handler})
