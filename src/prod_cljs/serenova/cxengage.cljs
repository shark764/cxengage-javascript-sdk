(ns serenova.cxengage
  (:require [cxengage-javascript-sdk.core :as c]
            [cxengage-javascript-sdk.state :as state]))

(defn ^:export dump-state []
  (clj->js (state/get-state)))

(defn ^:export initialize
  ([] (log :error "Wrong # of arguments passed to cxengage.initialize()"))
  ([params & rest] (log :error "Wrong # of arguments passed to cxengage.initialize()"))
  ([params] (c/initialize params)))
