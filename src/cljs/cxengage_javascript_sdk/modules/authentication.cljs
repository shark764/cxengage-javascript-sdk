(ns cxengage-javascript-sdk.modules.authentication
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]))

(s/def ::not-empty-string #(not= 0 (.-length %)))
(s/def ::username (s/and string? ::not-empty-string))
(s/def ::password (s/and string? ::not-empty-string))
(s/def ::login-params
  (s/keys :req-un [::username ::password]
          :opt-un [:specs/callback]))

(defn login
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (first others))
     (e/wrong-number-of-args-error)
     (login module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         module-state @(:state module)
         {:keys [username password callback]} params
         login-topic (p/get-topic :login-response)]
     (if (not (s/valid? ::login-params params))
       (p/publish {:topics login-topic
                   :error (e/invalid-args-error (s/explain-data ::login-params params))
                   :callback callback})
       (let [token-body {:username username
                         :password password}
             api-url (get-in module [:config :api-url])
             token-url (str api-url (get-in module-state [:urls :token]))
             login-url (str api-url (get-in module-state [:urls :login]))
             token-request {:method :post
                            :url token-url
                            :body token-body}
             login-request {:method :post
                            :url login-url}]
         (go (let [token-response (a/<! (iu/api-request token-request))
                   {:keys [status api-response]} token-response]
               (if (not= status 200)
                 (p/publish {:topics login-topic
                             :error (e/api-error "non 200 response")
                             :callback callback})
                 (do (st/set-token! (:token api-response))
                     (let [login-response (a/<! (iu/api-request login-request))
                           {:keys [status api-response]} login-response]
                       (if (not= status 200)
                         (p/publish {:topics login-topic
                                     :error (e/api-error "non 200 response")
                                     :callback callback})
                         (let [{:keys [result]} api-response]
                           (st/set-user-identity! result)
                           (p/publish {:topics login-topic
                                       :response result
                                       :callback callback})
                           (p/publish {:topics (p/get-topic :tenant-list)
                                       :response (:tenants result)}))))))))
         nil)))))

(def initial-state
  {:module-name :authentication
   :topics ["login"]
   :urls {:token "tokens"
          :login "login"}})

(defn get-entity [module entity-type])

(defrecord AuthenticationModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)]
      (register {:api {module-name {:login (partial login this)}}
                 :module-name module-name})
      (a/put! core-messages< {:module-registration-status :success :module module-name})
      (log :info (str "<----- Started " (name module-name) " SDK module! ----->"))))
  (stop [this]))
