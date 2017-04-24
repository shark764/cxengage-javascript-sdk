(ns cxengage-javascript-sdk.modules.session-test
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [clojure.string :as str]
                   [clojure.set :as set])
  (:require [cxengage-javascript-sdk.modules.session :as session]
            [cxengage-javascript-sdk.core :as core]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cljs-uuid-utils.core :as id]
            [cljs.test :refer-macros [deftest is testing async use-fixtures]]
            [cljs.core.async :as a]))

(deftest heartbeat-happy
  (testing "the start-heartbeats* fn"
    (async done
           (go
             (let [old iu/api-request
                   _ (reset! p/sdk-subscriptions {})
                   _ (state/reset-state)
                   the-chan (a/promise-chan)
                   SessionModule (session/map->SessionModule. (core/gen-new-initial-module-config (a/chan)))
                   session-id (str (id/make-random-uuid))
                   tenant-id (str (id/make-random-uuid))
                   resource-id (str (id/make-random-uuid))
                   _ (state/set-session-details! {:sessionId session-id
                                                  :tenant-id tenant-id
                                                  :state "ready"})
                   _ (state/set-user-identity! {:user-id resource-id})
                   _ (a/>! the-chan {:status 200
                                     :api-response {:result {:session-id session-id}}})
                   _ (set! iu/api-request (fn [request-map]
                                            (let [{:keys [url body method]} request-map]
                                              the-chan)))
                   cb (fn [error topic response]
                        (cond
                          (and (= topic (p/get-topic :presence-heartbeats-response)) response) (is (= {:sessionId session-id} (js->clj response :keywordize-keys true)))))
                   _ (p/subscribe "cxengage" cb)]
               (session/start-heartbeats* SessionModule)
               (set! iu/api-request old)
               (done))))))

(deftest heartbeart-sad
  (testing "start the heartbeats"
    (async done
           (go (let [old iu/api-request
                     _ (reset! p/sdk-subscriptions {})
                     _ (state/reset-state)
                     the-chan (a/promise-chan)
                     SessionModule (session/map->SessionModule. (core/gen-new-initial-module-config (a/chan)))
                     session-id (str (id/make-random-uuid))
                     tenant-id (str (id/make-random-uuid))
                     resource-id (str (id/make-random-uuid))
                     _ (state/set-session-details! {:sessionId session-id
                                                    :tenant-id tenant-id
                                                    :state "ready"})
                     _ (state/set-user-identity! {:user-id resource-id})
                     _ (a/>! the-chan {:status 400
                                       :api-response {:result {:session-id session-id}}})
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [url body method]} request-map]
                                                the-chan)))
                     cb (fn [error topic response]
                          (cond
                            (and (= topic (p/get-topic :presence-heartbeats-response)) error) (is (= {:code 1002 :error "API returned an error."} (js->clj error :keywordize-keys true)))))
                     _ (p/subscribe "cxengage" cb)]
                 (session/start-heartbeats* SessionModule)
                 (set! iu/api-request old)
                 (done))))))

(deftest go-not-ready
  (testing "the go-not-ready function w/ happy API response"
    (async done
           (go (let [old iu/api-request
                     _ (reset! p/sdk-subscriptions {})
                     _ (state/reset-state)
                     the-chan (a/promise-chan)
                     SessionModule (session/map->SessionModule. (core/gen-new-initial-module-config (a/chan)))
                     session-id (str (id/make-random-uuid))
                     tenant-id (str (id/make-random-uuid))
                     resource-id (str (id/make-random-uuid))
                     _ (state/set-session-details! {:sessionId session-id
                                                    :tenant-id tenant-id
                                                    :state "ready"})
                     _ (state/set-user-identity! {:user-id resource-id})
                     _ (a/>! the-chan {:status 200
                                       :api-response {:result {:session-id session-id
                                                               :state "notready"}}})
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [url body method]} request-map]
                                                the-chan)))
                     cb (fn [error topic response]
                          (let [error (dissoc (js->clj error :keywordize-keys true) :data)]
                            (cond
                              (and (= topic (p/get-topic :presence-state-change-request-acknowledged)) response) (is (= {:sessionId session-id
                                                                                                                         :state "notready"} (js->clj response :keywordize-keys true)))
                              (and (= topic (p/get-topic :presence-state-change-request-acknowledged)) error) (is (= {:code 1001 :error "Invalid arguments passed to SDK fn."} error)))))
                     _ (p/subscribe "cxengage" cb)]
                 (session/go-not-ready SessionModule)
                 (session/go-not-ready SessionModule {})
                 (session/go-not-ready SessionModule {:callback "test"})
                 (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (session/go-not-ready SessionModule {} "test")))
                 (set! iu/api-request old)
                 (done))))))

