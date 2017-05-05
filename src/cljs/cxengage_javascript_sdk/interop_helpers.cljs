(ns cxengage-javascript-sdk.interop-helpers)

(defn register [module-details]
  (let [f (aget js/window "CxEngage" "registerModule")]
    (f module-details)))

(defn send-core-message [message]
  (let [f (aget js/window "CxEngage" "sendCoreMessage")]
    (f message)))

(defn set-sdk-global [sdk]
  (aset js/window "CxEngage" sdk))

(defn get-sdk-global []
  (aget js/window "CxEngage"))
