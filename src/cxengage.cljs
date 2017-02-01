(ns cxengage.sdk
  (:require [cxengage-javascript-sdk.core :as c]
            [cxengage-javascript-sdk.state :as state]))

(enable-console-print!)

(defn ^:export dump-state []
  (clj->js @(state/get-state)))

(defn ^:export init [params]
  (c/init (or params {})))
