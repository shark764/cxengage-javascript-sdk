(ns cxengage-javascript-sdk.modules.messaging-test
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [clojure.string :as str])
  (:require [cxengage-javascript-sdk.modules.messaging :as messaging]
            [cljs-uuid-utils.core :as uuid]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.state :as state]
            [cljs.test :refer-macros [deftest is testing run-tests async]]))

(def mock-env :test)
(def mock-tenant-id (str (uuid/make-random-squuid)))
(def mock-user-id (str (uuid/make-random-squuid)))
(def mock-interaction-id (str (uuid/make-random-squuid)))
(def mock-interaction {:interaction-id mock-interaction-id
                       :tenant-id mock-tenant-id
                       :channel-type "Test"})

(defn mock-send-action-impl [_ _ action-topic callback]
  (let [pubsub-topic (topics/get-topic action-topic)]
    (println pubsub-topic)
    (go (p/publish {:topics pubsub-topic
                    :response true
                    :callback callback}))))

(state/set-active-tenant! mock-tenant-id)
(state/add-interaction! :pending mock-interaction)
(state/set-env! mock-env)

(deftest mark-as-seen-test
  (testing "test successful mark-seen function"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! messaging/send-action-impl mock-send-action-impl)
     (p/subscribe
      (topics/get-topic :mark-as-seen)
      (fn [error topic response]
        (is (= response true))
        (done)))
     (messaging/mark-as-seen {:interaction-id mock-interaction-id}))))

(deftest set-typing-indicator-test
  (testing "test successful set-typing-indicator enable"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! messaging/send-action-impl mock-send-action-impl)
     (p/subscribe
      (topics/get-topic :set-typing-indicator)
      (fn [error topic response]
        (is (= response true))
        (done)))
     (messaging/set-typing-indicator {:interaction-id mock-interaction-id :enable-indicator true})))
  (testing "test successful set-typing-indicator disable"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! messaging/send-action-impl mock-send-action-impl)
     (p/subscribe
      (topics/get-topic :set-typing-indicator)
      (fn [error topic response]
        (is (= response true))
        (done)))
     (messaging/set-typing-indicator {:interaction-id mock-interaction-id :enable-indicator false}))))

