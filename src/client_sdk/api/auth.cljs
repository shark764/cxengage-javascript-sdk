(ns client-sdk.api.auth
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [client-sdk.domain.errors :as err]
            [client-sdk.pubsub :refer [publish!]]
            [client-sdk.state :as state]
            [client-sdk.api.helpers :as h]
            [client-sdk.domain.specs :as specs]))

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

;; -- Public

(s/def ::username string?)
(s/def ::password string?)
(s/def ::login-params
  (s/keys :req-un [::username ::password]
          :opt-un [::specs/callback]))

(defn login
  ([params callback]
   (login (merge params {:callback callback})))
  ([params]
   (let [params (h/extract-params params)]
     (if-not (s/valid? ::login-params params)
       (err/invalid-params-err)
       (let [module-chan (state/get-module-chan :authentication)
             response-chan (a/promise-chan)
             {:keys [callback username password]} params
             msg (merge (h/base-module-request :AUTH/GET_TOKEN response-chan)
                        {:username username
                         :password password})]
         (a/put! module-chan msg)
         (go (let [response (a/<! response-chan)
                   {:keys [token]} response]
               (state/set-token! token)
               (login-handler {:callback callback :token token}))))))))
