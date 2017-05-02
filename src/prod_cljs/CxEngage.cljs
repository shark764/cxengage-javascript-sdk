(ns CxEngage
  (:require [cxengage-javascript-sdk.core :as c]
            [cxengage-javascript-sdk.state :as state]))

(defn ^:export initialize
  ([& params] (c/initialize params)))
