(ns cxengage-javascript-sdk.modules.voice-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.voice :as voice]
            [cljs.core.async :as a]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs-sdk-utils.interop-helpers :as ih]
            [cljs-sdk-utils.api :as api]
            [cljs-sdk-utils.topics :as topics]
            [cljs-sdk-utils.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs.test :refer-macros [deftest is testing async]]))


(def resource-id (id/make-random-uuid))
(def interaction-id (id/make-random-uuid))
(def transfer-resource-id (id/make-random-uuid))
(def transfer-queue-id (id/make-random-uuid))
(def transfer-extension {:type "pstn"
                         :value "+15055555555"})
(def expected-response {:interactionId interaction-id
                        :resourceId resource-id})

(deftest hold-test
  (testing "the customer hold function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (state/set-user-identity! {:user-id resource-id})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :hold-acknowledged)
      (fn [error topic response]
        (is (= expected-response (js->clj response :keywordize-keys true)))
        (done)))
     (voice/customer-hold {:interaction-id interaction-id}))))

(deftest hold-error-test
  (testing "the customer hold function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :hold-acknowledged)
      (fn [error topic response]
        (is (=  (e/failed-to-place-customer-on-hold-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/customer-hold {:interaction-id interaction-id}))))

(deftest resume-test
  (testing "the customer resume function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :resume-acknowledged)
      (fn [error topic response]
        (is (= expected-response (js->clj response :keywordize-keys true)))
        (done)))
     (voice/customer-resume {:interaction-id interaction-id}))))

