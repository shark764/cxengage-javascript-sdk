(ns cxengage-javascript-sdk.interop-helpers
  (:require [cxengage-javascript-sdk.state :as state]
            [camel-snake-kebab.core :as camel]
            [camel-snake-kebab.extras :refer [transform-keys]]))

(defn register [module-details]
  (let [f (aget js/window "CxEngage" "registerModule")]
    (f module-details)))

(defn send-core-message [message]
  (let [f (aget js/window "CxEngage" "sendCoreMessage")]
    (f message)))

(defn set-sqs-poller-interval [interval-id]
  (aset js/window "CxEngage" "internal" "SQSPoller" interval-id))

(defn get-sqs-poller-interval []
  (aget js/window "CxEngage" "internal" "SQSPoller"))

(defn set-sdk-global [sdk]
  (aset js/window "CxEngage" sdk))

(defn get-sdk-global []
  (aget js/window "CxEngage"))

(defn camelify [m]
  (->> m
       (transform-keys camel/->camelCase)
       (clj->js)))

(defn kebabify [m]
  (->> m
       (#(js->clj % :keywordize-keys true))
       (transform-keys camel/->kebab-case)))

(defn extract-params
  ([params]
   (extract-params params false))
  ([params preserve-casing?]
   (if preserve-casing?
     (js->clj params :keywordize-keys true)
     (if (= :cljs (state/get-consumer-type))
       params
       (kebabify (js->clj params :keywordize-keys true))))))

(defn js-publish [publish-details]
  (let [publish (aget js/window "CxEngage" "publish")
        details (extract-params publish-details)]
    (publish details false)))
