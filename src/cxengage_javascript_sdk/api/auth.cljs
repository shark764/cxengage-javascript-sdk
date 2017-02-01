(ns cxengage-javascript-sdk.api.auth
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.module-gateway :as mg]
            [cxengage-javascript-sdk.domain.errors :as err]
            [cxengage-javascript-sdk.pubsub :refer [sdk-response sdk-error-response]]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]))

(s/def ::username string?)
(s/def ::password string?)
(s/def ::login-params
  (s/keys :req-un [::username ::password]
          :opt-un [::specs/callback]))

(defn login
  ([params callback]
   (login (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [pubsub-topic "cxengage/authentication/login"
         params (iu/extract-params params)
         {:keys [callback]} params]
     (if-not (s/valid? ::login-params params)
       (sdk-error-response pubsub-topic (err/invalid-params-err) callback)
       (let [{:keys [username password]} params
             token-msg (iu/base-module-request
                        :AUTH/GET_TOKEN
                        {:username username
                         :password password})]
         (go (let [token-response (a/<! (mg/send-module-message token-msg))
                   {:keys [token]} token-response
                   _ (state/set-token! token)
                   login-msg (iu/base-module-request :AUTH/LOGIN)]
               (let [login-response (a/<! (mg/send-module-message login-msg))
                     {:keys [result]} login-response]
                 (state/set-user-identity! result)
                 (sdk-response pubsub-topic result callback)
                 nil))))))))