(deftest resume-error-test
  (testing "the customer resume function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :resume-acknowledged)
      (fn [error topic response]
        (is (=  (e/failed-to-resume-customer-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/customer-resume {:interaction-id interaction-id}))))

(deftest mute-test
  (testing "the customer mute function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :mute-acknowledged)
      (fn [error topic response]
        (is (= {:resourceId resource-id
                :targetResource resource-id
                :interactionId interaction-id} (js->clj response :keywordize-keys true)))
        (done)))
     (voice/mute {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest mute-error-test
  (testing "the customer mute function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :mute-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-mute-target-resource-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/mute {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest unmute-test
  (testing "the customer unmute function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :unmute-acknowledged)
      (fn [error topic response]
        (is (= {:resourceId resource-id
                :targetResource resource-id
                :interactionId interaction-id} (js->clj response :keywordize-keys true)))
        (done)))
     (voice/unmute {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest unmute-error-test
  (testing "the customer unmute function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :unmute-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-unmute-target-resource-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/unmute {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest resource-hold-test
  (testing "the resource-hold function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :resource-hold-acknowledged)
      (fn [error topic response]
        (is (= {:resourceId resource-id
                :targetResource resource-id
                :interactionId interaction-id} (js->clj response :keywordize-keys true)))
        (done)))
     (voice/resource-hold {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest resource-hold-error-test
  (testing "the resource-hold function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :resource-hold-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-place-resource-on-hold-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/resource-hold {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest resource-resume-test
  (testing "the resource-resume function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :resource-resume-acknowledged)
      (fn [error topic response]
        (is (= {:resourceId resource-id
                :targetResource resource-id
                :interactionId interaction-id} (js->clj response :keywordize-keys true)))
        (done)))
     (voice/resource-resume {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest resource-resume-error-test
  (testing "the resource-resume function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :resource-resume-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-resume-resource-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/resource-resume {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest resume-all-test
  (testing "the resume-all function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :resume-all-acknowledged)
      (fn [error topic response]
        (is (= expected-response (js->clj response :keywordize-keys true)))
        (done)))
     (voice/resume-all {:interaction-id interaction-id}))))

(deftest resume-all-error-test
  (testing "the resume-all function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :resume-all-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-resume-all-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/resume-all {:interaction-id interaction-id}))))

(deftest remove-resource-test
  (testing "the remove-resource function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :resource-removed-acknowledged)
      (fn [error topic response]
        (is (= {:resourceId resource-id
                :targetResource resource-id
                :interactionId interaction-id} (js->clj response :keywordize-keys true)))
        (done)))
     (voice/remove-resource {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest remove-resource-error-test
  (testing "the remove-resource function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :resource-removed-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-remove-resource-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/remove-resource {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest start-recording-test
  (testing "the start-recording function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :recording-start-acknowledged)
      (fn [error topic response]
        (is (= expected-response (js->clj response :keywordize-keys true)))
        (done)))
     (voice/start-recording {:interaction-id interaction-id}))))

(deftest start-recording-error-test
  (testing "the start-recording function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :recording-start-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-start-recording-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/start-recording {:interaction-id interaction-id}))))

(deftest stop-recording-test
  (testing "the stop-recording function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :recording-stop-acknowledged)
      (fn [error topic response]
        (is (= expected-response (js->clj response :keywordize-keys true)))
        (done)))
     (voice/stop-recording {:interaction-id interaction-id}))))

(deftest stop-recording-error-test
  (testing "the stop-recording function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :recording-stop-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-stop-recording-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/stop-recording {:interaction-id interaction-id}))))

(deftest transfer-to-resource-test
  (testing "the transfer-to-resource function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :customer-transfer-acknowledged)
      (fn [error topic response]
        (is (= {:interactionId interaction-id
                :resourceId resource-id
                :transferResourceId transfer-resource-id
                :transferType "cold-transfer"} (js->clj response :keywordize-keys true)))
        (done)))
     (voice/transfer-to-resource {:interaction-id interaction-id :resource-id transfer-resource-id :transfer-type "cold"}))))

(deftest transfer-to-resource-error-test
  (testing "the transfer-to-resource function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :customer-transfer-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-transfer-to-resource-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/transfer-to-resource {:interaction-id interaction-id :resource-id transfer-resource-id :transfer-type "cold"}))))

(deftest transfer-to-queue-test
  (testing "the transfer-to-queue function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :customer-transfer-acknowledged)
      (fn [error topic response]
        (is (= {:interactionId interaction-id
                :resourceId resource-id
                :transferQueueId transfer-queue-id
                :transferType "cold-transfer"} (js->clj response :keywordize-keys true)))
        (done)))
     (voice/transfer-to-queue {:interaction-id interaction-id :queue-id transfer-queue-id :transfer-type "cold"}))))

(deftest transfer-to-queue-error-test
  (testing "the transfer-to-queue function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :customer-transfer-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-transfer-to-queue-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/transfer-to-queue {:interaction-id interaction-id :queue-id transfer-queue-id :transfer-type "cold"}))))

(deftest transfer-to-extension-test
  (testing "the transfer-to-extension function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :customer-transfer-acknowledged)
      (fn [error topic response]
        (is (= {:interactionId interaction-id
                :resourceId resource-id
                :transferExtension transfer-extension
                :transferType "cold-transfer"} (js->clj response :keywordize-keys true)))
        (done)))
     (voice/transfer-to-extension {:interaction-id interaction-id :transfer-extension transfer-extension :transfer-type "cold"}))))

(deftest transfer-to-extension-error-test
  (testing "the transfer-to-extension function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :customer-transfer-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-transfer-to-extension-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/transfer-to-extension {:interaction-id interaction-id :transfer-extension transfer-extension :transfer-type "cold"}))))

(deftest cancel-resource-transfer-test
  (testing "the cancel-resource-transfer function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :cancel-transfer-acknowledged)
      (fn [error topic response]
        (is (= {:interactionId interaction-id
                :resourceId resource-id
                :transferResourceId transfer-resource-id
                :transferType "cold-transfer"} (js->clj response :keywordize-keys true)))
        (done)))
     (voice/cancel-resource-transfer {:interaction-id interaction-id :transfer-resource-id transfer-resource-id :transfer-type "cold"}))))

(deftest cancel-resource-transfer-error-test
  (testing "the cancel-resource-transfer function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (state/set-user-identity! {:user-id resource-id})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :cancel-transfer-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-cancel-resource-transfer-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/cancel-resource-transfer {:interaction-id interaction-id :transfer-resource-id transfer-resource-id :transfer-type "cold"}))))

(deftest cancel-queue-transfer-test
  (testing "the cancel-queue-transfer function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :cancel-transfer-acknowledged)
      (fn [error topic response]
        (is (= {:interactionId interaction-id
                :resourceId resource-id
                :transferQueueId transfer-queue-id
                :transferType "cold-transfer"} (js->clj response :keywordize-keys true)))
        (done)))
     (voice/cancel-queue-transfer {:interaction-id interaction-id :transfer-queue-id transfer-queue-id :transfer-type "cold"}))))

(deftest cancel-queue-transfer-error-test
  (testing "the cancel-queue-transfer function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :cancel-transfer-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-cancel-queue-transfer-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/cancel-queue-transfer {:interaction-id interaction-id :transfer-queue-id transfer-queue-id :transfer-type "cold"}))))

(deftest cancel-extension-transfer-test
  (testing "the cancel-extension-transfer function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :cancel-transfer-acknowledged)
      (fn [error topic response]
        (is (= {:interactionId interaction-id
                :resourceId resource-id
                :transferExtension transfer-extension
                :transferType "cold-transfer"} (js->clj response :keywordize-keys true)))
        (done)))
     (voice/cancel-extension-transfer {:interaction-id interaction-id :transfer-extension transfer-extension :transfer-type "cold"}))))

(deftest cancel-extension-transfer-error-test
  (testing "the cancel-extension-transfer function error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :cancel-transfer-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-cancel-extension-transfer-err) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/cancel-extension-transfer {:interaction-id interaction-id :transfer-extension transfer-extension :transfer-type "cold"}))))

(deftest cancel-test
  (testing "the end interaction function"
    (async done
           (reset! state/sdk-state)
           (reset! p/sdk-subscriptions)
           (let [old rest/send-interrupt-request
                 tenant-id (str (id/make-random-uuid))
                 resource-id (str (id/make-random-uuid))
                 interaction-id (str (id/make-random-uuid))
                 topic (topics/get-topic :cancel-dial-acknowledged)]
             (state/set-active-tenant! tenant-id)
             (state/set-user-identity! {:user-id resource-id})
             (set! rest/send-interrupt-request (fn [interaction-id interrupt-type interrupt-body]
                                                 (when (and interaction-id interrupt-type interrupt-body)
                                                   (go {:status 200}))))
             (p/subscribe topic (fn [e t r]
                                  (is (= {:interactionId interaction-id
                                          :resourceId resource-id} (js->clj r :keywordize-keys true)))
                                  (set! rest/send-interrupt-request old)
                                  (done)))
             (voice/cancel-dial {:interaction-id interaction-id})))))
