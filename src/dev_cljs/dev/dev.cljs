(ns dev.dev
  (:require [devtools.core :as devtools]))

(devtools/install! [:formatters :hints])
(enable-console-print!)
