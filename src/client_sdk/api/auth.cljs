(ns client-sdk.api.auth
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [client-sdk.pubsub :refer [publish!]]
            [client-sdk.state :as state]
            [client-sdk.api.helpers :as h]))

;; -- Private

(defn login-handler [params]
  (let [module-chan (state/get-module-chan :authentication)
        response-chan (a/promise-chan)
        {:keys [callback token]} (h/extract-params params)
        msg (h/base-module-request :AUTH/LOGIN response-chan token)]
    (a/put! module-chan msg)
    (go (let [response (a/<! response-chan)
              {:keys [result]} response]
          (state/set-user-identity! result)
          (publish! "cxengage/authentication/login-response" (h/format-response result))
          (when callback (callback (h/format-response result)))))))

#_(s/def ::token string?)
#_(s/def ::callback fn?)
#_(s/def ::get-token-params
    (s/keys :req-un [::token]
            :opt-un [::callback]))

#_ (if-not (s/valid? ::get-token-params (js->clj params :keywordize-keys true))
     (do (log :error "Incorrect parameters")
         (s/explain-data ::get-token-params (js->clj params :keywordize-keys true))))

;; -- Public

(defn login
  ([params callback]
   (login (merge params {:callback callback})))
  ([params]
   (let [module-chan (state/get-module-chan :authentication)
         response-chan (a/promise-chan)
         {:keys [callback username password]} (h/extract-params params)
         msg (merge (h/base-module-request :AUTH/GET_TOKEN response-chan)
                    {:username username
                     :password password})]

     #_{:type :AUTH/GET_TOKEN
        :resp-chan (a/promise-chan)
        :username ...
        :password ...}

     (a/put! module-chan msg)
     (go (let [response (a/<! response-chan)
               {:keys [token]} response]
           (state/set-token! token)
           (login-handler {:callback callback :token token}))))))
