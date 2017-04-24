(ns cxengage-javascript-sdk.modules.reporting-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.reporting :as reporting]
            [cxengage-javascript-sdk.core :as core]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs.core.async :as a]
            [cljs-uuid-utils.core :as uuid]
            [cljs.test :refer-macros [deftest is testing async use-fixtures]]))

(deftest polling-test
  (testing "the batch request polling functionality"
    (async done
           (let [the-chan (a/promise-chan)
                 old iu/api-request
                 ReportingModule (reporting/map->ReportingModule. (core/gen-new-initial-module-config (a/chan)))
                 _ (state/reset-state)
                 _ (set! iu/api-request (fn [request-map casing]
                                          (let [{:keys [requests]} request-map]
                                            (a/>! the-chan {:status 200
                                                            :api-response {:results requests}})
                                            the-chan)))
                 cb (fn [error topic response]
                      (cond
                        (and (= topic "cxengage/reporting/stat-subscription-added") error) (is (= {:code 1001 :error "Invalid arguments passed to SDK fn."} (dissoc (js->clj error :keywordize-keys true) :data)))
                        (and (= topic "cxengage/reporting/stat-subscription-removed") error) (is (= {:code 1001 :error "Invalid arguments passed to SDK fn."} (dissoc (js->clj error :keywordize-keys true) :data)))
                        (and (= topic "cxengage/reporting/stat-subscription-added") response) (is (uuid/valid-uuid? (:statId (js->clj response :keywordize-keys true))))
                        (and (= topic "cxengage/reporting/batch-response" response)) (is (map? response))
                        (and (= topic "cxengage/reporting/batch-response" error)) (is (= {:code 1002 :error "API returned an error."}))
                        (and (= topic "cxengage/reporting/stat-subscription-removed") response) (is (map? (js->clj response :keywordize-keys true)))
                        :else (do (is (true? false))
                                  (println error)
                                  (println topic)
                                  (println response))))
                 _ (p/subscribe "cxengage" cb)]
             (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (reporting/add-stat-subscription ReportingModule)))
             (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (reporting/add-stat-subscription ReportingModule {} "unit test")))
             (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (reporting/remove-stat-subscription ReportingModule)))
             (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (reporting/remove-stat-subscription ReportingModule {} "unit test")))
             (reporting/add-stat-subscription ReportingModule {:statistic "queue-abandon-count"})
             (reporting/add-stat-subscription ReportingModule {:statisitc {}})
             (reporting/add-stat-subscription ReportingModule {:statistic "hi" :queue-id "hi"})
             (reporting/add-stat-subscription ReportingModule {:statistic "hi" :queue-id (str (uuid/make-random-uuid)) :resource-id "test"})
             (reporting/remove-stat-subscription ReportingModule {:stat-id "hi"})
             (reporting/start-polling ReportingModule)
             (reporting/remove-stat-subscription ReportingModule {:stat-id (-> @(:state ReportingModule)
                                                                               (:statistics)
                                                                               (keys)
                                                                               (first))})
             (set! iu/api-request old)
             (done)))))
