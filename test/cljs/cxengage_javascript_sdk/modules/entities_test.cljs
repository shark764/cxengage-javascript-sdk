(ns cxengage-javascript-sdk.modules.entities-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.pubsub :as p]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.api-utils :as api]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.modules.entities :as ent]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs-uuid-utils.core :as uuid]
            [cljs.test :refer-macros [deftest is testing async]]))

(def test-state {:session {:tenant-id "f5b660ef-9d64-47c9-9905-2f27a74bc14c"}})

;; -------------------------------------------------------------------------- ;;
;; Get User Tests
;; -------------------------------------------------------------------------- ;;

(def successful-get-user-response
  {:status 200
   :api-response {:result {:first-name "Asdf"
                           :last-name "Asdfferson"
                           :state "notready"
                           :skills-with-proficiency nil}}})

(deftest get-user--happy-test
  (testing "get single user function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old api/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-get-user-response [:api-response])]
                 (a/>! resp-chan successful-get-user-response)
                 (reset! st/sdk-state test-state)
                 (set! api/api-request (fn [_]
                                          resp-chan))
                 (p/subscribe "cxengage/entities/get-user-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! api/api-request old)
                                (done)))
                 (ent/get-user {:resource-id "76818798-9075-43d5-a00c-9b8ccff7b1df"}))))))


(def successful-get-users-response
  {:status 200
   :api-response {:result [{:first-name "Asdf"
                            :last-name "Asdfferson"
                            :state "notready"}
                           {:first-name "Asdf2"
                            :last-name "Asdfferson2"
                            :state "notready"}]}})

(deftest get-users--happy-test
  (testing "get all users function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old api/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-get-users-response [:api-response])]
                 (a/>! resp-chan successful-get-users-response)
                 (reset! st/sdk-state test-state)
                 (set! api/api-request (fn [_]
                                          resp-chan))
                 (p/subscribe "cxengage/entities/get-users-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! api/api-request old)
                                (done)))
                 (ent/get-users))))))

;; -------------------------------------------------------------------------- ;;
;; Get Branding Test
;; -------------------------------------------------------------------------- ;;

(deftest get-branding-test
  (testing "get the branding for a tenant"
    (async done
           (reset! p/sdk-subscriptions {})
           (let [old rest/get-branding-request
                 tenant-id (str (uuid/make-random-uuid))
                 topic (topics/get-topic :get-branding-response)]
             (st/reset-state)
             (st/set-active-tenant! tenant-id)
             (set! rest/get-branding-request (fn []
                                               (go {:status 200
                                                    :api-response {:result {:logo "unit test"
                                                                            :favicon "test favicon"
                                                                            :styles "some css"
                                                                            :tenant-id tenant-id}}})))
             (p/subscribe topic (fn [e t r]
                                  (is (= ({:result {:logo "unit test"}
                                                   :favicon "test favicon"
                                                   :styles "some css"
                                                   :tenantId tenant-id} (js->clj r :keywordize-keys true))))
                                  (set! rest/get-branding-request old)
                                  (done)))
             (ent/get-branding)))))

;; -------------------------------------------------------------------------- ;;
;; Get Queue Tests
;; -------------------------------------------------------------------------- ;;

(def successful-get-whole-queue-response
  {:result {
            :name "mock queue",
            :description "mock queue description",
            :active true,
            :versions [
                        {:name "v1",:description "first version"},
                        {:name "v2",:description "second version"}]}})

(def successful-get-queue-response
  {:status 200
    :api-response {
                    :result {
                              :name "mock queue",
                              :description "mock queue description",
                              :active true}}})

(def successful-get-queue-versions-response
  {:status 200
    :api-response {
                    :result [
                              {:name "v1",:description "first version"},
                              {:name "v2",:description "second version"}]}})

(deftest get-queue--happy-test
  (testing "get single queue function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old api/api-request
                     pubsub-expected-response successful-get-whole-queue-response]
                 (reset! st/sdk-state test-state)
                 (set! api/api-request (fn [request]
                                          (cond
                                           (= (:url request) "tenants/f5b660ef-9d64-47c9-9905-2f27a74bc14c/queues/76818798-9075-43d5-a00c-9b8ccff7b1df") (go successful-get-queue-response)
                                           (= (:url request) "tenants/f5b660ef-9d64-47c9-9905-2f27a74bc14c/queues/76818798-9075-43d5-a00c-9b8ccff7b1df/versions") (go successful-get-queue-versions-response))))
                 (p/subscribe "cxengage/entities/get-queue-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! api/api-request old)
                                (done)))
                 (ent/get-queue {:queue-id "76818798-9075-43d5-a00c-9b8ccff7b1df"}))))))


(def successful-get-queues-response
  {:status 200
   :api-response {:result [{:name "asdf queue"
                            :description "asdf queue"
                            :active true}
                           {:name "asdf queue2"
                            :description "asdf queue2"
                            :active true}]}})

(deftest get-queues--happy-test
  (testing "get all queues function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old api/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-get-queues-response [:api-response])]
                 (a/>! resp-chan successful-get-queues-response)
                 (reset! st/sdk-state test-state)
                 (set! api/api-request (fn [_]
                                          resp-chan))
                 (p/subscribe "cxengage/entities/get-queues-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! api/api-request old)
                                (done)))
                 (ent/get-queues))))))

;; -------------------------------------------------------------------------- ;;
;; Get Transfer Lists Tests
;; -------------------------------------------------------------------------- ;;

