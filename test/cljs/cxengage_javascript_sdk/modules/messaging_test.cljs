(ns cxengage-javascript-sdk.modules.messaging-test
  (:require [cxengage-javascript-sdk.modules.messaging :as msg]
            [cljs-time.core :as time]
            [cljs-time.format :as fmt]
            [cljs.test :refer-macros [deftest is testing async use-fixtures]]))

(deftest get-host-test
  (testing "the utility functions in the messaging module."
    (let [region-name "us-east-1"
          endpoint "AOQEKEUIQJ"
          host-name (str endpoint ".iot." region-name ".amazonaws.com")]
      (is (= host-name (msg/get-host endpoint region-name))))))
