(ns dev.dev
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [devtools.core :as devtools]))

(js/console.info "                _      _      _\n              >(.)__ <(.)__ =(.)__\n               (___/  (___/  (___/\n\n              we are the ducklets of\n           safe code, your code will run\n            without errors, but only if\n           you say \"compile well duckos\"\n\n")

;; Enables pretty-printing of clojure data structures in your browser console
(devtools/install! [:formatters :hints :async])
(enable-console-print!)
