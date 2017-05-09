(ns cxengage-javascript-sdk.next-modules.reporting-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.pubsub :as p]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.next-modules.reporting :as rep]
            [cljs.test :refer-macros [deftest is testing async use-fixtures]]))

(def successful-stat-query-response
  {:status 200
   :api-response {:results {:stat-one {:status 200
                                       :body {:results {:count 1}}}}}})

(deftest stat-query-api--happy-test--query-response-pubsub
  (testing "stat query function success - query response pubsub"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     resp-chan (a/promise-chan)
                     pubsub-expected-response (get-in successful-stat-query-response [:api-response :results])
                     _ (js/console.log "pubsub" pubsub-expected-response)]
                 (a/>! resp-chan successful-stat-query-response)
                 (set! iu/api-request (fn [_]
                                        resp-chan))
                 (p/subscribe "cxengage/reporting/get-stat-query-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (rep/stat-query {:statistic "resources-logged-in-count"} #(js/console.log "muh callback")))))))
