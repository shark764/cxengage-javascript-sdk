(ns client-sdk.api
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [client-sdk.state :as state]
            [cljs.core.async :as a]))

;; -- public

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

(defn login-handler [params callback]
  (let [token-result-chan (a/promise-chan)
        token-msg (-> params
                      (js->clj :keywordize-keys true)
                      (assoc :resp-chan token-result-chan))
        token-chan (state/get-module-chan :auth :token)]
    (a/put! token-chan token-msg)
    (go (let [{:keys [token]} (a/<! token-result-chan)
              login-result-chan (a/promise-chan)
              login-chan (state/get-module-chan :auth :login)]
          (state/set-token! token)
          (a/put! login-chan {:token token :resp-chan login-result-chan})
          (let [{:keys [result]} (a/<! login-result-chan)]
            (state/set-user-identity! result)
            (callback (clj->js result)))))))

(def api
  (clj->js {:session {:setActiveTenant set-active-tenant-handler}
            :auth {:login login-handler}}))
