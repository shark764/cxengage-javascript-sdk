(ns cxengage-javascript-sdk.modules.reporting-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.pubsub :as p]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs-sdk-utils.interop-helpers :as ih]
            [cljs-sdk-utils.api :as api]
            [cljs-sdk-utils.test :refer [camels]]
            [cljs-sdk-utils.topics :as topics]
            [cljs-sdk-utils.errors :as e]
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

(def not-found {:status 404})

(deftest stat-query--happy-test--query-response-pubsub
  (testing "stat query function success"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/batch-request (fn [& _]
                                (go successful-stat-query-response)))
     (p/subscribe
      (topics/get-topic :get-stat-query-response)
      (fn [error topic response]
        (is (= (get-in successful-stat-query-response [:api-response :results]) (js->clj response :keywordize-keys true)))
        (done)))
     (rep/stat-query {:statistic "resources-logged-in-count"}))))

(def resource-id (str (uuid/make-random-uuid)))
(def queue-id (str (uuid/make-random-uuid)))
(def mock-stat-bundle {:statistic "average-unit-test-time"
                       :resource-id resource-id
                       :queue-id queue-id})

(deftest stat-query-error-test
  (testing "stat query function success"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (reset! st/sdk-state {})
     (set! rest/batch-request (fn [& _]
                                (go not-found)))
     (p/subscribe
      (topics/get-topic :get-stat-query-response)
      (fn [error topic response]
        (is (= (camels (e/failed-to-perform-stat-query-err mock-stat-bundle not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (rep/stat-query mock-stat-bundle))))

;; -------------------------------------------------------------------------- ;;
;; Get Capacity Tests
;; -------------------------------------------------------------------------- ;;

(def successful-capacity-response
  {:status 200
   :api-response {:results {:resource-capacity []}}})

(deftest get-capacity--happy-test--tenant
  (testing "get tenant capacity function success"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-capacity-request (fn [& _]
                                       (go successful-capacity-response)))
     (p/subscribe
      (topics/get-topic :get-capacity-response)
      (fn [error topic response]
        (is (= {:resourceCapacity []} (js->clj response :keywordize-keys true)))
        (done)))
     (rep/get-capacity))))

(deftest get-capacity-error-response-tenant
  (testing "get tenant capacity function error"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-capacity-request (fn [& _]
                                       (go not-found)))
     (p/subscribe
      (topics/get-topic :get-capacity-response)
      (fn [error topic response]
        (is (= (camels (e/failed-to-get-capacity-err not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (rep/get-capacity))))

(deftest get-capacity--happy-test--resource
  (testing "get resource capacity function success"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-capacity-request (fn [& _]
                                       (go successful-capacity-response)))
     (p/subscribe
      (topics/get-topic :get-capacity-response)
      (fn [error topic response]
        (is (= {:resourceCapacity []} (js->clj response :keywordize-keys true)))
        (done)))
     (rep/get-capacity {:resource-id resource-id}))))

(deftest get-capacity-error-response-resource
  (testing "get tenant capacity function error"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (let [old-request rest/get-capacity-request]
       (set! rest/get-capacity-request (fn [& _]
                                         (go not-found)))
       (p/subscribe
        (topics/get-topic :get-capacity-response)
        (fn [error topic response]
          (is (= (camels (e/failed-to-get-capacity-err not-found)) (js->clj error :keywordize-keys true)))
          (set! rest/get-capacity-request old-request)
          (done)))
       (rep/get-capacity {:resource-id "3e5890f1-0fef-46e3-b59f-3271e3d83646"})))))

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
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-available-stats-request (fn []
                                              (go successful-available-response)))
     (p/subscribe
      (topics/get-topic :get-available-stats-response)
      (fn [error topic response]
        (is (= (camels (:api-response successful-available-response)) (js->clj response :keywordize-keys true)))
        (done)))
     (rep/get-available-stats))))

