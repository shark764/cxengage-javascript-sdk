(ns cxengage-javascript-sdk.next-modules.reporting-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.pubsub :as p]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.next-modules.reporting :as rep]
            [cljs.test :refer-macros [deftest is testing async use-fixtures]]))


;; -------------------------------------------------------------------------- ;;
;; Stat Query Tests
;; -------------------------------------------------------------------------- ;;

(def successful-stat-query-response
  {:status 200
   :api-response {:results {:stat-one {:status 200
                                       :body {:results {:count 1}}}}}})

(deftest stat-query--happy-test--query-response-pubsub
  (testing "stat query function success - query response pubsub"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-stat-query-response [:api-response :results])]
                 (a/>! resp-chan successful-stat-query-response)
                 (set! st/get-active-tenant-id #(str "f5b660ef-9d64-47c9-9905-2f27a74bc14c"))
                 (set! iu/api-request (fn [_]
                                        resp-chan))
                 (p/subscribe "cxengage/reporting/get-stat-query-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (rep/stat-query {:statistic "resources-logged-in-count"}))))))

(deftest stat-query--sad-test--invalid-args-error-1
  (testing "stat-query failure - wrong # of args"
    (async done
           (reset! p/sdk-subscriptions {})
           (let [pubsub-expected-response (js->clj (ih/camelify (e/wrong-number-of-sdk-fn-args-err)))]
             (p/subscribe "cxengage/reporting/get-stat-query-response"
                          (fn bar [error topic response]
                            (is (= pubsub-expected-response (js->clj error)))
                            (done)))
             (rep/stat-query {:statistic "resources-logged-in-count"
                              :resource-id "e3373880-38c7-4bd8-be10-9d70a14e12ab"}
                             (fn [] nil)
                             "this should cause the fn to throw an error")))))

(deftest stat-query--sad-test--invalid-args-error-2
  (testing "stat-query failure - didn't pass a map"
    (async done
           (reset! p/sdk-subscriptions {})
           (let [pubsub-expected-response (js->clj (ih/camelify (e/params-isnt-a-map-err)))]
             (p/subscribe "cxengage/reporting/get-stat-query-response"
                          (fn [error topic response]
                            (is (= pubsub-expected-response (js->clj error)))
                            (done)))
             (rep/stat-query "test")))))

(deftest stat-query--sad-test--invalid-args-error-3
  (testing "stat-query failure - did pass a callback, but it isnt a function"
    (async done
           (reset! p/sdk-subscriptions {})
           (let [pubsub-expected-response (js->clj (ih/camelify (e/callback-isnt-a-function-err)))]
             (p/subscribe "cxengage/reporting/get-stat-query-response"
                          (fn [error topic response]
                            (is (= pubsub-expected-response (js->clj error)))
                            (done)))
             (rep/stat-query {:statistic "resources-logged-in-count"}
                             "this should cause the fn to throw an error")))))
