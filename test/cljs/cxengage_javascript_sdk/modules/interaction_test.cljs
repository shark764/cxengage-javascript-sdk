(ns cxengage-javascript-sdk.modules.interaction-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.interaction :as interaction]
            [cljs.core.async :as a]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs.test :refer-macros [deftest is testing async]]))

(deftest end-test
  (testing "the end interaction function"
    (async done
           (reset! state/sdk-state)
           (reset! p/sdk-subscriptions)
           (let [old rest/send-interrupt-request
                 tenant-id (str (id/make-random-uuid))
                 resource-id (str (id/make-random-uuid))
                 interaction-id (str (id/make-random-uuid))
                 topic (p/get-topic :interaction-end-acknowledged)]
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
             (interaction/end {:interaction-id interaction-id})))))

(deftest accept-test
  (testing "the accept interaction function"
    (async done
           (reset! state/sdk-state)
           (reset! p/sdk-subscriptions)
           (let [old rest/send-interrupt-request
                 topic (p/get-topic :interaction-accept-acknowledged)
                 tenant-id (str (id/make-random-uuid))
                 resource-id (str (id/make-random-uuid))
                 interaction-id (str (id/make-random-uuid))
                 sub-id (str (id/make-random-uuid))
                 action-id (str (id/make-random-uuid))
                 role-id (str (id/make-random-uuid))
                 session-id (str (id/make-random-uuid))
                 work-offer-id (str (id/make-random-uuid))
                 resource {:extension "twilio"
                           :role-id role-id
                           :session-id session-id
                           :work-offer-id work-offer-id}
                 interaction {:interaction-id interaction-id
                              :sub-id sub-id
                              :action-id action-id
                              :resource-id resource-id
                              :tenant-id tenant-id
                              :direction "inbound"
                              :resource resource
                              :timeout 30
                              :channel-type "Test"}]
             (state/set-active-tenant! tenant-id)
             (state/set-user-identity! {:user-id resource-id})
             (state/add-interaction! :pending interaction)
             (set! rest/send-interrupt-request (fn [interaction-id interrupt-type interrupt-body]
                                                 (when (and interaction-id interrupt-type interrupt-body)
                                                   (go {:status 200}))))
             (p/subscribe topic (fn [e t r]
                                  (is (= {:interactionId interaction-id
                                          :resourceId resource-id} (js->clj r :keywordize-keys true)))
                                  (set! rest/send-interrupt-request old)
                                  (done)))
             (interaction/accept {:interaction-id interaction-id})))))

(deftest focus-test
  (testing "the focus interaction function"
    (async done
           (reset! state/sdk-state)
           (reset! p/sdk-subscriptions)
           (let [old rest/send-interrupt-request
                 tenant-id (str (id/make-random-uuid))
                 resource-id (str (id/make-random-uuid))
                 interaction-id (str (id/make-random-uuid))
                 sub-id (str (id/make-random-uuid))
                 action-id (str (id/make-random-uuid))
                 role-id (str (id/make-random-uuid))
                 session-id (str (id/make-random-uuid))
                 work-offer-id (str (id/make-random-uuid))
                 resource {:extension "twilio"
                           :role-id role-id
                           :session-id session-id
                           :work-offer-id work-offer-id}
                 interaction {:interaction-id interaction-id
                              :sub-id sub-id
                              :action-id action-id
                              :resource-id resource-id
                              :tenant-id tenant-id
                              :direction "inbound"
                              :resource resource
                              :timeout 30
                              :channel-type "Test"}
                 topic (p/get-topic :interaction-focus-acknowledged)]
             (state/set-active-tenant! tenant-id)
             (state/set-user-identity! {:user-id resource-id})
             (state/add-interaction! :active interaction)
             (set! rest/send-interrupt-request (fn [interaction-id interrupt-type interrupt-body]
                                                 (when (and interaction-id interrupt-type interrupt-body)
                                                   (go {:status 200}))))
             (p/subscribe topic (fn [e t r]
                                  (is (= {:interactionId interaction-id
                                          :subId sub-id
                                          :actionId action-id
                                          :resourceId resource-id
                                          :tenantId tenant-id
                                          :sessionId session-id
                                          :workOfferId work-offer-id
                                          :direction "inbound"
                                          :channelType "Test"} (js->clj r :keywordize-keys true)))
                                  (set! rest/send-interrupt-request old)
                                  (done)))
             (interaction/focus {:interaction-id interaction-id})))))

