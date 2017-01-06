(ns client-sdk.api.auth
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [lumbajack.core :refer [log]]
            [client-sdk.state :as state]
            [client-sdk.api.helpers :as h]))

(defn login-handler [module-chan response-chan params]
  (let [{:keys [callback token]} (h/extract-params params)
        msg (h/base-module-request :AUTH/LOGIN response-chan token)]
    (a/put! module-chan msg)
    (go (let [response (a/<! response-chan)
              {:keys [result]} response]
          (state/set-user-identity! result)
          (a/put! (state/get-module-chan :pubsub)
                  (merge {:msg-type :AUTH/LOGIN_RESPONSE} response))
          (when callback (callback (h/format-response result)))))))

(s/def ::token string?)
(s/def ::callback fn?)
(s/def ::get-token-params
  (s/keys :req-un [::token]
          :opt-un [::callback]))

(defn get-token-handler [module-chan response-chan params]
  (if-not (s/valid? ::get-token-params (js->clj params :keywordize-keys true))
    (do (log :error "Incorrect parameters")
        (s/explain-data ::get-token-params (js->clj params :keywordize-keys true)))
    (do (log :info "Params are valid")
        (let [{:keys [callback username password]} (h/extract-params params)
              msg (merge (h/base-module-request :AUTH/GET_TOKEN response-chan)
                         {:username username
                          :password password})]
          (a/put! module-chan msg)
          (go (let [response (a/<! response-chan)
                    {:keys [token]} response]
                (state/set-token! token)
                (login-handler module-chan (a/promise-chan) {:callback callback :token token})))))))

(defn api [] {:login (partial get-token-handler
                              (state/get-module-chan :authentication)
                              (a/promise-chan))})