(deftest go-not-ready-with-sadness
  (testing "the go-not-ready function w/ sad api response"
    (async done
           (go (let [old iu/api-request
                     _ (reset! p/sdk-subscriptions {})
                     _ (state/reset-state)
                     the-chan (a/promise-chan)
                     SessionModule (session/map->SessionModule. (core/gen-new-initial-module-config (a/chan)))
                     session-id (str (id/make-random-uuid))
                     tenant-id (str (id/make-random-uuid))
                     resource-id (str (id/make-random-uuid))
                     _ (state/set-session-details! {:sessionId session-id
                                                    :tenant-id tenant-id
                                                    :state "ready"})
                     _ (state/set-user-identity! {:user-id resource-id})
                     _ (a/>! the-chan {:status 400
                                       :api-response {:result {:session-id session-id
                                                               :state "notready"}}})
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [url body method]} request-map]
                                                the-chan)))
                     cb (fn [error topic response]
                          (let [error (js->clj error :keywordize-keys true)]
                            (cond
                              (and (= topic (p/get-topic :presence-state-change-request-acknowledged)) error) (is (= {:code 1002 :error "API returned an error."} error)))))
                     _ (p/subscribe "cxengage" cb)]
                 (session/go-not-ready SessionModule)
                 (set! iu/api-request old)
                 (done))))))

(deftest go-offline
  (testing "the go-offline function w/ happy api response"
    (async done
           (go (let [old iu/api-request
                     _ (reset! p/sdk-subscriptions {})
                     _ (state/reset-state)
                     the-chan (a/promise-chan)
                     SessionModule (session/map->SessionModule. (core/gen-new-initial-module-config (a/chan)))
                     session-id (str (id/make-random-uuid))
                     tenant-id (str (id/make-random-uuid))
                     resource-id (str (id/make-random-uuid))
                     _ (state/set-session-details! {:sessionId session-id
                                                    :tenant-id tenant-id
                                                    :state "ready"})
                     _ (state/set-user-identity! {:user-id resource-id})
                     _ (a/>! the-chan {:status 200
                                       :api-response {:result {:session-id session-id
                                                               :state "offline"}}})
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [url body method]} request-map]
                                                the-chan)))
                     cb (fn [error topic response]
                          (let [error (dissoc (js->clj error :keywordize-keys true) :data)
                                response (js->clj response :keywordize-keys true)]
                            (cond
                              (and (= topic (p/get-topic :presence-state-change-request-acknowledged)) response) (is (= {:sessionId session-id
                                                                                                                         :state "offline"} response))
                              (and (= topic (p/get-topic :presence-state-change-request-acknowledged)) error) (is (= {:code 1001 :error "Invalid arguments passed to SDK fn."} error)))))
                     _ (p/subscribe "cxengage" cb)]
                 (session/go-offline SessionModule)
                 (session/go-offline SessionModule {})
                 (session/go-offline SessionModule {:callback "test"})
                 (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (session/go-offline SessionModule {} "test")))
                 (set! iu/api-request old)
                 (done))))))

