(ns cxengage-javascript-sdk.modules.testing
    (:require [cxengage-javascript-sdk.domain.interop-helpers :as ih]
              [cxengage-javascript-sdk.domain.protocols :as pr]))

;; -------------------------------;;
;; CxEngage.testing.throwError();
;; -------------------------------;;
(defn throw-error [& params]
    (throw (js/Error. "SDK Error")))

;; -------------------------------------------------------------------------- ;;
;; SDK Testing Module
;; -------------------------------------------------------------------------- ;;

(defrecord TestingModule []
  pr/SDKModule
  (start [this]
    (let [module-name :testing]
      (ih/register {:api {module-name {:throw-error throw-error}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))