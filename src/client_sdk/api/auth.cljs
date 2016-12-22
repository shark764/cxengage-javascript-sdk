(ns client-sdk.api.auth
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [client-sdk.state :as state]))

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

(def api {:login login-handler})
