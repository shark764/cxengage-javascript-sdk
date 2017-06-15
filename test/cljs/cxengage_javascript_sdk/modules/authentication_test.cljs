(ns cxengage-javascript-sdk.modules.authentication-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.pubsub :as p]
            [cljs.core.async :as a]
            [cljs-sdk-utils.errors :as e]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cljs-sdk-utils.interop-helpers :as ih]
            [cljs-sdk-utils.api :as api]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.modules.authentication :as auth]
            [cljs.test :refer-macros [deftest is testing async use-fixtures]]))

;; -------------------------------------------------------------------------- ;;
;; Login Tests
;; -------------------------------------------------------------------------- ;;

(set! ih/publish p/publish)

(def successful-login-response
  {:status 200
   :api-response {:result {:username "foo"
                           :tenants ["baz" "bar"]}}})

(def initial-test-state
  {:authentication {}
   :user {:user-id "f5b660ef-9d64-47c9-9905-2f27a74bc14c"}
   :session {:tenant-id "f5b660ef-9d64-47c9-9905-2f27a74bc14c"
             :session-id "9718ca30-3b31-11e7-9700-b883f2d63b7b"}
   :config {:api-url "https://dev-api.cxengagelabs.net/v1/"
            :env "dev"
            :consumer-type "js"
            :log-level "debug"}
   :interactions {:pending {}
                  :active {}
                  :past {:a1d81a20-3b31-11e7-9e33-9c2ba3b906af {:channel-type "voice"
                                                                :direction "inbound"}}}
   :logs {:unsaved-logs []
          :saved-logs []
          :valid-levels [:debug :info :warn :error :fatal]}
   :time {:offset 0}})

(def expected-test-state
  {:authentication {:token nil}
   :user {:username "foo", :tenants ["baz" "bar"]}
   :session {}
   :config {:api-url "https://dev-api.cxengagelabs.net/v1/"
            :env "dev"
            :consumer-type "js"
            :log-level "debug"}
   :internal {:enabled-modules []}
   :interactions {:pending {}
                  :active {}
                  :past {}
                  :incoming {}}
   :logs {:unsaved-logs []
          :saved-logs []
          :valid-levels [:debug :info :warn :error :fatal]}
   :time {:offset 0}})

(deftest login-api--happy-test--login-response-pubsub
  (testing "login function success - login response pubsub"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old api/api-request
                     pubsub-expected-response (get-in successful-login-response [:api-response :result])]
                 (reset! st/sdk-state initial-test-state)
                 (set! api/api-request (fn [_]
                                          (go successful-login-response)))
                 (p/subscribe "cxengage/authentication/login-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (is (= (st/get-state) expected-test-state))
                                (set! api/api-request old)
                                (done)))
                 (auth/login {:username "testuser@testemail.com"
                              :password "testpassword"}))))))

(deftest login-api--happy-test--tenant-list-pubsub
  (testing "login function success - tenant list pubsub"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old api/api-request
                     pubsub-expected-response (get-in successful-login-response [:api-response :result :tenants])]
                 (set! api/api-request (fn [_]
                                          (go successful-login-response)))
                 (p/subscribe "cxengage/session/tenant-list"
                              (fn foo [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! api/api-request old)
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

;; -------------------------------------------------------------------------- ;;
;; Logout Tests
;; -------------------------------------------------------------------------- ;;

(def successful-logout-response
  {:status 200
   :api-response {:result {:state "offline"}}})

(def success-state {:session {:tenant-id "f5b660ef-9d64-47c9-9905-2f27a74bc14c"
                              :session-id "9718ca30-3b31-11e7-9700-b883f2d63b7b"}
                    :user {:user-id "f1723430-c165-11e6-86e3-e898003f3411"}
                    :interactions {:active {}}})

(deftest logout-api--happy-test
  (testing "logout function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old api/api-request
                     pubsub-expected-response (get-in successful-logout-response [:api-response :result])]
                 (reset! st/sdk-state success-state)
                 (set! api/api-request (fn [_]
                                          (go successful-logout-response)))
                 (p/subscribe "cxengage/session/state-change-request-acknowledged"
                              (fn bar [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! api/api-request old)
                                (done)))
                 (auth/logout))))))

(def fail-state {:session {:tenant-id "f5b660ef-9d64-47c9-9905-2f27a74bc14c"}
                 :interactions {:active {:a1d81a20-3b31-11e7-9e33-9c2ba3b906af {:channel-type "voice"
                                                                                :direction "inbound"}}}})

(deftest logout-api--sad-test--active-interaction-error
  (testing "logout function failure - active interaction"
    (async done
           (reset! p/sdk-subscriptions {})
           (let [pubsub-expected-response (js->clj (ih/camelify (e/active-interactions-err)))]
             (reset! st/sdk-state fail-state)
             (p/subscribe "cxengage/session/state-change-request-acknowledged"
                          (fn bar [error topic response]
                            (is (= pubsub-expected-response (js->clj error)))
                            (done)))
             (auth/logout)))))