(def successful-get-transfer-list-response
  {:status 200
   :api-response {:result {:name "asdf transfer list"
                           :description "asdf transfer list"
                           :active true}}})

(deftest get-transfer-list--happy-test
  (testing "get single transfer list function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old api/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-get-transfer-list-response [:api-response])]
                 (a/>! resp-chan successful-get-transfer-list-response)
                 (reset! st/sdk-state test-state)
                 (set! api/api-request (fn [_]
                                          resp-chan))
                 (p/subscribe "cxengage/entities/get-transfer-list-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! api/api-request old)
                                (done)))
                 (ent/get-transfer-list {:transfer-list-id "76818798-9075-43d5-a00c-9b8ccff7b1df"}))))))


(def successful-get-transfer-lists-response
  {:status 200
   :api-response {:result [{:name "asdf transfer list "
                            :description "asdf transfer list "
                            :active true}
                           {:name "asdf transfer list 2"
                            :description "asdf transfer list 2"
                            :active true}]}})

(deftest get-transfer-lists--happy-test
  (testing "get all transfer lists function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old api/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-get-transfer-lists-response [:api-response])]
                 (a/>! resp-chan successful-get-transfer-lists-response)
                 (reset! st/sdk-state test-state)
                 (set! api/api-request (fn [_]
                                          resp-chan))
                 (p/subscribe "cxengage/entities/get-transfer-lists-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! api/api-request old)
                                (done)))
                 (ent/get-transfer-lists))))))

;; -------------------------------------------------------------------------- ;;
;; Update User
;; -------------------------------------------------------------------------- ;;

(def successful-update-user-response
  {:status 200
   :api-response {:result {:first-name "asdfasdf"
                           :last-name "Asdfferson"
                           :state "notready"}}})

(deftest update-user--happy-test
  (testing "get single user function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old api/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-update-user-response [:api-response])]
                 (a/>! resp-chan successful-update-user-response)
                 (reset! st/sdk-state test-state)
                 (set! api/api-request (fn [_]
                                          resp-chan))
                 (p/subscribe "cxengage/entities/update-user-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! api/api-request old)
                                (done)))
                 (ent/update-user {:user-id "76818798-9075-43d5-a00c-9b8ccff7b1df" :update-body {:first-name "asdfasdf"}}))))))

;; -------------------------------------------------------------------------- ;;
;; Fetch reporting events
;; -------------------------------------------------------------------------- ;;

(def fetch-reporting-event-success
  {:api-response {:result [{:id 42
                            :events [{:event-id "1"}
                                     {:event-id "2"}
                                     {:event-id "3"}]
                            :interaction "523940-8-342"}]}
   :status 200})

(deftest fetch-reporting-event
  (testing "get single user function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old api/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in fetch-reporting-event-success [:api-response])]
                 (a/>! resp-chan fetch-reporting-event-success)
                 (reset! st/sdk-state test-state)
                 (set! api/api-request (fn [_]
                                          resp-chan))
                 (p/subscribe "cxengage/entities/get-entity-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! api/api-request old)
                                (done)))
                 (ent/get-entity {:path ["interactions", "interaction-id", "reporting-events", "flow-state-log"]}))))))


(def files ["blahblahblah.png"])
(def artifact-id (uuid/make-random-uuid))
(def tenant-id (uuid/make-random-uuid))
(deftest get-recording-test
  (testing "the get-recording fn"
    (async
     done
     (set! rest/get-artifact-by-id-request (fn [& _]
                                             (go {:api-response {:files files}
                                                  :status 200})))
     (p/subscribe
      (topics/get-topic :get-recordings-response)
      (fn [error topic response]
        (is (= files (js->clj response :keywordize-keys true)))
        (done)))
     (ent/get-recording interaction-id tenant-id artifact-id (fn [& _] nil)))))

(deftest get-recording-error-test
  (testing "the get-recording error response"
    (async
     done
     (set! rest/get-artifact-by-id-request (fn [& _]
                                             (go not-found)))
     (p/subscribe
      (topics/get-topic :get-recordings-response)
      (fn [error topic response]
        (is (= (camels (e/failed-to-get-recordings-err interaction-id artifact-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (ent/get-recording interaction-id tenant-id artifact-id (fn [& _] nil)))))

(deftest get-recordings-test
  (testing "the get-recordings fn"
    (async
     done
     (state/set-active-tenant! tenant-id)
     (set! rest/get-artifact-by-id-request (fn [& _]
                                             (go {:api-response {:files files}
                                                  :status 200})))
     (set! rest/get-interaction-artifacts-request (fn [& _]
                                                    (go {:api-response {:results [{:artifact-type "audio-recording"}]}
                                                         :status 200})))
     (p/subscribe
      (topics/get-topic :get-recordings-response)
      (fn [error topic response]
        (is (= files (js->clj response :keywordize-keys true)))
        (done)))
     (ent/get-recordings {:interaction-id interaction-id}))))

(deftest get-recordings-none-test
  (testing "the get-recordings response for no recordings"
    (async
     done
     (set! rest/get-interaction-artifacts-request (fn [& _]
                                                    (go {:api-response {:results []}
                                                         :status 200})))
     (p/subscribe
      (topics/get-topic :get-recordings-response)
      (fn [error topic response]
        (is (= [] (js->clj response :keywordize-keys true)))
        (done)))
     (ent/get-recordings {:interaction-id interaction-id}))))
