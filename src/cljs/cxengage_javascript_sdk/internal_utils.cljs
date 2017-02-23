(ns cxengage-javascript-sdk.internal-utils
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [goog.crypt :as c]
            [cxengage-javascript-sdk.state :as state])
  (:import [goog.crypt Sha256 Hmac]))

(defn format-response [response]
  (if (= :cljs (state/get-consumer-type))
    response
    (clj->js response)))

(defn extract-params [params]
  (if (= :cljs (state/get-consumer-type))
    params
    (js->clj params :keywordize-keys true)))

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