(deftest go-offline-with-sadness
  (testing "the go-offline function w/ sad api response"
    (async done
           (go (let [old iu/api-request
                     _ (reset! p/sdk-subscriptions {})
                     _ (state/reset-state)
                     the-chan (a/promise-chan)
                     SessionModule (session/map->SessionModule. (core/gen-new-initial-module-config (a/chan)))
                     session-id (str (id/make-random-uuid))
                     tenant-id (str (id/make-random-uuid))
                     resource-id (str (id/make-random-uuid))
                     _ (state/set-session-details! {:sessionId session-id
                                                    :tenant-id tenant-id
                                                    :state "ready"})
                     _ (state/set-user-identity! {:user-id resource-id})
                     _ (a/>! the-chan {:status 400
                                       :api-response {:result {:session-id session-id
                                                               :state "notready"}}})
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [url body method]} request-map]
                                                the-chan)))
                     cb (fn [error topic response]
                          (let [error (js->clj error :keywordize-keys true)]
                            (cond
                              (and (= topic (p/get-topic :presence-state-change-request-acknowledged)) error) (is (= {:code 1002 :error "API returned an error."} error)))))
                     _ (p/subscribe "cxengage" cb)]
                 (session/go-offline SessionModule)
                 (set! iu/api-request old)
                 (done))))))

(deftest start-session-test
  (testing "the start-session function w/ happy api response"
    (async done
           (go (let [old iu/api-request
                     _ (reset! p/sdk-subscriptions {})
                     _ (state/reset-state)
                     the-chan (a/promise-chan)
                     SessionModule (session/map->SessionModule. (core/gen-new-initial-module-config (a/chan)))
                     session-id (str (id/make-random-uuid))
                     tenant-id (str (id/make-random-uuid))
                     resource-id (str (id/make-random-uuid))
                     _ (state/set-session-details! {})
                     _ (state/set-user-identity! {:user-id resource-id})
                     _ (a/>! the-chan {:status 200
                                       :api-response {:result {:session-id session-id
                                                               :state "notready"}}})
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [url body method]} request-map]
                                                (when url body method
                                                      the-chan))))
                     cb (fn [error topic response]
                          (let [response (dissoc (js->clj response :keywordize-keys true) :resourceId)]
                            (cond
                              (and (= topic (p/get-topic :session-started)) response) (is (= {:sessionId session-id
                                                                                              :state "notready"} response)))))
                     _ (p/subscribe "cxengage" cb)]
                 (session/start-session* SessionModule)
                 (set! iu/api-request old)
                 (done))))))

(deftest start-session-with-sadness
  (testing "The start-session function w/ sad api response"
    (async done
           (go (let [old iu/api-request
                     _ (reset! p/sdk-subscriptions {})
                     _ (state/reset-state)
                     the-chan (a/promise-chan)
                     SessionModule (session/map->SessionModule. (core/gen-new-initial-module-config (a/chan)))
                     session-id (str (id/make-random-uuid))
                     tenant-id (str (id/make-random-uuid))
                     resource-id (str (id/make-random-uuid))
                     _ (state/set-user-identity! {:user-id resource-id})
                     _ (a/>! the-chan {:status 400
                                       :api-response {:result {}}})
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [url body method]} request-map]
                                                the-chan)))
                     cb (fn [error topic response]
                          (let [error (js->clj error :keywordize-keys true)]
                            (cond
                              (and (= topic (p/get-topic :session-started)) error) (is (= {:code 1002 :error "API returned an error."} error)))))
                     _ (p/subscribe "cxengage" cb)]
                 (session/start-session* SessionModule)
                 (set! iu/api-request old)
                 (done))))))

(deftest get-config-test
  (testing "the start session function w/ happy api response"
    (async done
           (go (let [old iu/api-request
                     _ (reset! p/sdk-subscriptions {})
                     _ (state/reset-state)
                     the-chan (a/promise-chan)
                     SessionModule (session/map->SessionModule. (core/gen-new-initial-module-config (a/chan)))
                     session-id (str (id/make-random-uuid))
                     tenant-id (str (id/make-random-uuid))
                     resource-id (str (id/make-random-uuid))
                     _ (state/set-session-details! {:session-id session-id
                                                    :tenant-id tenant-id})
                     _ (state/set-user-identity! {:user-id resource-id})
                     _ (a/>! the-chan {:status 200
                                       :api-response {:result {:session-id session-id
                                                               :state "notready"
                                                               :active-extension {:extensions "twilio"}}}})
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [url body method]} request-map]
                                                (when url body method
                                                      the-chan))))
                     cb (fn [error topic response]
                          (let [response (dissoc (js->clj response :keywordize-keys true) :resourceId)]
                            (cond
                              (and (= topic (p/get-topic :config-response)) response) (is (= {:sessionId session-id
                                                                                              :state "notready"
                                                                                              :activeExtension {:extensions "twilio"}} response))
                              (and (= topic (p/get-topic :extension-list)) response) (is (= {:activeExtension {:extensions "twilio"}} response)))))
                     _ (p/subscribe "cxengage" cb)]
                 (session/get-config* SessionModule)
                 (set! iu/api-request old)
                 (done))))))

