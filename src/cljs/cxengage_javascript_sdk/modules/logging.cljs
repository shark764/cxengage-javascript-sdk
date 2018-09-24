(ns cxengage-javascript-sdk.modules.logging
  (:require-macros [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]])
  (:require [cljs.spec.alpha :as s]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.state :as state]))

;; -------------------------------;;
;; CxEngage.logging.setLevel({ level: "info" });
;; -------------------------------;;

(s/def ::set-level-params
  (s/keys :req-un [::specs/level]
          :opt-un [::specs/callback]))

(def-sdk-fn set-level
  ""
  {:validation ::set-level-params
   :topic-key :log-level-set}
  [params]
  (let [{:keys [level topic callback]} params
        level (keyword level)]
    (ih/set-log-level! level)
    (p/publish {:topics topic
                :response level
                :callback callback})))

;; -------------------------------------------------------------------------- ;;
;; SDK Logging Module
;; -------------------------------------------------------------------------- ;;

(defrecord LoggingModule []
  pr/SDKModule
  (start [this]
    (let [module-name :logging]
      (ih/register {:api {module-name {:set-level set-level}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
