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
          (callback)))))

(def api {:setActiveTenant set-active-tenant-handler})
