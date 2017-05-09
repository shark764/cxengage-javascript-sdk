(ns cxengage-javascript-sdk.next-modules.authentication-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.pubsub :as p]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.next-modules.authentication :as auth]
            [cljs.test :refer-macros [deftest is testing async use-fixtures]]))

(def successful-login-response
  {:status 200
   :api-response {:result {:username "foo"
                           :tenants ["baz" "bar"]}}})

(deftest login-api--happy-test--login-response-pubsub
  (testing "login function success - login response pubsub"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-login-response [:api-response :result])]
                 (a/>! resp-chan successful-login-response)
                 (set! iu/api-request (fn [_]
                                        resp-chan))
                 (p/subscribe "cxengage/authentication/login-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (auth/login {:username "testuser@testemail.com"
                              :password "testpassword"}))))))

(deftest login-api--happy-test--tenant-list-pubsub
  (testing "login function success - tenant list pubsub"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-login-response [:api-response :result :tenants])]
                 (a/>! resp-chan successful-login-response)
                 (set! iu/api-request (fn [_]
                                        resp-chan))
                 (p/subscribe "cxengage/session/tenant-list"
                              (fn foo [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (auth/login {:username "testuser@testemail.com"
                              :password "testpassword"}))))))

(deftest login-api--sad-test--invalid-args-error-1
  (testing "login function failure - wrong # of args"
    (async done
           (reset! p/sdk-subscriptions {})
           (let [pubsub-expected-response (js->clj (ih/camelify (e/wrong-number-of-sdk-fn-args-err)))]
             (p/subscribe "cxengage/authentication/login-response"
                          (fn bar [error topic response]
                            (is (= pubsub-expected-response (js->clj error)))
                            (done)))
             (auth/login {:username "testyoyoyoy"
                          :password "oyoyoyoy"}
                         (fn [] nil)
                         "this should cause the fn to throw an error")))))

(deftest login-api--sad-test--invalid-args-error-2
  (testing "login function failure - didn't pass a map"
    (async done
           (reset! p/sdk-subscriptions {})
           (let [pubsub-expected-response (js->clj (ih/camelify (e/params-isnt-a-map-err)))]
             (p/subscribe "cxengage/authentication/login-response"
                          (fn [error topic response]
                            (is (= pubsub-expected-response (js->clj error)))
                            (done)))
             (auth/login "test")))))

(deftest login-api--sad-test--invalid-args-error-3
  (testing "login function failure - did pass a callback, but it isnt a function"
    (async done
           (reset! p/sdk-subscriptions {})
           (let [pubsub-expected-response (js->clj (ih/camelify (e/callback-isnt-a-function-err)))]
             (p/subscribe "cxengage/authentication/login-response"
                          (fn [error topic response]
                            (is (= pubsub-expected-response (js->clj error)))
                            (done)))
             (auth/login {:username "testyoyoyoy"
                          :password "oyoyoyoy"}
                         "this should cause the fn to throw an error")))))
