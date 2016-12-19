(ns client-sdk.presence
  (:require [cljs.core.async :as a]))

(def topics {:resource-state-change (a/chan)})

(def apis {})