(deftest unfocus-test
  (testing "the unfocus interaction function"
    (async done
           (reset! state/sdk-state)
           (reset! p/sdk-subscriptions)
           (let [old rest/send-interrupt-request
                 tenant-id (str (id/make-random-uuid))
                 resource-id (str (id/make-random-uuid))
                 interaction-id (str (id/make-random-uuid))
                 sub-id (str (id/make-random-uuid))
                 action-id (str (id/make-random-uuid))
                 role-id (str (id/make-random-uuid))
                 session-id (str (id/make-random-uuid))
                 work-offer-id (str (id/make-random-uuid))
                 resource {:extension "twilio"
                           :role-id role-id
                           :session-id session-id
                           :work-offer-id work-offer-id}
                 interaction {:interaction-id interaction-id
                              :sub-id sub-id
                              :action-id action-id
                              :resource-id resource-id
                              :tenant-id tenant-id
                              :direction "inbound"
                              :resource resource
                              :timeout 30
                              :channel-type "Test"}
                 topic (p/get-topic :interaction-unfocus-acknowledged)]
             (state/set-active-tenant! tenant-id)
             (state/set-user-identity! {:user-id resource-id})
             (state/add-interaction! :active interaction)
             (set! rest/send-interrupt-request (fn [interaction-id interrupt-type interrupt-body]
                                                 (when (and interaction-id interrupt-type interrupt-body)
                                                   (go {:status 200}))))
             (p/subscribe topic (fn [e t r]
                                  (is (= {:interactionId interaction-id
                                          :subId sub-id
                                          :actionId action-id
                                          :resourceId resource-id
                                          :tenantId tenant-id
                                          :sessionId session-id
                                          :workOfferId work-offer-id
                                          :direction "inbound"
                                          :channelType "Test"} (js->clj r :keywordize-keys true)))
                                  (set! rest/send-interrupt-request old)
                                  (done)))
             (interaction/unfocus {:interaction-id interaction-id})))))

(deftest assign-test
  (testing "the assign contact to interaction function"
    (async done
           (reset! state/sdk-state)
           (reset! p/sdk-subscriptions)
           (let [old rest/send-interrupt-request
                 tenant-id (str (id/make-random-uuid))
                 resource-id (str (id/make-random-uuid))
                 interaction-id (str (id/make-random-uuid))
                 sub-id (str (id/make-random-uuid))
                 action-id (str (id/make-random-uuid))
                 role-id (str (id/make-random-uuid))
                 session-id (str (id/make-random-uuid))
                 work-offer-id (str (id/make-random-uuid))
                 contact-id (str (id/make-random-uuid))
                 resource {:extension "twilio"
                           :role-id role-id
                           :session-id session-id
                           :work-offer-id work-offer-id}
                 interaction {:interaction-id interaction-id
                              :sub-id sub-id
                              :action-id action-id
                              :resource-id resource-id
                              :tenant-id tenant-id
                              :direction "inbound"
                              :resource resource
                              :timeout 30
                              :channel-type "Test"}
                 topic (p/get-topic :contact-assignment-acknowledged)]
             (state/set-active-tenant! tenant-id)
             (state/set-user-identity! {:user-id resource-id})
             (state/add-interaction! :active interaction)
             (set! rest/send-interrupt-request (fn [interaction-id interrupt-type interrupt-body]
                                                 (when (and interaction-id interrupt-type interrupt-body)
                                                   (go {:status 200}))))
             (p/subscribe topic (fn [e t r]
                                  (is (= {:interactionId interaction-id
                                          :subId sub-id
                                          :actionId action-id
                                          :resourceId resource-id
                                          :tenantId tenant-id
                                          :sessionId session-id
                                          :workOfferId work-offer-id
                                          :direction "inbound"
                                          :channelType "Test"
                                          :contactId contact-id} (js->clj r :keywordize-keys true)))
                                  (set! rest/send-interrupt-request old)
                                  (done)))
             (interaction/assign {:contact-id contact-id :interaction-id interaction-id})))))

