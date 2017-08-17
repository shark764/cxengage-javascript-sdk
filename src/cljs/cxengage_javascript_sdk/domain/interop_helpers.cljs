(ns cxengage-javascript-sdk.domain.interop-helpers
  (:require [camel-snake-kebab.core :as camel]
            [camel-snake-kebab.extras :refer [transform-keys]]))

(defn register [module-details]
  (let [f (aget js/window "CxEngage" "registerModule")]
    (f module-details)))

(defn send-core-message [message]
  (let [f (aget js/window "CxEngage" "sendCoreMessage")]
    (f message)))

(defn set-log-level! [level]
  (aset js/window "CxEngage" "logging" "level" (name level)))

(defn get-log-level []
  (or (aget js/window "CxEngage" "logging" "level") "debug"))

(defn set-sdk-global [sdk]
  (aset js/window "CxEngage" sdk))

(defn get-sdk-global []
  (aget js/window "CxEngage"))

(defn set-time-offset! [offset]
  (let [offset-fn (aget js/window "CxEngage" "internal" "setTimeOffset")]
    (offset-fn offset)))

(defn get-token []
  (let [token-fn (aget js/window "CxEngage" "session" "getToken")]
    (token-fn)))

(defn get-active-tenant-id []
  (let [tenant-fn (aget js/window "CxEngage" "session" "getActiveTenantId")]
    (tenant-fn)))

(defn get-active-user-id []
  (let [user-fn (aget js/window "CxEngage" "session" "getActiveUserId")]
    (user-fn)))

(defn publish [params]
  (let [pub-fn (aget js/window "CxEngage" "publish")]
    (pub-fn params)))

(defn subscribe [topic callback]
  (let [sub-fn (aget js/window "CxEngage" "subscribe")]
    (sub-fn topic callback)))

(defn twilio-ready? []
  (and (aget js/window "Twilio")
       (aget js/window "Twilio" "Device")
       (aget js/window "Twilio" "Device" "setup")))

(defn core-ready? []
  (and (aget js/window "CxEngage")
       (aget js/window "CxEngage" "registerModule")))

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
    (kebabify (js->clj params :keywordize-keys true)))))
