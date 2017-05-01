(ns cxengage-javascript-sdk.next-modules.authentication-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.pubsub :as p]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.next-modules.authentication :as auth]
            [cljs.test :refer-macros [deftest is testing async use-fixtures]]))

(deftest login-test
  (testing "login successful"
    (async done
           (let [expected-response {:status 200
                                    :api-response {:foo :bar}}]
             (go (let [resp-chan (a/promise-chan)]
                   (a/>! resp-chan expected-response)
                   (set! iu/api-request (fn [_]
                                          resp-chan))
                   (p/subscribe "cxengage/authentication/login-response"
                                (fn [error topic response]
                                  (println "[[[[[[[[[[[[[[[" response error topic)
                                  (is (= response expected-response))
                                  (done)))
                   (auth/login {:username "testuser@testemail.com"
                                :password "testpassword"}))))))



  #_(testing "login fails with invalid" (is (= 1 1)))
  #_(testing "login fails with invalid arguments" (is (= 1 1)))
  #_(testing "login failed (some API error)" (is (= 1 1))))
