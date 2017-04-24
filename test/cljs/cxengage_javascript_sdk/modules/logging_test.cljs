(ns cxengage-javascript-sdk.modules.logging-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.logging :as log]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.core :as core]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs.core.async :as a]
            [cljs.spec :as s]
            [cljs.test :refer-macros [deftest is testing async use-fixtures]]))

(deftest format-request-logs
  (testing "The format request logs"
    (let [a-log {:level :debug
                 :data ["blah" "blah" "blah"]}]
      (is (= {:level "info", :message "{\"data\":\"blah blah blah\",\"originalClientLogLevel\":\"debug\"}"} (dissoc (log/format-request-logs a-log) :timestamp))))))

(deftest log*-test
  (testing "the log* function"
    (let [_ (state/reset-state)
          _ (log/log* :debug "Unit" "Test")]
      (is (= [{:level :debug :data ["Unit" "Test"]}] (state/get-unsaved-logs)))
      (log/log* :info "Test" "Unit")
      (is (= [{:level :debug :data ["Unit" "Test"]}
              {:level :info :data ["Test" "Unit"]}] (state/get-unsaved-logs))))))

(deftest dump-logs-test
  (testing "the dump logs function"
    (let [logging-module (log/map->LoggingModule. (core/gen-new-initial-module-config (a/chan)))
          _ (state/reset-state)
          _ (p/subscribe "cxengage" (fn [error topic response]
                                      (cond
                                        (= topic "cxengage/logging/logs-dumped") (is (= [] (js->clj response :keywordize-keys true))))))]
      (= (is {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (log/dump-logs logging-module {} "test"))))))

(deftest set-level-test
  (testing "the set log level function"
    (let [logging-module (log/map->LoggingModule. (core/gen-new-initial-module-config (a/chan)))
          _ (state/reset-state)
          _ (set! s/explain-data (fn [& _] nil))
          _ (p/subscribe "cxengage" (fn [error topic response]
                                      (cond
                                        (= topic "cxengage/logging/log-level-set") (is (= {:code 1001 :error "Invalid arguments passed to SDK fn."} (dissoc (js->clj error :keywordize-keys true) :data))))))]
      (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (log/set-level logging-module)))
      (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (log/set-level logging-module {} "test")))
      (log/set-level logging-module {:level 1})
      (log/set-level logging-module {:level "info"})
      (is (= :info (state/get-log-level))))))

(deftest save-logs-test
  (testing "The save logs function"
    (async done
           (go (let [logging-module (log/map->LoggingModule. (core/gen-new-initial-module-config (a/chan)))
                     the-chan (a/promise-chan)
                     _ (a/>! the-chan {:status 400
                                       :api-response {:result "Unit test"}})
                     _ (state/reset-state)
                     old iu/api-request
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [method url body] :as params} request-map]
                                                the-chan)))
                     _ (p/subscribe "cxengage" (fn [error topic response]
                                                 (cond
                                                   (= topic "cxengage/logging/logs-saved") (is (= {:code 1002 :error "API returned an error."}  (js->clj error :keywordize-keys true))))))]
                 (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (log/save-logs logging-module {} "test")))
                 (log/save-logs logging-module)
                 (set! iu/api-request old)
                 (done))))))

(deftest save-logs-happy-test
  (testing "The save logs function happy path"
    (async done
           (go (let [logging-module (log/map->LoggingModule. (core/gen-new-initial-module-config (a/chan)))
                     _ (state/reset-state)
                     the-chan (a/promise-chan)
                     _ (a/>! the-chan {:status 200
                                       :api-response {:result {:message "true"}}})
                     old iu/api-request
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [body]} request-map]
                                                the-chan)))
                     _ (p/subscribe "cxengage" (fn [error topic response]
                                                 (cond
                                                   (and response (= topic "cxengage/logging/logs-saved")) (is (= {:message "true"} (js->clj response :keywordize-keys true))))))]
                 (log/save-logs logging-module)
                 (set! iu/api-request old)
                 (done))))))
