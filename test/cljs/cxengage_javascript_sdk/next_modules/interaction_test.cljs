(ns cxengage-javascript-sdk.next-modules.interaction-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.next-modules.interaction :as interaction]
            [cljs.core.async :as a]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as state]
            [cljs.test :refer-macros [deftest is testing async]]))

(deftest custom-interrupt-test
  (testing "the custom-interrupt fn"
    (async done
           (reset! p/sdk-subscriptions {})
           (state/reset-state)
           (go (let [old iu/api-request
                     interaction-id (str (id/make-random-uuid))
                     flow-id (str (id/make-random-uuid))
                     flow-version (str (id/make-random-uuid))
                     tenant-id (str (id/make-random-uuid))
                     the-chan (a/promise-chan)
                     cb (fn [error topic response]
                          (is (= {:interactionId interaction-id :flowId flow-id :flowVersion flow-version} (js->clj response :keywordize-keys true)))
                          (set! iu/api-request old)
                          (done))]
                 (state/set-active-tenant! tenant-id)
                 (p/subscribe "cxengage/interactions/send-custom-interrupt-acknowledged" cb)
                 (a/>! the-chan {:api-response {:interaction-id interaction-id
                                                :flow-id flow-id
                                                :flow-version flow-version}
                                 :status 200})
                 (set! iu/api-request (fn [request-map]
                                        (let [{:keys [url method body]} request-map
                                              {:keys [interrupt-body interrupt-type interaction-id]} body]
                                          the-chan)))
                 (interaction/custom-interrupt {:interrupt-type "unit-test" :interrupt-body {} :interaction-id interaction-id}))))))