(deftest get-available-stats-error-test
  (testing "get-available-stats"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-available-stats-request (fn [] (go not-found)))
     (p/subscribe
      (topics/get-topic :get-available-stats-response)
      (fn [error topic response]
        (is (= (camels (e/failed-to-get-available-stats-err not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (rep/get-available-stats))))

;; -------------------------------------------------------------------------- ;;
;; Get Interaction Tests
;; -------------------------------------------------------------------------- ;;

(def successful-interaction-response
  {:status 200
   :api-response {:details {:direction "inbound"
                            :channel-type "voice"
                            :customer "+1234567890"}}})

(def interaction-id (str (uuid/make-random-squuid)))

(deftest get-interaction--happy-test
  (testing "get interaction function success"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-interaction-history-request (fn [& _]
                                                  (go successful-interaction-response)))
     (p/subscribe
      (topics/get-topic :get-interaction-response)
      (fn [error topic response]
        (is (= (camels (:api-response successful-interaction-response)) (js->clj response :keywordize-keys true)))
        (done)))
     (rep/get-interaction {:interaction-id interaction-id}))))

(deftest get-interaction-error-test
  (testing "get interaction function error"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-interaction-history-request (fn [& _]
                                                  (go not-found)))
     (p/subscribe
      (topics/get-topic :get-interaction-response)
      (fn [error topic response]
        (is (= (camels (e/failed-to-get-interaction-reporting-err interaction-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (rep/get-interaction {:interaction-id interaction-id}))))

;; -------------------------------------------------------------------------- ;;
;; Get Contact Interaction History Tests
;; -------------------------------------------------------------------------- ;;

(def contact-id (str (uuid/make-random-squuid)))

(def successful-contact-history-response
  {:status 200
   :api-response {:results []}})

(deftest get-contact-history--happy-test
  (testing "get contact interaction history function success"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-contact-interaction-history-request (fn [& _]
                                                          (go successful-contact-history-response)))
     (p/subscribe
      (topics/get-topic :get-contact-interaction-history-response)
      (fn [error topic response]
        (is (= (:api-response successful-contact-history-response) (js->clj response :keywordize-keys true)))
        (done)))
     (rep/get-contact-interaction-history {:contact-id contact-id}))))

(deftest get-contact-interaction-history-error-test
  (testing "get-contact-interaction-history error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-contact-interaction-history-request (fn [& _]
                                                          (go not-found)))
     (p/subscribe
      (topics/get-topic :get-contact-interaction-history-response)
      (fn [error topic response]
        (is (= (camels (e/failed-to-get-contact-interaction-history-err contact-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (rep/get-contact-interaction-history {:contact-id contact-id :page 5}))))

;; -------------------------------------------------------------------------- ;;
;; Add Statistic Subscription Tests
;; -------------------------------------------------------------------------- ;;

(def successful-batch-response
  {:status 200
   :api-response {:results {:queue-length {:name "queue-length"
                                           :type "interaction-count"
                                           :user-friendly-name "Queue Length"}}}})

(def successful-stat-subs-update
  {:statistics {"c82d912c-2034-4b9e-a92a-f175870f5d8b" {:statistic "queue-length"
                                                        :topic "cxengage/reporting/stat-subscription-added"}}})

(deftest add-stat-sub--happy-test--batch-response
  (testing "add statistic subscription - batch success"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/batch-request  (fn [& _]
                                 (go successful-batch-response)))
     (p/subscribe
      (topics/get-topic :batch-response)
      (fn [error topic response]
        (is (= (get-in successful-batch-response [:api-response :results]) (ih/kebabify response)))
        (done)))
     (rep/add-stat-subscription {:statistic "queue-length"}))))

(deftest add-stat-sub-error-test
  (testing "Add statistic subscription, batch failure"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (reset! rep/stat-subscriptions {})
     (let [old-id uuid/make-random-uuid]
       (set! uuid/make-random-uuid (fn [] "c82d912c-2034-4b9e-a92a-f175870f5d8b"))
       (set! rest/batch-request (fn [& _]
                                  (go not-found)))
       (p/subscribe
        (topics/get-topic :batch-response)
        (fn [error topic response]
          (is (= (camels (e/reporting-batch-request-failed-err (:statistics successful-stat-subs-update) not-found)) (js->clj error :keywordize-keys true)))
          (set! uuid/make-random-uuid old-id)
          (done)))
       (rep/add-stat-subscription {:statistic "queue-length"})))))

(deftest add-stat-sub--happy-test--subscription-added
  (testing "add statistic subscription - subscription success"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (let [old-id uuid/make-random-uuid]
       (set! uuid/make-random-uuid (fn [] "c82d912c-2034-4b9e-a92a-f175870f5d8b"))
       (p/subscribe
        (topics/get-topic :add-stat)
        (fn [error topic response]
          (is (= {:statId "c82d912c-2034-4b9e-a92a-f175870f5d8b"}  (js->clj response :keywordize-keys true)))
          (set! uuid/make-random-uuid old-id)
          (done))))
     (rep/add-stat-subscription {:statistic "queue-length"}))))

;; -------------------------------------------------------------------------- ;;
;; Remove Statistic Subscription Tests
;; -------------------------------------------------------------------------- ;;

(def stat-removal-mock-subscription
  {:statistics {"c82d912c-2034-4b9e-a92a-f175870f5d8b" {:statistic "queue-length"
                                                        :topic "cxengage/reporting/stat-subscription-added"}
                "7749c9c0-3979-11e7-b8fc-d0f69d796523" {:statistic "queue-time"
                                                        :topic "cxengage/reporting/stat-subscription-added"}}})

(def stat-removal-response {:7749c9c0-3979-11e7-b8fc-d0f69d796523 {:statistic "queue-time"
                                                                   :topic "cxengage/reporting/stat-subscription-added"}})

(deftest remove-stat-sub--happy-test
  (testing "add statistic subscription - subscription success"
    (async done
           (reset! p/sdk-subscriptions {})
           (reset! st/sdk-state test-state)
           (reset! rep/stat-subscriptions stat-removal-mock-subscription)
           (p/subscribe
            (topics/get-topic :remove-stat)
            (fn [error topic response]
              (is (= stat-removal-response (js->clj response :keywordize-keys true)))
              (done)))
           (rep/remove-stat-subscription {:stat-id "c82d912c-2034-4b9e-a92a-f175870f5d8b"}))))
