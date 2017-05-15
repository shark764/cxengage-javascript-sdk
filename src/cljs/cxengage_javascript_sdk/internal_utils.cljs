(ns cxengage-javascript-sdk.internal-utils
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a]
            [goog.crypt :as c]
            [ajax.core :as ajax]
            [clojure.string :as str]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [camel-snake-kebab.core :as camel]
            [camel-snake-kebab.extras :refer [transform-keys]])
  (:import [goog.crypt Sha256 Hmac]))

(defn normalize-phone-number
  [phone-number]
  (-> phone-number
      (clojure.string/replace #"\+" "")
      (clojure.string/replace #" " "")))

(defn deep-merge
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn api-url
  ([url]
   (str (state/get-base-api-url) url))
  ([url params]
   (reduce-kv
    (fn [s k v]
      (clojure.string/replace s (re-pattern (name k)) v))
    (str (state/get-base-api-url) url)
    params)))

(defn get-now
  []
  (let [offset (state/get-time-offset)
        correct-date (js/Date.now)
        correct-date (- correct-date offset)]
    correct-date))

(defn update-local-time-offset
  [response]
  (when-let [timestamp (get-in response [:api-response :result :timestamp])]
    (let [local (js/Date.now)
          server (js/Date.parse timestamp)
          offset (- local server)]
      (state/set-time-offset! offset))))

(defn normalize-response-stucture
  [[ok? response] preserve-casing? manifest-endpoint?]
  (if (and (false? ok?)
           (= (:status response) 200))
    {:api-response nil :status 200}
    (let [status (if ok? 200 (get response :status))
          response (if (or preserve-casing? manifest-endpoint?) response (ih/kebabify response))
          api-response (if (map? response)
                         (dissoc response :status)
                         response)]
      {:api-response api-response :status status})))

(defn str-long-enough? [len st]
  (>= (.-length st) len))

(defn service-unavailable?
  [status]
  (= status 503))

(defn client-error?
  [status]
  (and (>= status 400)
       (<= status 499)))

(defn server-error?
  [status]
  (and (>= status 500)
       (<= status 599)))

(defn request-success?
  [status]
  (and (>= status 200)
       (<= status 299)))

(defn api-request
  ([request-map]
   (api-request request-map false))
  ([request-map preserve-casing?]
   (let [resp-chan (a/promise-chan)]
     (go-loop [failed-attempts 0]
       (let [response-channel (a/promise-chan)
             {:keys [method url body]} request-map
             manifest-endpoint? (if url
                                  (not= -1 (.indexOf url "artifacts.s3"))
                                  false)
             request (merge {:uri url
                             :method method
                             :timeout 120000
                             :handler #(let [normalized-response (normalize-response-stucture % preserve-casing? manifest-endpoint?)]
                                         (update-local-time-offset normalized-response)
                                         (a/put! response-channel normalized-response))
                             :format (ajax/json-request-format)
                             :response-format (if manifest-endpoint?
                                                (ajax/text-response-format)
                                                (ajax/json-response-format {:keywords? true}))}
                            (when body
                              {:params (if preserve-casing? body (ih/camelify body))})
                            (when-let [token (state/get-token)]
                              (if manifest-endpoint?
                                {}
                                {:headers {"Authorization" (str "Token " token)}})))]
         (ajax/ajax-request request)
         (let [response (a/<! response-channel)
               {:keys [status]} response]
           (if (and (service-unavailable? status) (< failed-attempts 3))
             (do
               (js/console.error (str "Received server error " status " retrying in " (* 3 failed-attempts) " seconds."))
               (a/<! (a/timeout (* 3000 (+ 1 failed-attempts))))
               (recur (inc failed-attempts)))
             (do (when (request-success? status)
                   (a/put! resp-chan response))
                 (when (client-error? status)
                   (ih/js-publish {:topics "cxengage/errors/error/api-rejected-bad-client-request"
                                   :error (e/client-request-err)}))
                 (when (server-error? status)
                   (ih/js-publish {:topics "cxengage/errors/error/api-encountered-internal-server-error"
                                   :error (e/internal-server-err)})))))))
     resp-chan)))

(defn file-api-request [request-map]
  (let [response-channel (a/promise-chan)
        {:keys [method url body callback]} request-map
        request (merge {:uri url
                        :method method
                        :timeout 120000
                        :handler #(let [normalized-response (normalize-response-stucture % false true)]
                                    (if callback
                                      (callback normalized-response)
                                      (a/put! response-channel normalized-response)))
                        :format (ajax/json-request-format)
                        :response-format (ajax/json-response-format {:keywords? true})
                        :body body}
                       (when-let [token (state/get-token)]
                         {:headers {"Authorization" (str "Token " token)}}))]
    (ajax/ajax-request request)
    (if callback
      nil
      response-channel)))

(defn get-artifact [interaction-id tenant-id artifact-id]
  (let [url (api-url
             "tenants/tenant-id/interactions/interaction-id/artifacts/artifact-id"
             {:tenant-id tenant-id
              :interaction-id interaction-id
              :artifact-id artifact-id})
        artifact-request {:method :get
                          :url url}]
    (api-request artifact-request)))

(defn get-interaction-files [interaction-id]
  (let [tenant-id (state/get-active-tenant-id)
        url (api-url
             "tenants/tenant-id/interactions/interaction-id/artifacts"
             {:interaction-id interaction-id
              :tenant-id tenant-id})
        file-request {:method :get
                      :url url}]
    (api-request file-request)))

(defn send-interrupt*
  [params]
  (let [params (ih/extract-params params)
        {:keys [interaction-id interrupt-type interrupt-body topic on-confirm-fn callback]} params
        tenant-id (state/get-active-tenant-id)
        interrupt-request {:method :post
                           :body {:source "client"
                                  :interrupt-type interrupt-type
                                  :interrupt interrupt-body}
                           :url (str (state/get-base-api-url) "tenants/" tenant-id "/interactions/" interaction-id "/interrupts")}]
    (go (let [interrupt-response (a/<! (api-request interrupt-request))
              {:keys [api-response status]} interrupt-response]
          (when (= status 200)
            (ih/js-publish {:topics topic
                            :response (merge {:interaction-id interaction-id} interrupt-body)
                            :callback callback})
            (when on-confirm-fn (on-confirm-fn)))))
    nil))

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
