(ns cxengage-javascript-sdk.internal-utils
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [goog.crypt :as c]
            [ajax.core :as ajax]
            [clojure.string :as str]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
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

;; Copied from https://github.com/clojure/core.incubator/blob/master/src/main/clojure/clojure/core/incubator.clj#L63
(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

;; From https://stackoverflow.com/a/3249777
(defn in?
  "Returns true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))

(defn api-url
  ([url]
   (str (state/get-base-api-url) url))
  ([url params]
   (try
     (reduce-kv
         (fn [s k v]
           (clojure.string/replace s (re-pattern (str k)) v))
         (str (state/get-base-api-url) url)
         params)
     (catch js/Object e
       (log :warn "An exception occurred attempting to form an API URL.")
       (log :warn "URL provided:" url)
       (log :warn "Params provided:" (clj->js params))
       nil))))

(defn get-now
  []
  (let [offset (state/get-time-offset)
        correct-date (js/Date.now)
        correct-date (- correct-date offset)]
    correct-date))

(defn uuid-to-seconds
  "As per RFC 4122, extracts the timestamp from a UUID v1, and converts to milliseconds since unix epoch."
  [uuid]
  (let [uuid (str uuid)
        ;;_ (log :debug "UUID Passed to (uuid-to-seconds):" uuid)
        split-id (str/split uuid "-")
        gregorian-offset 122192928000000000
        upper-time-bitsv (vector (subs (nth split-id 2) 1))
        lower-mid-time-bitsv (vec (reverse (subvec split-id 0 2)))]
    (-> upper-time-bitsv
        (into lower-mid-time-bitsv)
        (str/join)
        (js/parseInt 16)
        (- gregorian-offset)
        (/ 10000)
        (js/Math.floor))))

(defn uuid-came-before? [before after]
  (< (uuid-to-seconds before) (uuid-to-seconds after)))

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
