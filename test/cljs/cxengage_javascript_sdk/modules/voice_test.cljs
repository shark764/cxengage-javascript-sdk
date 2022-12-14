(ns cxengage-javascript-sdk.modules.voice-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.voice :as voice]
            [cljs.core.async :as a]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.api-utils :as api]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cljs-sdk-utils.test :refer [camels]]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs.test :refer-macros [deftest is testing async use-fixtures]]))

(use-fixtures :each {:before (fn []
                               (p/destroy-subscriptions))})

(def resource-id (id/make-random-uuid))
(def interaction-id (id/make-random-uuid))
(def expected-response {:interactionId interaction-id
                        :resourceId resource-id})

(def not-found {:status 404})

(def mock-active-extension {:type "webrtc"
                            :value "blah_blah_blah"
                            :provider "twilio"
                            :description "Default Twilio extension"
                            :region "default"
                            :sdk-version "default"})

(deftest silent-monitor-test
  (testing "the silent-monitor fn"
    (async
     done
     (state/set-user-identity! {:user-id resource-id})
     (swap! state/sdk-state assoc-in [:session :config :active-extension] mock-active-extension)
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :silent-monitoring-start-acknowledged)
      (fn [error topic response]
        (is (= (camels {:interaction-id interaction-id
                        :resource-id resource-id
                        :active-extension (js->clj response :keywordize-keys true)})))
        (done)))
     (voice/silent-monitor {:interaction-id interaction-id}))))

(deftest silent-monitor-error-test
  (testing "the error response in silent-monitor function"
    (async
     done
     (set! rest/send-interrupt-request (fn [& _]
                                         (go not-found)))
     (p/subscribe
      (topics/get-topic :silent-monitoring-start-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-start-silent-monitoring interaction-id not-found)) (js->clj error :keywordize-keys true))
        (done)))
     (voice/silent-monitor {:interaction-id interaction-id}))))

