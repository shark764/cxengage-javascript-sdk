(ns client-sdk.api.auth
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [lumbajack.core :refer [log]]
            [client-sdk.pubsub :refer [publish!]]
            [client-sdk.state :as state]
            [client-sdk.api.helpers :as h]))

(defn login-handler [module-chan response-chan params]
  (let [{:keys [callback token]} (h/extract-params params)
        msg (h/base-module-request :AUTH/LOGIN response-chan token)]
    (a/put! module-chan msg)
    (go (let [{:keys [result]} (a/<! response-chan)]
          (state/set-user-identity! result)
          (when callback (callback (h/format-response result)))))))

(defn get-token-handler [module-chan response-chan params]
  (let [{:keys [callback username password]} (h/extract-params params)
        msg (merge (h/base-module-request :AUTH/GET_TOKEN response-chan)
                   {:username username
                    :password password})]
    (a/put! module-chan msg)
    (go (let [{:keys [token]} (a/<! response-chan)]
          (state/set-token! token)
          (login-handler module-chan (a/promise-chan) {:callback callback :token token})))))

(defn api [] {:login (partial get-token-handler
                              (state/get-module-chan :auth)
                              (a/promise-chan))})
