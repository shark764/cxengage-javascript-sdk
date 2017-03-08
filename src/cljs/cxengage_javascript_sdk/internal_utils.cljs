(ns cxengage-javascript-sdk.internal-utils
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [goog.crypt :as c]
            [ajax.core :as ajax]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.state :as state]
            [camel-snake-kebab.core :as camel]
            [camel-snake-kebab.extras :refer [transform-keys]])
  (:import [goog.crypt Sha256 Hmac]))

(defn deep-merge
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn camelify [m]
  (->> m
       (transform-keys camel/->camelCase)
       (clj->js)))

(defn kebabify [m]
  (->> m
       (#(js->clj % :keywordize-keys true))
       (transform-keys camel/->kebab-case)))

(defn build-api-url-with-params [url params]
  (let [{:keys [tenant-id resource-id session-id entity-id entity-sub-id contact-id layout-id interaction-id artifact-id]} params]
    (cond-> url
      tenant-id (clojure.string/replace #"tenant-id" (str tenant-id))
      resource-id (clojure.string/replace #"resource-id" (str resource-id))
      session-id (clojure.string/replace #"session-id" session-id)
      entity-id (clojure.string/replace #"entity-id" entity-id)
      entity-sub-id (clojure.string/replace #"entity-sub-id" entity-sub-id)
      contact-id (clojure.string/replace #"contact-id" contact-id)
      layout-id (clojure.string/replace #"layout-id" layout-id)
      interaction-id (clojure.string/replace #"interaction-id" interaction-id)
      artifact-id (clojure.string/replace #"artifact-id" artifact-id))))

(defn normalize-response-stucture
  [[ok? response]]
  (if (and (false? ok?)
           (= (:status response) 200))
    {:api-response nil :status 200}
    (let [status (if ok? 200 (get response :status))
          api-response (-> response
                           (dissoc :status)
                           (kebabify))]
      {:api-response api-response :status status})))

(defn api-request [request-map]
  (let [response-channel (a/promise-chan)
        {:keys [method url body]} request-map
        request (merge {:uri url
                        :method method
                        :timeout 30000
                        :handler #(let [normalized-response (normalize-response-stucture %)]
                                    (a/put! response-channel normalized-response))
                        :format (ajax/json-request-format)
                        :response-format (ajax/json-response-format {:keywords? true})}
                       (when body
                         {:params body})
                       (when-let [token (state/get-token)]
                         {:headers {"Authorization" (str "Token " token)}}))]
    (ajax/ajax-request request)
    response-channel))

(defn format-response [response]
  (if (= :cljs (state/get-consumer-type))
    response
    (clj->js response)))

(defn extract-params [params]
  (if (= :cljs (state/get-consumer-type))
    params
    (kebabify params)))

(defn base-module-request
  ([type]
   (base-module-request type nil))
  ([type additional-params]
   (let [token (state/get-token)
         resp-chan (a/promise-chan)]
     (cond-> {:type type
              :resp-chan resp-chan}
       additional-params (merge additional-params)
       token (merge {:token token})))))

(defn publish* [thing]
  ((aget js/window "serenova" "cxengage" "api" "publish") thing))

(defn send-interrupt*
  ([module params]
   (let [params (extract-params params)
         module-state @(:state module)
         {:keys [interaction-id interrupt-type interrupt-body topic on-confirm-fn callback]} params
         tenant-id (state/get-active-tenant-id)
         interrupt-request {:method :post
                            :body {:source "client"
                                   :interrupt-type interrupt-type
                                   :interrupt interrupt-body}
                            :url (str (state/get-base-api-url) "tenants/" tenant-id "/interactions/" interaction-id "/interrupts")}]
     (do (go (let [interrupt-response (a/<! (api-request interrupt-request))
                   {:keys [api-response status]} interrupt-response]
               (if (not= status 200)
                 (publish* {:topics topic
                            :error (e/api-error api-response)
                            :callback callback})
                 (do (publish* {:topics topic
                                :response {:interacton-id interaction-id}
                                :callback callback})
                     (when on-confirm-fn (on-confirm-fn))))))
         nil))))

;;;;;;;;;;;;;;;
;; sigv4 utils
;;;;;;;;;;;;;;;

(defn sign
  [key msg]
  (let [hmac (doto (Hmac. (Sha256.) key)
               (.update msg))]
    (c/byteArrayToHex (.digest hmac))))
(defn sha256
  [msg]
  (let [hash (doto (Sha256.)
               (.update msg))]
    (c/byteArrayToHex (.digest hash))))
(defn get-signature-key
  [key date-stamp region-name service-name]
  (let [date-stamp (or date-stamp (js/Date.))
        k-date (doto (Hmac. (Sha256.) (c/stringToByteArray (str "AWS4" key))) (.update date-stamp))
        k-region (doto (Hmac. (Sha256.)  (.digest k-date)) (.update region-name))
        k-service (doto (Hmac. (Sha256.) (.digest k-region)) (.update service-name))
        k-signing (doto (Hmac. (Sha256.) (.digest k-service)) (.update "aws4_request"))]
    (.digest k-signing)))