(deftest unassign-test
  (testing "the assign contact to interaction function"
    (async done
           (reset! state/sdk-state)
           (reset! p/sdk-subscriptions)
           (let [old rest/send-interrupt-request
                 tenant-id (str (id/make-random-uuid))
                 resource-id (str (id/make-random-uuid))
                 interaction-id (str (id/make-random-uuid))
                 sub-id (str (id/make-random-uuid))
                 action-id (str (id/make-random-uuid))
                 role-id (str (id/make-random-uuid))
                 session-id (str (id/make-random-uuid))
                 work-offer-id (str (id/make-random-uuid))
                 contact-id (str (id/make-random-uuid))
                 resource {:extension "twilio"
                           :role-id role-id
                           :session-id session-id
                           :work-offer-id work-offer-id}
                 interaction {:interaction-id interaction-id
                              :sub-id sub-id
                              :action-id action-id
                              :resource-id resource-id
                              :tenant-id tenant-id
                              :direction "inbound"
                              :resource resource
                              :timeout 30
                              :channel-type "Test"}
                 topic (p/get-topic :contact-unassignment-acknowledged)]
             (state/set-active-tenant! tenant-id)
             (state/set-user-identity! {:user-id resource-id})
             (state/add-interaction! :active interaction)
             (set! rest/send-interrupt-request (fn [interaction-id interrupt-type interrupt-body]
                                                 (when (and interaction-id interrupt-type interrupt-body)
                                                   (go {:status 200}))))
             (p/subscribe topic (fn [e t r]
                                  (is (= {:interactionId interaction-id
                                          :subId sub-id
                                          :actionId action-id
                                          :resourceId resource-id
                                          :tenantId tenant-id
                                          :sessionId session-id
                                          :workOfferId work-offer-id
                                          :direction "inbound"
                                          :channelType "Test"
                                          :contactId contact-id} (js->clj r :keywordize-keys true)))
                                  (set! rest/send-interrupt-request old)
                                  (done)))
             (interaction/unassign {:contact-id contact-id :interaction-id interaction-id})))))

(deftest enable-wrapup-test
  (testing "the enable-wrapup function"
    (async done
           (reset! state/sdk-state)
           (reset! p/sdk-subscriptions)
           (let [old rest/send-interrupt-request
                 tenant-id (str (id/make-random-uuid))
                 resource-id (str (id/make-random-uuid))
                 interaction-id (str (id/make-random-uuid))
                 topic (p/get-topic :enable-wrapup-acknowledged)]
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
             (interaction/enable-wrapup {:interaction-id interaction-id})))))

(deftest disable-wrapup-test
  (testing "the enable-wrapup function"
    (async done
           (reset! state/sdk-state)
           (reset! p/sdk-subscriptions)
           (let [old rest/send-interrupt-request
                 tenant-id (str (id/make-random-uuid))
                 resource-id (str (id/make-random-uuid))
                 interaction-id (str (id/make-random-uuid))
                 topic (p/get-topic :disable-wrapup-acknowledged)]
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
             (interaction/disable-wrapup {:interaction-id interaction-id})))))

(deftest end-wrapup-test
  (testing "the end-wrapup function"
    (async done
           (reset! state/sdk-state)
           (reset! p/sdk-subscriptions)
           (let [old rest/send-interrupt-request
                 tenant-id (str (id/make-random-uuid))
                 resource-id (str (id/make-random-uuid))
                 interaction-id (str (id/make-random-uuid))
                 topic (p/get-topic :end-wrapup-acknowledged)]
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
             (interaction/end-wrapup {:interaction-id interaction-id})))))

(deftest deselect-disposition-test
  (testing "the deselect disposition function"
    (async done
           (reset! state/sdk-state)
           (reset! p/sdk-subscriptions)
           (let [old rest/send-interrupt-request
                 tenant-id (str (id/make-random-uuid))
                 resource-id (str (id/make-random-uuid))
                 interaction-id (str (id/make-random-uuid))
                 topic (p/get-topic :disposition-code-changed)]
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
             (interaction/deselect-disposition {:interaction-id interaction-id})))))

(deftest select-disposition-test
  (testing "the select disposition function"
    (async done
           (reset! state/sdk-state)
           (reset! p/sdk-subscriptions)
           (let [old rest/send-interrupt-request
                 tenant-id (str (id/make-random-uuid))
                 resource-id (str (id/make-random-uuid))
                 interaction-id (str (id/make-random-uuid))
                 disposition-id (str (id/make-random-uuid))
                 disposition {:disposition-id disposition-id}
                 topic (p/get-topic :disposition-code-changed)]
             (state/set-active-tenant! tenant-id)
             (state/set-user-identity! {:user-id resource-id})
             (state/add-interaction! :active {:interaction-id interaction-id
                                              :disposition-code-details {:dispositions [disposition]}})
             (set! rest/send-interrupt-request (fn [interaction-id interrupt-type interrupt-body]
                                                 (when (and interaction-id interrupt-type interrupt-body)
                                                   (go {:status 200}))))
             (p/subscribe topic (fn [e t r]
                                  (is (= {:interactionId interaction-id
                                          :resourceId resource-id
                                          :disposition {:dispositionId disposition-id
                                                        :selected true}} (js->clj r :keywordize-keys true)))
                                  (set! rest/send-interrupt-request old)
                                  (done)))
             (interaction/select-disposition {:interaction-id interaction-id :disposition-id disposition-id})))))

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
