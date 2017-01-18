(ns client-sdk.runner
  (:require [clojure.test :as t]
            [doo.runner :refer-macros [doo-tests]]
            [client-sdk.core-test]
            [client-sdk.api-auth-test]
            [client-sdk.modules.contacts-test]))

(doo-tests 'client-sdk.core-test
           'client-sdk.modules.contacts-test
           'client-sdk.api-auth-test)
