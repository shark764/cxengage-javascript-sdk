(ns cxengage.sdk
  (:require [client-sdk.core :as c]
            [client-sdk.state :as state]))

(enable-console-print!)

(defn ^:export dump-state []
  (clj->js @(state/get-state)))

(defn ^:export init [params]
  (c/init (or params {})))
