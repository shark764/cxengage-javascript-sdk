(ns cxengage-javascript-sdk.modules.reporting-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.pubsub :as p]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.modules.reporting :as rep]
            [cljs-uuid-utils.core :as uuid]
            [cljs.test :refer-macros [deftest is testing async]]))

(def test-state {:session {:tenant-id "f5b660ef-9d64-47c9-9905-2f27a74bc14c"}})

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
                 (reset! st/sdk-state test-state)
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
                 (reset! st/sdk-state test-state)
                 (set! iu/api-request (fn [request]
                                        (is (= (get request :url) tenant-capacity-url))
                                        resp-chan))
                 (p/subscribe "cxengage/reporting/get-capacity-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (rep/get-capacity))))))

(deftest get-capacity--happy-test--resource
  (testing "get resource capacity function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-capacity-response [:api-response :results])]
                 (a/>! resp-chan successful-capacity-response)
                 (reset! st/sdk-state test-state)
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
                 (reset! st/sdk-state test-state)
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
                 (reset! st/sdk-state test-state)
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

(def contact-history-url (str (st/get-base-api-url) "tenants/f5b660ef-9d64-47c9-9905-2f27a74bc14c/contacts/7749c9c0-3979-11e7-b8fc-d0f69d796523/interactions"))
(def paged-history-url (str (st/get-base-api-url) "tenants/f5b660ef-9d64-47c9-9905-2f27a74bc14c/contacts/7749c9c0-3979-11e7-b8fc-d0f69d796523/interactions?page=5"))

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
                 (reset! st/sdk-state test-state)
                 (set! iu/api-request (fn [request]
                                        (is (= (get request :url) contact-history-url))
                                        resp-chan))
                 (p/subscribe "cxengage/reporting/get-contact-interaction-history-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (rep/get-contact-interaction-history {:contact-id "7749c9c0-3979-11e7-b8fc-d0f69d796523"}))))))


(deftest get-contact-history--happy-test--paged
  (testing "get paged contact interaction history function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-contact-history-response [:api-response])]
                 (a/>! resp-chan successful-contact-history-response)
                 (reset! st/sdk-state test-state)
                 (set! iu/api-request (fn [request]
                                        (is (= (get request :url) paged-history-url))
                                        resp-chan))
                 (p/subscribe "cxengage/reporting/get-contact-interaction-history-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (rep/get-contact-interaction-history {:contact-id "7749c9c0-3979-11e7-b8fc-d0f69d796523" :page 5}))))))

;; -------------------------------------------------------------------------- ;;
;; Add Statistic Subscription Tests
;; -------------------------------------------------------------------------- ;;

(def successful-batch-response
  {:status 200
   :api-response {:results { :queue-length {:name "queue-length"
                                            :type "interaction-count"
                                            :user-friendly-name "Queue Length"}}}})

(def successful-stat-subs-update
  {:statistics {"c82d912c-2034-4b9e-a92a-f175870f5d8b" {:statistic "queue-length"
                                                        :topic "cxengage/reporting/stat-subscription-added"}}})

(def successful-stat-sub-response
  {:stat-id "c82d912c-2034-4b9e-a92a-f175870f5d8b"})

(def stat-subs (atom {}))

(deftest add-stat-sub--happy-test--batch-response
  (testing "add statistic subscription - batch success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-batch-response [:api-response :results])]
                 (a/>! resp-chan successful-batch-response)
                 (reset! st/sdk-state test-state)
                 (set! iu/api-request (fn [_]
                                        resp-chan))
                 (p/subscribe "cxengage/reporting/batch-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (reset! stat-subs)
                                (done)))
                 (rep/add-stat-subscription {:statistic "queue-length"}))))))

(deftest add-stat-sub--happy-test--subscription-added
  (testing "add statistic subscription - subscription success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)]
                 (a/>! resp-chan successful-stat-sub-response)
                 (set! rep/stat-subscriptions stat-subs)
                 (set! uuid/make-random-uuid #(str "c82d912c-2034-4b9e-a92a-f175870f5d8b"))
                 (reset! st/sdk-state test-state)
                 (set! iu/api-request (fn [_]
                                        resp-chan))
                 (p/subscribe "cxengage/reporting/stat-subscription-added"
                              (fn [error topic response]
                                (is (= successful-stat-sub-response (ih/kebabify response)))
                                (is (= @stat-subs successful-stat-subs-update))
                                (set! iu/api-request old)
                                (reset! stat-subs)
                                (done)))
                 (rep/add-stat-subscription {:statistic "queue-length"}))))))

;; -------------------------------------------------------------------------- ;;
;; Remove Statistic Subscription Tests
;; -------------------------------------------------------------------------- ;;

(def successful-stat-removal
  {:statistics nil})

(deftest remove-stat-sub--happy-test
  (testing "add statistic subscription - subscription success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)]
                 (a/>! resp-chan successful-stat-sub-response)
                 (set! rep/stat-subscriptions stat-subs)
                 (reset! st/sdk-state test-state)
                 (set! iu/api-request (fn [_]
                                        resp-chan))
                 (p/subscribe "cxengage/reporting/stat-subscription-removed"
                              (fn [error topic response]
                                (is (= @stat-subs successful-stat-removal))
                                (set! iu/api-request old)
                                (reset! stat-subs)
                                (done)))
                 (rep/remove-stat-subscription {:stat-id "c82d912c-2034-4b9e-a92a-f175870f5d8b"}))))))
