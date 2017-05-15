(ns cxengage-javascript-sdk.modules.session-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.session :as session]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.state :as st]
            [cljs.core.async :as a]
            [cljs.test :refer-macros [deftest is testing async]]))

(def test-state {:user {:user-id "d843a0be-bdad-4d86-bd49-162d018610fe"}
                 :session {:tenant-id "78b2805a-c3c2-4685-8705-f925b1c31bf2"
                           :session-id "186aa67e-fab9-48e3-9463-a1899ed0b86d"
                           :config {:active-extension {:value "test"}
                                    :extensions [{:type "test" :value "test2"} {:type "test2" :value "test3"}]}}})

(deftest go-ready--sad-test--invalid-extension-provided-pubsub
  (testing "go ready sad path - invalid extension provided"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (e/invalid-extension-provided-err)]
                 (a/>! resp-chan {:status 200 :api-response {:result {:nothing :nothing}}})
                 (reset! st/sdk-state test-state)
                 (set! iu/api-request (fn [_] resp-chan))
                 (p/subscribe "cxengage/session/state-change-request-acknowledged"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (js->clj error :keywordize-keys true)))
                                (set! iu/api-request old)
                                (done)))
                 (session/go-ready {:extension-value "test"}))))))

;; TODO: test failed to update user profile w new extension
;; TODO: test failed to change state
;; TODO: test happy path
