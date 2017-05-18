(ns cxengage-javascript-sdk.modules.logging-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.logging :as log]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs.core.async :as a]
            [cljs.test :refer-macros [deftest is testing async]]
            [cljs-uuid-utils.core :as id]))

(deftest format-request-logs
  (testing "The format request logs"
    (let [a-log {:level :debug
                 :data ["blah" "blah" "blah"]}]
      (is (= {:level "info", :message "{\"data\":\"blah blah blah\",\"originalClientLogLevel\":\"debug\"}"} (dissoc (log/format-request-logs a-log) :timestamp))))))

(deftest log*-test
  (testing "the log* function"
    (state/destroy-state)
    (log/log* :debug "Unit" "Test")
    (is (= [{:level :debug :data ["Unit" "Test"]}] (state/get-unsaved-logs)))
    (log/log* :info "Test" "Unit")
    (is (= [{:level :debug :data ["Unit" "Test"]}
            {:level :info :data ["Test" "Unit"]}] (state/get-unsaved-logs)))))

(deftest dump-logs-test
  (testing "the dump logs function"
    (async done
           (state/destroy-state)
           (p/subscribe "cxengage/logging/logs-dumped" (fn [error topic response]
                                                         (is (= [{:level "info"
                                                                  :data ["Test" "Unit"]}
                                                                 {:level "debug"
                                                                  :data ["Unit" "Test"]}] (js->clj response :keywordize-keys true)))
                                                         (done)))
           (log/log* :info "Test" "Unit")
           (log/log* :debug "Unit" "Test")
           (log/dump-logs))))

(deftest set-level-test
  (testing "the set log level function"
    (async done
           (p/subscribe "cxengage/logging/log-level-set" (fn [error topic response]
                                                           (is (= "info" (js->clj response :keywordize-keys true)))
                                                           (done)))
           (log/set-level {:level "info"}))))

(deftest save-logs-happy-test
  (testing "The save logs function happy path"
    (async done
           (state/destroy-state)
           (go (let [the-chan (a/promise-chan)
                     old iu/api-request
                     tenant-id (str (id/make-random-uuid))
                     resource-id (str (id/make-random-uuid))]
                 (state/set-active-tenant! {:tenant-id tenant-id})
                 (state/set-user-identity! {:user-id resource-id})
                 (a/>! the-chan {:status 200
                                 :api-response {:message "success"}})
                 (set! iu/api-request (fn [request-map]
                                        (let [{:keys [body]} request-map]
                                          the-chan)))
                 (p/subscribe "cxengage/logging/logs-saved" (fn [error topic response]
                                                              (is (= {:message "success"} (js->clj response :keywordize-keys true)))
                                                              (set! iu/api-request old)
                                                              (done)))
                 (log/save-logs))))))
