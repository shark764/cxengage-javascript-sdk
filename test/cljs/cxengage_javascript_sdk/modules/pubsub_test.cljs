(ns cxengage-javascript-sdk.modules.pubsub-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cljs.test :refer-macros [deftest is testing async]]))

;; Mutable map for callbacks to write to. Used to track number of callbacks called
(def call-count (atom {}))
;; Generates a callback function that updates call-count. Optionally unsubscribes
(defn gen-callback [& unsubscribe]
  (fn [e t r subscription-id]
    (let [clj-response (ih/kebabify r)
          current-count (get @call-count subscription-id 0)]
       (swap! call-count assoc subscription-id (inc current-count))
       (if unsubscribe
         (p/unsubscribe subscription-id)))))

(deftest publish-to-multiple-subscribers-same-topic
  (testing "publish function with multiple subscribers for the same topic"
    (async done
      (reset! p/sdk-subscriptions {})
      (reset! call-count {})
      (go (let [subscription-a (p/subscribe "cxengage/entities/get-user-response" (gen-callback))
                subscription-b (p/subscribe "cxengage/entities/get-user-response" (gen-callback))
                ;; negative case - unrelated topic should not be published to
                subscription-c (p/subscribe "cxengage/capabilities" (gen-callback))]
              (p/publish {:topics "cxengage/entities/get-user-response"
                          :response {:response "a fake response"}})
              ;; Checks both callbacks were called and that subscription-id was in the responses
              (is (and (= (@call-count subscription-a) 1)
                       (= (@call-count subscription-b) 1)
                       (= (contains? @call-count subscription-c) false)))
              (done))))))

(deftest publish-to-mutiple-subscribers-related-topics
  (testing "publish function with multiple subscribers for related topics"
    (async done
      (reset! p/sdk-subscriptions {})
      (reset! call-count {})
      (go (let [subscription-a (p/subscribe "cxengage/entities/get-user-response" (gen-callback))
                subscription-b (p/subscribe "cxengage/entities" (gen-callback))
                subscription-c (p/subscribe "cxengage" (gen-callback))
                ;; negative case - unrelated topic should not be published to
                subscription-d (p/subscribe "cxengage/capabilities" (gen-callback))]
              (p/publish {:topics "cxengage/entities/get-user-response"
                          :response {:response "a fake response"}})
              (is (and (= (@call-count subscription-a) 1)
                       (= (@call-count subscription-b) 1)
                       (= (@call-count subscription-c) 1)
                       (= (contains? @call-count subscription-d) false)))
              (done))))))

(deftest publish-and-unsubscribe
  (testing "ability to unsubscribe in the callback function"
    (async done
      (reset! p/sdk-subscriptions {})
      (reset! call-count {})
      (go (let [subscription-a (p/subscribe "cxengage/entities/get-user-response" (gen-callback true))
                ;; negative case - unrelated topic should not be published to
                subscription-b (p/subscribe "cxengage/entities/get-user-response" (gen-callback))]
              (dotimes [n 5]
                (p/publish {:topics "cxengage/entities/get-user-response"
                            :response {:response "a fake response"}}))
              (is (and (= (@call-count subscription-a) 1)
                       (= (@call-count subscription-b) 5)))
              (done))))))
