(ns cxengage-javascript-sdk.interop-helpers
  (:require [cljs.core.async :as a]))

(defn register [module-details]
  (let [f (aget js/window "CxEngage" "registerModule")]
    (f module-details)))

(defn send-core-message [message]
  (let [f (aget js/window "CxEngage" "sendCoreMessage")]
    (f message)))

(defn publish [message]
  (let [f (aget js/window "CxEngage" "publish")]
    (f message)))
