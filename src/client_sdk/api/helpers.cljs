(ns client-sdk.api.helpers
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [client-sdk.state :as state]))

(defn sdk-handler [] nil)

(defn format-response [response]
  (if (= :cljs (state/get-consumer-type))
    response
    (clj->js response)))

(defn extract-params [params]
  (if (= :cljs (state/get-consumer-type))
    params
    (js->clj params :keywordize-keys true)))

(defn base-module-request
  ([type result-chan]
   (base-module-request type result-chan nil))
  ([type result-chan token]
   (merge {:resp-chan result-chan
           :type type}
          (when token {:token token}))))