(deftest get-config-test-with-api-sadness
  (testing "the start session function w/ sad api response"
    (async done
           (go (let [old iu/api-request
                     _ (reset! p/sdk-subscriptions {})
                     _ (state/reset-state)
                     the-chan (a/promise-chan)
                     SessionModule (session/map->SessionModule. (core/gen-new-initial-module-config (a/chan)))
                     session-id (str (id/make-random-uuid))
                     tenant-id (str (id/make-random-uuid))
                     resource-id (str (id/make-random-uuid))
                     _ (state/set-session-details! {:session-id session-id
                                                    :tenant-id tenant-id})
                     _ (state/set-user-identity! {:user-id resource-id})
                     _ (a/>! the-chan {:status 400
                                       :api-response {:result {:session-id session-id
                                                               :state "notready"}}})
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [url body method]} request-map]
                                                (when url body method
                                                      the-chan))))
                     cb (fn [error topic response]
                          (let [error (js->clj error :keywordize-keys true)]
                            (cond
                              (and (= topic (p/get-topic :config-response)) error) (is (= {:code 1002 :error "API returned an error."} error)))))
                     _ (p/subscribe "cxengage" cb)]
                 (session/get-config* SessionModule)
                 (set! iu/api-request old)
                 (done))))))

(deftest set-direction-test
  (testing "the set direction function"
    (async done
           (go (let [old iu/api-request
                     _ (reset! p/sdk-subscriptions {})
                     _ (state/reset-state)
                     the-chan (a/promise-chan)
                     SessionModule (session/map->SessionModule. (core/gen-new-initial-module-config (a/chan)))
                     session-id (str (id/make-random-uuid))
                     tenant-id (str (id/make-random-uuid))
                     resource-id (str (id/make-random-uuid))
                     direction "inbound"
                     _ (state/set-session-details! {:session-id session-id
                                                    :tenant-id tenant-id
                                                    :state "offline"})
                     _ (state/set-user-identity! {:user-id resource-id})
                     _ (a/>! the-chan {:status 200
                                       :api-response {:result {:session-id session-id
                                                               :direction direction}}})
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [url body method]} request-map]
                                                the-chan)))
                     cb (fn [error topic response]
                          (println error)
                          (println topic)
                          (println (js->clj response :keywordize true))
                          (let [response (js->clj response :keywordize-keys true)
                                error (dissoc (js->clj error :keywordize-keys true) :data)]
                            (cond
                              (and (= topic (p/get-topic :set-direction-response)) response) (is (= {:sessionId session-id
                                                                                                     :direction direction} response))
                              (and (= topic (p/get-topic :set-direction-response)) error) (is (= {:code 1001 :error "Invalid arguments passed to SDK fn."} error)))))
                     _ (p/subscribe "cxengage" cb)]
                 (is (= (session/set-direction SessionModule) {:code 1000 :error "Incorrect number of arguments passed to SDK fn."}))
                 (is (= (session/set-direction SessionModule {} "test") {:code 1000 :error "Incorrect number of arguments passed to SDK fn."}))
                 (session/set-direction SessionModule {:direction direction})
                 #_(session/set-direction SessionModule {:direction "otherbound"})
                 (set! iu/api-request old)
                 (done))))))

