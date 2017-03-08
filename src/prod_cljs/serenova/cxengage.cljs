(ns serenova.cxengage
  (:require [cxengage-javascript-sdk.core :as c]
            [cxengage-javascript-sdk.state :as state]))

(defn ^:export dump-state []
  (clj->js (state/get-state)))

(defn ^:export initialize
  ([] (js/console.error "Wrong # of arguments passed to cxengage.initialize()"))
  ([params & rest] (js/console.error "Wrong # of arguments passed to cxengage.initialize()"))
  ([params] (c/initialize params)))
