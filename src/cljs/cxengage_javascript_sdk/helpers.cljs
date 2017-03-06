(ns cxengage-javascript-sdk.helpers
  (:require [cxengage-javascript-sdk.modules.logging :refer [log*]]))

(defn log [level & data]
  (log* level data))
