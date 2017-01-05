(ns client-sdk.runner
  (:require [clojure.test :as t]
            [doo.runner :refer-macros [doo-tests]]))

(doo-tests 'client-sdk.core-test)
