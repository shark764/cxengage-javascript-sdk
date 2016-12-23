(ns client-sdk.api.auth
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [lumbajack.core :refer [log]]
            [client-sdk.state :as state]))

(defn login-handler [module-chan params callback]
  (let [token-result-chan (a/promise-chan)
        token-msg (-> params
                      (js->clj :keywordize-keys true)
                      (assoc :resp-chan token-result-chan
                             :type :AUTH/GET_TOKEN))]
    (a/put! module-chan token-msg)
    (go (let [{:keys [token]} (a/<! token-result-chan)
              login-result-chan (a/promise-chan)
              login-chan (state/get-module-chan :auth)]
          (state/set-token! token)
          (a/put! login-chan {:token token :resp-chan login-result-chan :type :AUTH/LOGIN})
          (let [{:keys [result]} (a/<! login-result-chan)]
            (state/set-user-identity! result)
            (callback (clj->js result)))))))

(defn api [] {:login (partial login-handler (state/get-module-chan :auth))})
