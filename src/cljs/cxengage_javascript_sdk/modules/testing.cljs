(ns cxengage-javascript-sdk.modules.testing
    (:require [cxengage-javascript-sdk.domain.interop-helpers :as ih]
              [cxengage-javascript-sdk.domain.protocols :as pr]
              [cxengage-javascript-sdk.pubsub :as p]))

;; -------------------------------;;
;; CxEngage.testing.throwError();
;; -------------------------------;;
(defn throw-error [& params]
    (throw (js/Error. "SDK Error")))

;; -------------------------------;;
;; CxEngage.testing.saveLogs({
;;   anything: {{ any value | circular reference }}
;; });
;; -------------------------------;;
(defn save-logs [& data]
  (p/save-logs {:code "josh"
                :context "josh"
                :data data
                :level "error"
                :message "#grandejosh #elpapudepapus test"}))

;; -------------------------------------------------------------------------- ;;
;; SDK Testing Module
;; -------------------------------------------------------------------------- ;;

(defrecord TestingModule []
  pr/SDKModule
  (start [this]
    (let [module-name :testing]
      (ih/register {:api
                      {module-name
                          {:throw-error throw-error
                           :save-logs save-logs}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
