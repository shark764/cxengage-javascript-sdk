(ns CxEngage
  (:require [cxengage-javascript-sdk.core :as c]))

(defn ^:export initialize
  "Single entry-point for the SDK. Registers the global ('CxEngage')"
  [& params]
  (c/initialize params))
