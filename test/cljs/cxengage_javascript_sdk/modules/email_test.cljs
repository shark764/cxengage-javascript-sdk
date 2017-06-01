(ns cxengage-javascript-sdk.modules.email-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.email :as email]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs-sdk-utils.api :as api]
            [cljs-uuid-utils.core :as id]
            [cljs.test :refer-macros [deftest is testing async]]))

(deftest click-to-email-test-happy
  (testing "The click to email function called start-outbound-email"
    (async done
           (reset! p/sdk-subscriptions)
           (let [old rest/create-interaction-request
                 tenant-id (str (id/make-random-squuid))
                 resource-id (str (id/make-random-squuid))
                 interaction-id (str (id/make-random-squuid))
                 flow-id (str (id/make-random-squuid))
                 flow-version (str (id/make-random-squuid))]
             (state/set-active-tenant! tenant-id)
             (state/set-user-identity! {:user-id resource-id})
             (set! rest/create-interaction-request (fn [body]
                                                     (let [{:keys [source
                                                                   customer
                                                                   contact-point
                                                                   channel-type
                                                                   direction
                                                                   interaction
                                                                   metadata
                                                                   id]} body]
                                                       (when (and source customer contact-point channel-type
                                                                  direction interaction metadata id)
                                                         (go {:api-response {:interaction-id interaction-id
                                                                             :flow-id flow-id
                                                                             :flow-version flow-version}
                                                              :status 200})))))
             (p/subscribe "cxengage/interactions/email/start-outbound-email" (fn [e t r]
                                                                               (is (= {:interactionId interaction-id
                                                                                       :flowId flow-id
                                                                                       :flowVersion flow-version} (js->clj r :keywordize-keys true)))
                                                                               (set! rest/create-interaction-request old)
                                                                               (done)))
             (email/start-outbound-email {:address "unit@test.com"})))))

(deftest click-to-email-test-sad
  (testing "The click to email function called start-outbound-email"
    (async done
           (let [old rest/create-interaction-request
                 tenant-id (str (id/make-random-squuid))
                 resource-id (str (id/make-random-squuid))
                 interaction-id (str (id/make-random-squuid))
                 flow-id (str (id/make-random-squuid))
                 flow-version (str (id/make-random-squuid))]
             (state/set-active-tenant! tenant-id)
             (state/set-user-identity! {:user-id resource-id})
             (set! rest/create-interaction-request (fn [body]
                                                     (let [{:keys [source
                                                                   customer
                                                                   contact-point
                                                                   channel-type
                                                                   direction
                                                                   interaction
                                                                   metadata
                                                                   id]} body]
                                                       (when (and source customer contact-point channel-type
                                                                  direction interaction metadata id)
                                                         (go {:api-response {:bad-request "Missing some stuff, or whatever."}
                                                              :status 400})))))
             (p/subscribe "cxengage/errors/error/failed-to-create-outbound-email-interaction" (fn [e t r]
                                                                                                (is (= {:code 10002
                                                                                                        :level "error"
                                                                                                        :message "Failed to create outbound email interaction."} (js->clj e :keywordize-keys true)))
                                                                                                (set! rest/create-interaction-request old)
                                                                                                (done)))
             (email/start-outbound-email {:address "unit@test.com"})))))
