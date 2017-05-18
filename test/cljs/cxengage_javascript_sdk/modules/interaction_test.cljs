(ns cxengage-javascript-sdk.modules.interaction-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.interaction :as interaction]
            [cljs.core.async :as a]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
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

;; -------------------------------------------------------------------------- ;;
;; Get Note Tests
;; -------------------------------------------------------------------------- ;;

(def successful-get-note-response
  {:status 200
   :api-response {:result {:title "Asdf Note"
                           :body "asdasdasd asdasdasd"}}})

(deftest get-note--happy-test
  (testing "get single note function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     pubsub-expected-response (get-in successful-get-note-response [:api-response])]
                 (set! iu/api-request (fn [_]
                                        (go successful-get-note-response)))
                 (p/subscribe "cxengage/interactions/get-note-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (interaction/get-note {:interaction-id (str (id/make-random-uuid)) :note-id (str (id/make-random-uuid))}))))))

(def successful-get-notes-response
  {:status 200
   :api-response {:result [{:title "Asdf Note"
                            :body "asdasdasd asdasdasd"}
                           {:title "Asdf Note 2"
                            :body "wasdwasdwasdawa wasd"}]}})

(deftest get-notes--happy-test
  (testing "get all notes function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     pubsub-expected-response (get-in successful-get-notes-response [:api-response])]
                 (set! iu/api-request (fn [_]
                                        (go successful-get-notes-response)))
                 (p/subscribe "cxengage/interactions/get-notes-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (interaction/get-all-notes {:interaction-id (str (id/make-random-uuid))}))))))

;; -------------------------------------------------------------------------- ;;
;; Create Note Tests
;; -------------------------------------------------------------------------- ;;

(def successful-create-note-response
  {:status 200
   :api-response {:result {:note-id "f1723430-c165-11e6-86e3-e898003f3411"}}})

(deftest create-note--happy-test
  (testing "get all notes function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     pubsub-expected-response (get-in successful-create-note-response [:api-response])]
                 (set! iu/api-request (fn [_]
                                        (go successful-create-note-response)))
                 (p/subscribe "cxengage/interactions/create-note-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (interaction/create-note {:interaction-id (str (id/make-random-uuid)) :title "Asdf Note" :body "asdasd asdasdasd"}))))))

;; -------------------------------------------------------------------------- ;;
;; Update Note Tests
;; -------------------------------------------------------------------------- ;;

(def successful-update-note-response
  {:status 200
   :api-response {}})

(deftest update-note--happy-test
  (testing "get all notes function success"
    (async done
           (reset! p/sdk-subscriptions {})
           (go (let [old iu/api-request
                     pubsub-expected-response (get-in successful-update-note-response [:api-response])]
                 (set! iu/api-request (fn [_]
                                        (go successful-update-note-response)))
                 (p/subscribe "cxengage/interactions/update-note-response"
                              (fn [error topic response]
                                (is (= pubsub-expected-response (ih/kebabify response)))
                                (set! iu/api-request old)
                                (done)))
                 (interaction/update-note {:interaction-id (str (id/make-random-uuid))
                                           :note-id (str (id/make-random-uuid))
                                           :title "Asdf Note"
                                           :body "asdasd asdasdasd"}))))))
