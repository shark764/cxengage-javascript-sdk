(ns cxengage-javascript-sdk.modules.messaging-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.messaging :as msg]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.core :as core]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as state]
            [cljs-time.core :as time]
            [cljs-time.format :as fmt]
            [cljs-uuid-utils.core :as id]
            [cljs.test :refer-macros [deftest is testing async use-fixtures]]
            [cljs.core.async :as a]
            [clojure.string :as str]))

(deftest get-host-test
  (testing "the utility functions in the messaging module."
    (let [region-name "us-east-1"
          endpoint "AOQEKEUIQJ"
          host-name (str endpoint ".iot." region-name ".amazonaws.com")]
      (is (= host-name (msg/get-host endpoint region-name))))))

(deftest send-sms-by-interrupt-test
  (testing "the send sms by interrupt fn"
    (async done
           (go
             (let [old iu/api-request
                   _ (reset! p/sdk-subscriptions {})
                   _ (state/reset-state)
                   the-chan (a/promise-chan)
                   MessagingModule (msg/map->MessagingModule. (core/gen-new-initial-module-config (a/chan)))
                   tenant-id (str (id/make-random-uuid))
                   resource-id (str (id/make-random-uuid))
                   interaction-id (str (id/make-random-uuid))
                   _ (state/set-active-tenant! tenant-id)
                   _ (state/set-user-identity! {:user-id resource-id})
                   _ (a/>! the-chan {:status 200
                                     :api-response nil})
                   _ (set! iu/api-request (fn [request-map]
                                            (let [{:keys [url method body]} request-map]
                                              (when (and url method body)
                                                the-chan))))
                   cb (fn [error topic response]
                        (println error)
                        (println topic)
                        (println response)
                        (cond
                          (and (= topic (p/get-topic :send-sms-by-interrupt)) response) (is (= {:interactionId interaction-id} (js->clj response :keywordize-keys true)))))
                   _ (p/subscribe "cxengage" cb)]
               (msg/send-sms-by-interrupt MessagingModule {:message "Unit test" :interaction-id interaction-id})
               (msg/send-sms-by-interrupt MessagingModule {:message "Unit test" :interaction-id interaction-id :callback cb})
               (msg/send-sms-by-interrupt MessagingModule {:message "Unit test" :interaction-id interaction-id} cb)
               (set! iu/api-request old)
               (done))))))

(deftest click-to-sms-test
  (testing "the send outbound sms fn"
    (async done
           (go
             (let [old iu/api-request
                   _ (reset! p/sdk-subscriptions {})
                   _ (state/reset-state)
                   the-chan (a/promise-chan)
                   MessagingModule (msg/map->MessagingModule. (core/gen-new-initial-module-config (a/chan)))
                   tenant-id (str (id/make-random-uuid))
                   resource-id (str (id/make-random-uuid))
                   flow-id (str (id/make-random-uuid))
                   flow-version (str (id/make-random-uuid))
                   interaction-id (str (id/make-random-uuid))
                   _ (state/set-active-tenant! tenant-id)
                   _ (state/set-user-identity! {:user-id resource-id})
                   _ (a/>! the-chan {:status 200
                                     :api-response {:flow-id flow-id
                                                    :flow-version flow-version
                                                    :interaction-id interaction-id}})
                   _ (set! iu/api-request (fn [request-map]
                                            (let [{:keys [url method body]} request-map]
                                              (when (and url method body)
                                                the-chan))))
                   cb (fn [error topic response]
                        (println error)
                        (println topic)
                        (let [response (js->clj response :keywordize-keys true)]
                          (cond
                            (and (= topic (p/get-topic :click-to-sms-response)) response) (is (= {:flowId flow-id
                                                                                                  :flowVersion flow-version
                                                                                                  :interactionId interaction-id} response))))
                        )
                   _ (p/subscribe "cxengage" cb)]
               (msg/click-to-sms MessagingModule {:phone-number "15065555555" :message "This is a unit test"})
               (msg/click-to-sms MessagingModule {:phone-number "15069999999" :message "This is also a unit test" :callback cb})
               (msg/click-to-sms MessagingModule {:phone-number "15069999999" :message "This is also a unit test"} cb)
               (set! iu/api-request old)
               (done))))))
