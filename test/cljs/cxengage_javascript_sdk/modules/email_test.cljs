(ns cxengage-javascript-sdk.modules.email-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.email :as email]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs-sdk-utils.api :as api]
            [cljs-sdk-utils.topics :as topics]
            [cljs-sdk-utils.errors :as e]
            [cljs-sdk-utils.test :refer [camels]]
            [cljs-uuid-utils.core :as id]
            [cljs.test :refer-macros [deftest is testing async]]))

(deftest click-to-email-test-happy
  (testing "The click to email function called start-outbound-email"
    (async done
           (reset! p/sdk-subscriptions)
           (let [old rest/create-interaction-request
                 tenant-id (str (id/make-random-squuid))
                 resource-id (str (id/make-random-squuid))
                 interaction-id (str (id/make-random-squuid))
                 flow-id (str (id/make-random-squuid))
                 flow-version (str (id/make-random-squuid))]
             (state/set-active-tenant! tenant-id)
             (state/set-user-identity! {:user-id resource-id})
             (set! rest/create-interaction-request (fn [body]
                                                     (let [{:keys [source
                                                                   customer
                                                                   contact-point
                                                                   channel-type
                                                                   direction
                                                                   interaction
                                                                   metadata
                                                                   id]} body]
                                                       (when (and source customer contact-point channel-type
                                                                  direction interaction metadata id)
                                                         (go {:api-response {:interaction-id interaction-id
                                                                             :flow-id flow-id
                                                                             :flow-version flow-version}
                                                              :status 200})))))
             (p/subscribe "cxengage/interactions/email/start-outbound-email" (fn [e t r]
                                                                               (is (= {:interactionId interaction-id
                                                                                       :flowId flow-id
                                                                                       :flowVersion flow-version} (js->clj r :keywordize-keys true)))
                                                                               (set! rest/create-interaction-request old)
                                                                               (done)))
             (email/start-outbound-email {:address "unit@test.com"})))))

(deftest click-to-email-test-sad
  (testing "The click to email function called start-outbound-email"
    (async done
           (let [old rest/create-interaction-request
                 tenant-id (str (id/make-random-squuid))
                 resource-id (str (id/make-random-squuid))
                 interaction-id (str (id/make-random-squuid))
                 flow-id (str (id/make-random-squuid))
                 flow-version (str (id/make-random-squuid))]
             (state/set-active-tenant! tenant-id)
             (state/set-user-identity! {:user-id resource-id})
             (set! rest/create-interaction-request (fn [body]
                                                     (let [{:keys [source
                                                                   customer
                                                                   contact-point
                                                                   channel-type
                                                                   direction
                                                                   interaction
                                                                   metadata
                                                                   id]} body]
                                                       (when (and source customer contact-point channel-type
                                                                  direction interaction metadata id)
                                                         (go {:api-response {:bad-request "Missing some stuff, or whatever."}
                                                              :status 400})))))
             (p/subscribe "cxengage/errors/error/failed-to-create-outbound-email-interaction" (fn [e t r]
                                                                                                (is (= {:code 10002
                                                                                                        :level "error"
                                                                                                        :message "Failed to create outbound email interaction."} (js->clj e :keywordize-keys true)))
                                                                                                (set! rest/create-interaction-request old)
                                                                                                (done)))
             (email/start-outbound-email {:address "unit@test.com"})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; agent-reply-started unit tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def interaction-id (id/make-random-uuid))
(def session-id (id/make-random-uuid))
(def resource-id (id/make-random-uuid))
(def tenant-id (id/make-random-uuid))
(def channel-type "email")

(def expected-response {:interactionId interaction-id
                        :sessionId session-id
                        :resourceId resource-id
                        :tenantId tenant-id
                        :channelType channel-type})

(deftest agent-reply-started-test
  (testing "the agent-reply-started fn"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (state/reset-state)
     (state/set-user-identity! {:user-id resource-id})
     (state/set-session-details! {:session-id session-id
                                  :tenant-id tenant-id})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :agent-reply-started-acknowledged)
      (fn [error topic response]
        (is (= expected-response (js->clj response :keywordize-keys true)))
        (done)))
     (email/agent-reply-started {:interaction-id interaction-id}))))

(deftest agent-reply-started-error-test
  (testing "the agent-reply-started fn error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :agent-reply-started-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-send-agent-reply-started-err) (js->clj error :keywordize-keys true)))
        (done)))
     (email/agent-reply-started {:interaction-id interaction-id}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; agent-no-reply unit tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest agent-no-reply-test
  (testing "the agent-no-reply fn"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :agent-no-reply-acknowledged)
      (fn [error topic response]
        (is (= expected-response (js->clj response :keywordize-keys true)))
        (done)))
     (email/agent-no-reply {:interaction-id interaction-id}))))

(deftest agent-no-reply-error-test
  (testing "the agent-no-reply fn error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :agent-no-reply-acknowledged)
      (fn [error topic response]
        (is (= (e/failed-to-send-agent-no-reply-err) (js->clj error :keywordize-keys true)))
        (done)))
     (email/agent-no-reply {:interaction-id interaction-id}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; agent-cancel-reply unit tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest agent-cancel-reply-test
  (testing "the agent-cancel-reply"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 200})))
     (p/subscribe
      (topics/get-topic :agent-cancel-reply-acknowledged)
      (fn [error topic response]
        (is (= expected-response (js->clj response :keywordize-keys true)))
        (done)))
     (email/agent-cancel-reply {:interaction-id interaction-id}))))


(deftest agent-cancel-reply-error-test
  (testing "the agent-no-reply fn error response"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/send-interrupt-request (fn [& _]
                                         (go {:status 404})))
     (p/subscribe
      (topics/get-topic :agent-cancel-reply-acknowledged)
      (fn [error topic response]
        (is (= (camels (e/failed-to-send-agent-cancel-reply-err interaction-id {:status 404})) (js->clj error :keywordize-keys true)))
        (done)))
     (email/agent-cancel-reply {:interaction-id interaction-id}))))
