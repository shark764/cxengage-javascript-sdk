(ns cxengage-javascript-sdk.runner
  (:require [clojure.test :as t]
            [doo.runner :refer-macros [doo-tests]]
            [cxengage-javascript-sdk.core-test]
            [cxengage-javascript-sdk.api-auth-test]
            [cxengage-javascript-sdk.modules.contacts-test]))

(doo-tests 'cxengage-javascript-sdk.core-test
           'cxengage-javascript-sdk.modules.contacts-test
           'cxengage-javascript-sdk.api-auth-test)
