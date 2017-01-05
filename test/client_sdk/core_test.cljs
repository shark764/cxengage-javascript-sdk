(ns client-sdk.core-test
  (:require [clojure.test :as t]
            [cljs.test :refer-macros [deftest is testing run-tests use-fixtures async]]))

(deftest sample-test
  (testing "sample test"
    (is (= 1 1))
    (is (= 1 1))))
