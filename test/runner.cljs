(ns cxengage-javascript-sdk.runner
  (:require [clojure.test :as t]
            [doo.runner :refer-macros [doo-tests]]))

(doo-tests 'cxengage-javascript-sdk.modules.contacts-test)
