(ns client-sdk.api-auth-test
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [client-sdk.test-macros :refer [with-reset]])
  (:require [cljs.test :refer-macros [deftest is testing async]]
            [cljs.core.async :as a]
            [client-sdk.module-gateway :as mg]
            [client-sdk.api.auth :as auth]))

(deftest login-test
  (testing "Invalid params error thrown w/ bad params"
    (async
     done
     (let [bad-params
           (clj->js
            {:callback (fn [error topic response]
                         (let [{:strs [code message]} (js->clj error)]
                           (is (= code 1000))
                           (is (= message "Invalid arguments passed to SDK function."))
                           (is (= topic "cxengage/authentication/login"))
                           (is (= nil response))
                           (done)))})]
       (auth/login bad-params))))

  #_(testing "Login result returns successfully (w/ api stub & proper params)"
      (async
       done
       (let [good-params
             (clj->js
              {:username "foo"
               :password "bar"
               :callback (fn [error topic response]
                           (let [response (js->clj response)]
                             (println error)
                             (println topic)
                             (println response)
                             (is (= 1 1))
                             (done)))})]
         (auth/login good-params)))))
