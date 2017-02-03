(ns cxengage-javascript-sdk.internal-utils
  (:require [cljs.core.async :as a]
            [cxengage-javascript-sdk.state :as state]))

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
