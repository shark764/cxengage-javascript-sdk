(ns cxengage-javascript-sdk.modules.voice-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.voice :as voice]
            [cljs.core.async :as a]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs.test :refer-macros [deftest is testing async]]))

(deftest cancel-test
  (testing "the end interaction function"
    (async done
           (reset! state/sdk-state)
           (reset! p/sdk-subscriptions)
           (let [old rest/send-interrupt-request
                 tenant-id (str (id/make-random-uuid))
                 resource-id (str (id/make-random-uuid))
                 interaction-id (str (id/make-random-uuid))
                 topic (p/get-topic :cancel-dial-acknowledged)]
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