(deftest hold-test
  (testing "the customer hold function"
    (async
     done
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
     (set! rest/send-interrupt-request (fn [& _]
                                         (go not-found)))
     (p/subscribe
      (topics/get-topic :hold-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-place-customer-on-hold-err interaction-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/customer-hold {:interaction-id interaction-id}))))

(deftest resume-test
  (testing "the customer resume function"
    (async
     done
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
     (set! rest/send-interrupt-request (fn [& _]
                                         (go not-found)))
     (p/subscribe
      (topics/get-topic :resume-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-resume-customer-err interaction-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/customer-resume {:interaction-id interaction-id}))))

(deftest mute-test
  (testing "the customer mute function"
    (async
     done
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
     (set! rest/send-interrupt-request (fn [& _]
                                         (go not-found)))
     (p/subscribe
      (topics/get-topic :mute-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-mute-target-resource-err interaction-id resource-id resource-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/mute {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest unmute-test
  (testing "the customer unmute function"
    (async
     done
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
     (set! rest/send-interrupt-request (fn [& _]
                                         (go not-found)))
     (p/subscribe
      (topics/get-topic :unmute-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-unmute-target-resource-err interaction-id resource-id resource-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/unmute {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest resource-hold-test
  (testing "the resource-hold function"
    (async
     done
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
     (set! rest/send-interrupt-request (fn [& _]
                                         (go not-found)))
     (p/subscribe
      (topics/get-topic :resource-hold-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-place-resource-on-hold-err interaction-id resource-id resource-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/resource-hold {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest resource-resume-test
  (testing "the resource-resume function"
    (async
     done
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
     (set! rest/send-interrupt-request (fn [& _]
                                         (go not-found)))
     (p/subscribe
      (topics/get-topic :resource-resume-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-resume-resource-err interaction-id resource-id resource-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/resource-resume {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest resume-all-test
  (testing "the resume-all function"
    (async
     done
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
     (set! rest/send-interrupt-request (fn [& _]
                                         (go not-found)))
     (p/subscribe
      (topics/get-topic :resume-all-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-resume-all-err interaction-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/resume-all {:interaction-id interaction-id}))))

(deftest remove-resource-test
  (testing "the remove-resource function"
    (async
     done
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
     (set! rest/send-interrupt-request (fn [& _]
                                         (go not-found)))
     (p/subscribe
      (topics/get-topic :resource-removed-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-remove-resource-err interaction-id resource-id resource-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/remove-resource {:interaction-id interaction-id :target-resource-id resource-id}))))

(deftest start-recording-test
  (testing "the start-recording function"
    (async
     done
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
     (set! rest/send-interrupt-request (fn [& _]
                                         (go not-found)))
     (p/subscribe
      (topics/get-topic :recording-start-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-start-recording-err interaction-id resource-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/start-recording {:interaction-id interaction-id}))))

(deftest stop-recording-test
  (testing "the stop-recording function"
    (async
     done
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
     (set! rest/send-interrupt-request (fn [& _]
                                         (go not-found)))
     (p/subscribe
      (topics/get-topic :recording-stop-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-stop-recording-err interaction-id resource-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/stop-recording {:interaction-id interaction-id}))))


(def flow-id (id/make-random-uuid))
(def flow-version (id/make-random-uuid))
(def expected-interaction-response {:interactionId interaction-id
                                    :flowId flow-id
                                    :flowVersion flow-version})
(def phone-number "+15055555555")

(deftest dial-test
  (testing "the click to dial function"
    (async
     done
     (set! rest/create-interaction-request (fn [& _]
                                             (go {:status 200
                                                  :api-response {:interaction-id interaction-id
                                                                 :flow-id flow-id
                                                                 :flow-version flow-version}})))
     (p/subscribe
      (topics/get-topic :dial-send-acknowledged)
      (fn [error topic response]
        (is (= expected-interaction-response (js->clj response :keywordize-keys true)))
        (done)))
     (voice/dial {:phone-number phone-number}))))

(deftest dial-error-test
  (testing "the dial error response"
    (async
     done
     (set! rest/create-interaction-request (fn [& _]
                                             (go not-found)))
     (p/subscribe
      (topics/get-topic :dial-send-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-perform-outbound-dial-err phone-number not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/dial {:phone-number phone-number}))))

(deftest cancel-test
  (testing "the cancel-dial function"
    (async done
           (state/destroy-state)
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

(deftest cancel-error-test
  (testing "the cancel-dial fn's error response"
    (async
     done
     (set! rest/send-interrupt-request (fn [& _]
                                         (go not-found)))
     (p/subscribe
      (topics/get-topic :cancel-dial-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-cancel-outbound-dial-err interaction-id not-found)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/cancel-dial {:interaction-id interaction-id}))))

(def digit "0")

(deftest send-digits-test
  (testing "the send-digits function"
    (async
     done
     (state/reset-state)
     (let [mock-twilio-connection (js/Object.)]
       (set! (.-sendDigits mock-twilio-connection) (fn [& _] "beep"))
       (state/set-twilio-connection mock-twilio-connection))
     (state/set-config! {:integrations [{:type "twilio"}]
                         :active-extension {:provider "twilio"}})
     (state/add-interaction! :active {:interaction-id interaction-id
                                      :channel-type "voice"})
     (p/subscribe
      (topics/get-topic :send-digits-acknowledged)
      (fn [error topic response]
        (is (= {:interactionId interaction-id
                :digitSent digit} (js->clj response :keywordize-keys true)))
        (done)))
     (voice/send-digits {:interaction-id interaction-id
                         :digit digit}))))

(deftest send-digits-no-integration-err
  (testing "the no twilio integration error response"
    (async
     done
     (state/reset-state)
     (state/set-config! {:integrations [{:type "plivo"}]
                         :active-extension {:provider "plivo"}})
     (p/subscribe
      (topics/get-topic :no-twilio-integration)
      (fn [error topic response]
        (is (= (camels (e/no-twilio-integration-err)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/send-digits {:interaction-id interaction-id
                         :digit digit}))))

(deftest send-digits-exception-test
  (testing "the send-digits exception handler"
    (async
     done
     (state/reset-state)
     (state/set-config! {:integrations [{:type "twilio"}]
                         :active-extension {:provider "twilio"}})
     (state/add-interaction! :active {:interaction-id interaction-id
                                      :channel-type "voice"})
     (p/subscribe
      (topics/get-topic :send-digits-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-send-twilio-digits-err digit)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/send-digits {:interaction-id interaction-id
                         :digit digit}))))

(deftest send-digits-error-test
  (testing "the send-digits error response"
    (async
     done
     (state/reset-state)
     (state/set-config! {:integrations [{:type "twilio"}]
                         :active-extension {:provider "twilio"}})
     (state/add-interaction! :active {:interaction-id interaction-id
                                      :channel-type "messaging"})
     (p/subscribe
      (topics/get-topic :failed-to-send-digits-invalid-interaction)
      (fn [error topic response]
        (is (= (camels (e/failed-to-send-digits-invalid-interaction-err interaction-id)) (js->clj error :keywordize-keys true)))
        (done)))
     (voice/send-digits {:interaction-id interaction-id
                         :digit digit}))))
