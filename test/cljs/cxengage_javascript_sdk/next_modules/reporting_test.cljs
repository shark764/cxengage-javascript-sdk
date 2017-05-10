(ns cxengage-javascript-sdk.next-modules.reporting-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.pubsub :as p]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.next-modules.reporting :as rep]
            [cljs.test :refer-macros [deftest is testing async use-fixtures]]
            [cljs-uuid-utils.core :as uuid]))

;; -------------------------------------------------------------------------- ;;
;; Stat Query Tests
;; -------------------------------------------------------------------------- ;;

(def successful-stat-query-response
  {:status 200
   :api-response {:results {:stat-one {:status 200
                                       :body {:results {:count 1}}}}}})

(deftest stat-query--happy-test--query-response-pubsub
  (testing "stat query function success"
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

;; -------------------------------------------------------------------------- ;;
;; Get Capacity Tests
;; -------------------------------------------------------------------------- ;;

(def successful-capacity-response
  {:status 200
   :api-response {:results {:resource-capacity []}}})

(def tenant-capacity-url (str (st/get-base-api-url) "tenants/f5b660ef-9d64-47c9-9905-2f27a74bc14c/realtime-statistics/resource-capacity"))
(def resource-capacity-url (str (st/get-base-api-url) "tenants/f5b660ef-9d64-47c9-9905-2f27a74bc14c/users/3e5890f1-0fef-46e3-b59f-3271e3d83646/realtime-statistics/resource-capacity"))

(deftest get-capacity--happy-test--tenant
  (testing "get tenant capacity function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-capacity-response [:api-response :results])]
                 (a/>! resp-chan successful-capacity-response)
                 (set! st/get-active-tenant-id #(str "f5b660ef-9d64-47c9-9905-2f27a74bc14c"))
                 (set! iu/api-request (fn [request]
                                        (is (= (get request :url) tenant-capacity-url))
                                        resp-chan))
                 (p/subscribe "cxengage/reporting/get-capacity-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (rep/get-capacity))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest get-capacity--happy-test--resource
  (testing "get resource capacity function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-capacity-response [:api-response :results])]
                 (a/>! resp-chan successful-capacity-response)
                 (set! st/get-active-tenant-id #(str "f5b660ef-9d64-47c9-9905-2f27a74bc14c"))
                 (set! iu/api-request (fn [request]
                                        (is (= (get request :url) resource-capacity-url))
                                        resp-chan))
                 (p/subscribe "cxengage/reporting/get-capacity-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (rep/get-capacity {:resource-id "3e5890f1-0fef-46e3-b59f-3271e3d83646"}))))))

;; -------------------------------------------------------------------------- ;;
;; Get Available Stats Tests
;; -------------------------------------------------------------------------- ;;

(def successful-available-response
  {:status 200
   :api-response {:queue-length {:name "queue-length"
                                 :type "interaction-count"
                                 :user-friendly-name "Queue Length"}}})

(deftest get-available-stats--happy-test
  (testing "get available stats function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-available-response [:api-response])]
                 (a/>! resp-chan successful-available-response)
                 (set! st/get-active-tenant-id #(str "f5b660ef-9d64-47c9-9905-2f27a74bc14c"))
                 (set! iu/api-request (fn [_]
                                        resp-chan))
                 (p/subscribe "cxengage/reporting/get-available-stats-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (rep/get-available-stats))))))

;; -------------------------------------------------------------------------- ;;
;; Get Interaction Tests
;; -------------------------------------------------------------------------- ;;

(def successful-interaction-response
  {:status 200
   :api-response {:details {:direction "inbound"
                            :channel-type "voice"
                            :customer "+1234567890"}}})

(deftest get-interaction--happy-test
  (testing "get interaction function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-interaction-response [:api-response])]
                 (a/>! resp-chan successful-interaction-response)
                 (set! st/get-active-tenant-id #(str "f5b660ef-9d64-47c9-9905-2f27a74bc14c"))
                 (set! iu/api-request (fn [_]
                                        resp-chan))
                 (p/subscribe "cxengage/reporting/get-interaction-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (rep/get-interaction {:interaction-id "2937ac8b-380d-472b-9b9e-599097ee8c0d"}))))))

;; -------------------------------------------------------------------------- ;;
;; Get Contact Interaction History Tests
;; -------------------------------------------------------------------------- ;;

(def successful-contact-history-response
  {:status 200
   :api-response {:results []}})

(deftest get-contact-history--happy-test
  (testing "get contact interaction history function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-contact-history-response [:api-response])]
                 (a/>! resp-chan successful-contact-history-response)
                 (set! st/get-active-tenant-id #(str "f5b660ef-9d64-47c9-9905-2f27a74bc14c"))
                 (set! iu/api-request (fn [_]
                                        resp-chan))
                 (p/subscribe "cxengage/reporting/get-contact-interaction-history-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (rep/get-contact-interaction-history {:interaction-id "2937ac8b-380d-472b-9b9e-599097ee8c0d"}))))))
