(ns cxengage-javascript-sdk.interop-helpers)

(defn register [module-details]
  (let [f (aget js/window "serenova" "cxengage" "modules" "register")]
    (f module-details)))

(defn send-core-message [message]
  (let [f (aget js/window "serenova" "cxengage" "api" "sendCoreMessage")]
    (f message)))