(deftest set-direction-test-with-api-sadness
  (testing "the set direction function with api sadness"
    (async done
           (go (let [old iu/api-request
                     _ (reset! p/sdk-subscriptions {})
                     _ (state/reset-state)
                     the-chan (a/promise-chan)
                     SessionModule (session/map->SessionModule. (core/gen-new-initial-module-config (a/chan)))
                     session-id (str (id/make-random-uuid))
                     tenant-id (str (id/make-random-uuid))
                     resource-id (str (id/make-random-uuid))
                     direction "inbound"
                     _ (state/set-session-details! {:session-id session-id
                                                    :tenant-id tenant-id
                                                    :state "offline"})
                     _ (state/set-user-identity! {:user-id resource-id})
                     _ (a/>! the-chan {:status 400
                                       :api-response {:result {:session-id session-id
                                                               :state "notready"}}})
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [url body method]} request-map]
                                                the-chan)))
                     cb (fn [error topic response]
                          (let [error (js->clj error :keywordize-keys true)]
                            (cond
                              (and (= topic (p/get-topic :set-direction-response)) error) (is (= {:code 1002 :error "API returned an error."} error)))))
                     _ (p/subscribe "cxengage" cb)]
                 (session/set-direction SessionModule {:direction direction})
                 (set! iu/api-request old)
                 (done))))))

(deftest set-active-tenant-test
  (testing "The set active tenant function"
    (async done
           (go (let [old iu/api-request
                     old-config session/get-config*
                     _ (reset! p/sdk-subscriptions {})
                     _ (state/reset-state)
                     the-chan (a/promise-chan)
                     SessionModule (session/map->SessionModule. (core/gen-new-initial-module-config (a/chan)))
                     session-id (str (id/make-random-uuid))
                     tenant-id (str (id/make-random-uuid))
                     resource-id (str (id/make-random-uuid))
                     _ (state/set-session-details! {:session-id session-id
                                                    :state "offline"})
                     _ (state/set-user-identity! {:user-id resource-id
                                                  :tenants [{:tenant-id tenant-id
                                                             :tenant-permissions ["CONTACTS_CREATE"
                                                                                  "CONTACTS_UPDATE"
                                                                                  "CONTACTS_READ"
                                                                                  "CONTACTS_ATTRIBUTES_READ"
                                                                                  "CONTACTS_LAYOUTS_READ"
                                                                                  "CONTACTS_ASSIGN_INTERACTION"
                                                                                  "CONTACTS_INTERACTION_HISTORY_READ"
                                                                                  "ARTIFACTS_CREATE_ALL"]}]})
                     _ (a/>! the-chan {:status 200
                                       :api-response {:result {:session-id session-id
                                                               :state "notready"}}})
                     _ (set! iu/api-request (fn [request-map]
                                              (let [{:keys [url body method]} request-map]
                                                the-chan)))
                     _ (set! session/get-config* (fn [params]
                                                   (println (str "got " params " !"))))
                     cb (fn [error topic response]
                          (let [response (js->clj response :keywordize-keys true)
                                error (dissoc (js->clj error :keywordize-keys true) :data)]
                            (cond
                              (and (= topic (p/get-topic :active-tenant-set)) response) (is (= {:tenantId tenant-id} response))
                              (and (= topic (p/get-topic :active-tenant-set)) error) (is (= {:code 1003 :error "Missing required permissions"} error)))))
                     _ (p/subscribe "cxengage" cb)]
                 (session/set-active-tenant SessionModule {:tenant-id tenant-id})
                 (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (session/set-active-tenant SessionModule)))
                 (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (session/set-active-tenant SessionModule {} "test")))
                 (state/set-user-identity! {:user-id resource-id
                                            :tenants [{:tenant-id tenant-id
                                                       :tenant-permissions ["CONTACTS_CREATE"
                                                                            "CONTACTS_UPDATE"
                                                                            "CONTACTS_READ"
                                                                            "CONTACTS_ATTRIBUTES_READ"
                                                                            "CONTACTS_LAYOUTS_READ"
                                                                            "CONTACTS_ASSIGN_INTERACTION"
                                                                            "CONTACTS_INTERACTION_HISTORY_READ"]}]})
                 (session/set-active-tenant SessionModule {:tenant-id tenant-id})
                 (set! iu/api-request old)
                 (set! session/get-config* old-config)
                 (done))))))
