(ns cxengage-javascript-sdk.modules.contacts-test
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [clojure.string :as str])
  (:require [cxengage-javascript-sdk.modules.contacts :as contacts]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs-uuid-utils.core :as uuid]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.api-utils :as api]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cljs-sdk-utils.test :refer [camels]]
            [cxengage-javascript-sdk.state :as state]
            [cljs.test :refer-macros [deftest is testing run-tests async]]))

(def mock-sad-api-response
  {:api-response {}
   :status 500})

(def mock-tenant-id (str (uuid/make-random-squuid)))
(def mock-user-id (str (uuid/make-random-squuid)))
(def mock-contact-id (str (uuid/make-random-squuid)))
(def mock-timestamp (.toISOString (js/Date.)))

(state/set-active-tenant! mock-tenant-id)

;; -----------------------------------------------------------------------------------------------------------
;; Testing internal functions
;; -----------------------------------------------------------------------------------------------------------

(deftest get-query-str-test
  (testing "The query string builder"
    (let [query {:q "name"
                 :mobile "+15554442222"
                 :limit "5"
                 :page 0}]
      (is (= "?q=name&mobile=+15554442222&limit=5&page=0" (contacts/get-query-str query))))))

;; -----------------------------------------------------------------------------------------------------------
;; Testing public-facing fn CxEngage.contacts.getContact();
;; -----------------------------------------------------------------------------------------------------------

(def get-contact-happy-path-mock-api-response
  {:api-response {"result" {:id mock-contact-id
                           :attributes {:name "Ian Bishop"
                                        :mobile "+15554442222"
                                        :age 27}
                           :created mock-timestamp
                           :createdBy mock-user-id
                           :updated mock-timestamp
                           :updatedBy mock-user-id}}
   :status 200})

(def get-contact-happy-path-mock-consumer-response
  (get-in get-contact-happy-path-mock-api-response [:api-response "result"]))

(deftest get-contact-test-1
  (testing "Contact module get-contact function -- happy path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-contact-request (fn []
                                      (go get-contact-happy-path-mock-api-response)))
     (p/subscribe
      "cxengage/contacts/get-contact-response"
      (fn [error topic response]
        (is (= get-contact-happy-path-mock-consumer-response
               (js->clj response :keywordize-keys true)))
        (done)))
     (contacts/get-contact {:contact-id mock-contact-id}))))

(deftest get-contact-test-2
  (testing "Contact module get-contact function -- happy path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-contact-request
           (fn []
             (go get-contact-happy-path-mock-api-response)))
     (contacts/get-contact
      {:contact-id mock-contact-id}
      (fn [error topic response]
        (is (= get-contact-happy-path-mock-consumer-response
               (js->clj response :keywordize-keys true)))
        (done))))))

(deftest get-contact-test-3
  (testing "Contact module get-contact function -- sad path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-contact-request
           (fn []
             (go mock-sad-api-response)))
     (p/subscribe
      "cxengage/contacts/get-contact-response"
      (fn [error topic response]
        (is (= (camels (e/failed-to-get-contact-err mock-contact-id mock-sad-api-response))
               (js->clj error :keywordize-keys true)))
        (is (= response nil))
        (done)))
     (contacts/get-contact {:contact-id mock-contact-id}))))

(deftest get-contact-test-4
  (testing "Contact module get-contact function -- sad path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-contact-request
           (fn []
             (go mock-sad-api-response)))
     (contacts/get-contact
      {:contact-id mock-contact-id}
      (fn [error topic response]
        (is (= (camels (e/failed-to-get-contact-err mock-contact-id mock-sad-api-response))
               (js->clj error :keywordize-keys true)))
        (is (= response nil))
        (done))))))

;; -----------------------------------------------------------------------------------------------------------
;; Testing public-facing fn CxEngage.contacts.getContacts();
;; -----------------------------------------------------------------------------------------------------------

(def get-contacts-happy-path-mock-api-response
  {:api-response {"result" [{:id mock-contact-id
                            :attributes {:name "Ian Bishop"
                                         :mobile "+15554442222"
                                         :age 27}
                            :created mock-timestamp
                            :createdBy mock-user-id
                            :updated mock-timestamp
                            :updatedBy mock-user-id}]}
   :status 200})

(def get-contacts-happy-path-mock-consumer-response
  (get-in get-contacts-happy-path-mock-api-response [:api-response "result"]))

(deftest get-contacts-test-1
  (testing "Contact module get-contacts function -- happy path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-contacts-request
           (fn []
             (go get-contacts-happy-path-mock-api-response)))
     (p/subscribe
      "cxengage/contacts/get-contacts-response"
      (fn [error topic response]
        (is (= get-contacts-happy-path-mock-consumer-response
               (js->clj response :keywordize-keys true)))
        (done)))
     (contacts/get-contacts))))

(deftest get-contacts-test-2
  (testing "Contact module get-contacts function -- happy path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-contacts-request
           (fn []
             (go get-contacts-happy-path-mock-api-response)))
     (contacts/get-contacts
      (fn [error topic response]
        (is (= get-contacts-happy-path-mock-consumer-response
               (js->clj response :keywordize-keys true)))
        (done))))))

(deftest get-contacts-test-3
  (testing "Contact module get-contacts function -- sad path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-contacts-request
           (fn []
             (go mock-sad-api-response)))
     (p/subscribe
      "cxengage/contacts/get-contacts-response"
      (fn [error topic response]
        (is (= nil response))
        (is (= (js->clj error :keywordize-keys true) (camels (e/failed-to-list-all-contacts-err mock-sad-api-response))))
        (done)))
     (contacts/get-contacts))))

(deftest get-contacts-test-4
  (testing "Contact module get-contacts function -- sad path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-contacts-request
           (fn []
             (go mock-sad-api-response)))
     (contacts/get-contacts
      (fn [error topic response]
        (is (= response nil))
        (is (= (js->clj error :keywordize-keys true) (camels (e/failed-to-list-all-contacts-err mock-sad-api-response))))
        (done))))))

;; -----------------------------------------------------------------------------------------------------------
;; Testing public-facing fn CxEngage.contacts.searchContacts();
;; -----------------------------------------------------------------------------------------------------------

(def search-contacts-happy-path-mock-api-response
  {:api-response {"result" {:page 1
                           :count 1
                           :totalPages 1
                           :results [{:id mock-contact-id
                                      :attributes {:name "Ian Bishop"
                                                   :mobile "+15554442222"
                                                   :age 27}
                                      :created mock-timestamp
                                      :createdBy mock-user-id
                                      :updated mock-timestamp
                                      :updatedBy mock-user-id}]}}
   :status 200})

(def search-contacts-happy-path-mock-consumer-response
  (get-in search-contacts-happy-path-mock-api-response [:api-response "result"]))

(deftest search-contacts-test-1
  (testing "Contact module search-contacts function -- happy path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/search-contacts-request
           (fn []
             (go search-contacts-happy-path-mock-api-response)))
     (p/subscribe
      "cxengage/contacts/search-contacts-response"
      (fn [error topic response]
        (is (= (js->clj response :keywordize-keys true)
               search-contacts-happy-path-mock-consumer-response))
        (done)))
     (contacts/search-contacts {:query {:name "Ian"}}))))

(deftest search-contacts-test-2
  (testing "Contact module search-contacts function -- happy path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/search-contacts-request
           (fn []
             (go search-contacts-happy-path-mock-api-response)))
     (contacts/search-contacts
      {:query {:name "Ian"}}
      (fn [error topic response]
        (is (= search-contacts-happy-path-mock-consumer-response
               (js->clj response :keywordize-keys true)))
        (done))))))

(deftest search-contacts-test-3
  (testing "Contact module search-contacts function -- sad path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/search-contacts-request
           (fn []
             (go mock-sad-api-response)))
     (p/subscribe
      "cxengage/contacts/search-contacts-response"
      (fn [error topic response]
        (is (= nil response))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-search-contacts-err {:name "Ian"} mock-sad-api-response))))
        (done)))
     (contacts/search-contacts {:query {:name "Ian"}}))))

(deftest search-contacts-test-4
  (testing "Contact module search-contacts function -- sad path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/search-contacts-request
           (fn []
             (go mock-sad-api-response)))
     (contacts/search-contacts
      {:query {:name "Ian"}}
      (fn [error topic response]
        (is (= response nil))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-search-contacts-err {:name "Ian"} mock-sad-api-response))))
        (done))))))

;; -----------------------------------------------------------------------------------------------------------
;; Testing public-facing fn CxEngage.contacts.createContact();
;; -----------------------------------------------------------------------------------------------------------

(def create-contact-happy-path-mock-api-response
  {:api-response {"result" {:id mock-contact-id
                           :attributes {:name "Serenova"}
                           :created mock-timestamp
                           :createdBy mock-user-id
                           :updated mock-timestamp
                           :updatedBy mock-user-id}}
   :status 200})
 
(def create-contact-happy-path-mock-consumer-response
  (get-in create-contact-happy-path-mock-api-response [:api-response "result"]))

(deftest create-contact-test-1
  (testing "Contact module create-contact function -- happy path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/create-contact-request
           (fn []
             (go create-contact-happy-path-mock-api-response)))
     (p/subscribe
      "cxengage/contacts/create-contact-response"
      (fn [error topic response]
        (is (= (js->clj response :keywordize-keys true)
               create-contact-happy-path-mock-consumer-response))
        (done)))
     (contacts/create-contact {:attributes {:name "Serenova"}}))))

(deftest create-contact-test-2
  (testing "Contact module create-contact function -- happy path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/create-contact-request
           (fn []
             (go create-contact-happy-path-mock-api-response)))
     (contacts/create-contact
      {:attributes {:name "Serenova"}}
      (fn [error topic response]
        (is (= create-contact-happy-path-mock-consumer-response
               (js->clj response :keywordize-keys true)))
        (done))))))

(deftest create-contact-test-3
  (testing "Contact module create-contact function -- sad path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/create-contact-request
           (fn []
             (go mock-sad-api-response)))
     (p/subscribe
      "cxengage/contacts/create-contact-response"
      (fn [error topic response]
        (is (= nil response))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-create-contact-err {:name "Serenova"} mock-sad-api-response))))
        (done)))
     (contacts/create-contact {:attributes {:name "Serenova"}}))))

(deftest create-contact-test-4
  (testing "Contact module create-contact function -- sad path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/create-contact-request
           (fn []
             (go mock-sad-api-response)))
     (contacts/create-contact
      {:attributes {:name "Serenova"}}
      (fn [error topic response]
        (is (= response nil))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-create-contact-err {:name "Serenova"} mock-sad-api-response))))
        (done))))))

;; -----------------------------------------------------------------------------------------------------------
;; Testing public-facing fn CxEngage.contacts.updateContact();
;; -----------------------------------------------------------------------------------------------------------

(def update-contact-happy-path-mock-api-response
  {:api-response {"result" {:id mock-contact-id
                           :attributes {:name "Serenova"}
                           :created mock-timestamp
                           :createdBy mock-user-id
                           :updated mock-timestamp
                           :updatedBy mock-user-id}}
   :status 200})

(def update-contact-happy-path-mock-consumer-response
  (get-in update-contact-happy-path-mock-api-response [:api-response "result"]))

(deftest update-contact-test-1
  (testing "Contact module update-contact function -- happy path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/update-contact-request
           (fn []
             (go update-contact-happy-path-mock-api-response)))
     (p/subscribe
      "cxengage/contacts/update-contact-response"
      (fn [error topic response]
        (is (= (js->clj response :keywordize-keys true)
               update-contact-happy-path-mock-consumer-response))
        (done)))
     (contacts/update-contact {:contactId mock-contact-id
                               :attributes {:name "Serenova"}}))))

(deftest update-contact-test-2
  (testing "Contact module update-contact function -- happy path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/update-contact-request
           (fn []
             (go update-contact-happy-path-mock-api-response)))
     (contacts/update-contact
      {:contactId mock-contact-id
       :attributes {:name "Serenova"}}
      (fn [error topic response]
        (is (= update-contact-happy-path-mock-consumer-response
               (js->clj response :keywordize-keys true)))
        (done))))))

(deftest update-contact-test-3
  (testing "Contact module update-contact function -- sad path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/update-contact-request
           (fn []
             (go mock-sad-api-response)))
     (p/subscribe
      "cxengage/contacts/update-contact-response"
      (fn [error topic response]
        (is (= nil response))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-update-contact-err mock-contact-id {:name "Serenova"} mock-sad-api-response))))
        (done)))
     (contacts/update-contact {:contactId mock-contact-id
                               :attributes {:name "Serenova"}}))))

(deftest update-contact-test-4
  (testing "Contact module update-contact function -- sad path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/update-contact-request
           (fn []
             (go mock-sad-api-response)))
     (contacts/update-contact
      {:contactId mock-contact-id
       :attributes {:name "Serenova"}}
      (fn [error topic response]
        (is (= response nil))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-update-contact-err mock-contact-id {:name "Serenova"} mock-sad-api-response))))
        (done))))))

;; -----------------------------------------------------------------------------------------------------------
;; Testing public-facing fn CxEngage.contacts.deleteContact();
;; -----------------------------------------------------------------------------------------------------------

(def delete-contact-happy-path-mock-api-response
  {:api-response {:result true}
   :status 200})

(def delete-contact-happy-path-mock-consumer-response
  (get-in delete-contact-happy-path-mock-api-response [:api-response :result]))

(deftest delete-contact-test-1
  (testing "Contact module delete-contact function -- happy path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/delete-contact-request
           (fn []
             (go delete-contact-happy-path-mock-api-response)))
     (p/subscribe
      "cxengage/contacts/delete-contact-response"
      (fn [error topic response]
        (is (= (js->clj response :keywordize-keys true)
               delete-contact-happy-path-mock-consumer-response))
        (done)))
     (contacts/delete-contact {:contactId mock-contact-id
                               :attributes {:name "Serenova"}}))))

(deftest delete-contact-test-2
  (testing "Contact module delete-contact function -- happy path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/delete-contact-request
           (fn []
             (go delete-contact-happy-path-mock-api-response)))
     (contacts/delete-contact
      {:contactId mock-contact-id
       :attributes {:name "Serenova"}}
      (fn [error topic response]
        (is (= delete-contact-happy-path-mock-consumer-response
               (js->clj response :keywordize-keys true)))
        (done))))))

(deftest delete-contact-test-3
  (testing "Contact module delete-contact function -- sad path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/delete-contact-request
           (fn []
             (go mock-sad-api-response)))
     (p/subscribe
      "cxengage/contacts/delete-contact-response"
      (fn [error topic response]
        (is (= nil response))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-delete-contact-err mock-contact-id mock-sad-api-response))))
        (done)))
     (contacts/delete-contact {:contactId mock-contact-id
                               :attributes {:name "Serenova"}}))))

(deftest delete-contact-test-4
  (testing "Contact module delete-contact function -- sad path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/delete-contact-request
           (fn []
             (go mock-sad-api-response)))
     (contacts/delete-contact
      {:contactId mock-contact-id
       :attributes {:name "Serenova"}}
      (fn [error topic response]
        (is (= response nil))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-delete-contact-err mock-contact-id mock-sad-api-response))))
        (done))))))

;; -----------------------------------------------------------------------------------------------------------
;; Testing public-facing fn CxEngage.contacts.mergeContacts();
;; -----------------------------------------------------------------------------------------------------------

(def merge-contacts-happy-path-mock-api-response
  {:api-response {"result" {:id mock-contact-id
                           :attributes {:name "Serenova"}
                           :created mock-timestamp
                           :createdBy mock-user-id
                           :updated mock-timestamp
                           :updatedBy mock-user-id}}
   :status 200})

(def merge-contacts-happy-path-mock-consumer-response
  (get-in merge-contacts-happy-path-mock-api-response [:api-response "result"]))

(deftest merge-contacts-test-1
  (testing "Contact module merge-contacts function -- happy path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/merge-contact-request
           (fn []
             (go merge-contacts-happy-path-mock-api-response)))
     (p/subscribe
      "cxengage/contacts/merge-contacts-response"
      (fn [error topic response]
        (is (= (js->clj response :keywordize-keys true)
               merge-contacts-happy-path-mock-consumer-response))
        (done)))
     (contacts/merge-contacts {:attributes {:name "Serenova"}
                               :contactIds [(str (uuid/make-random-squuid)) (str (uuid/make-random-squuid))]}))))

(deftest merge-contacts-test-2
  (testing "Contact module merge-contacts function -- happy path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/merge-contact-request
           (fn []
             (go merge-contacts-happy-path-mock-api-response)))
     (contacts/merge-contacts
      {:attributes {:name "Serenova"}
       :contactIds [(str (uuid/make-random-squuid)) (str (uuid/make-random-squuid))]}
      (fn [error topic response]
        (is (= merge-contacts-happy-path-mock-consumer-response
               (js->clj response :keywordize-keys true)))
        (done))))))

(def mock-contact-ids [(str (uuid/make-random-squuid)) (str (uuid/make-random-squuid))])

(deftest merge-contacts-test-3
  (testing "Contact module merge-contacts function -- sad path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/merge-contact-request
           (fn []
             (go mock-sad-api-response)))
     (p/subscribe
      "cxengage/contacts/merge-contacts-response"
      (fn [error topic response]
        (is (= nil response))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-merge-contacts-err mock-contact-ids mock-sad-api-response))))
        (done)))
     (contacts/merge-contacts {:attributes {:name "Serenova"}
                               :contactIds mock-contact-ids}))))

(deftest merge-contacts-test-4
  (testing "Contact module merge-contacts function -- sad path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/merge-contact-request
           (fn []
             (go mock-sad-api-response)))
     (contacts/merge-contacts
      {:attributes {:name "Serenova"}
       :contactIds mock-contact-ids}
      (fn [error topic response]
        (is (= response nil))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-merge-contacts-err mock-contact-ids mock-sad-api-response))))
        (done))))))

;; -----------------------------------------------------------------------------------------------------------
;; Testing public-facing fn CxEngage.contacts.listAttributes();
;; -----------------------------------------------------------------------------------------------------------

(def list-attributes-happy-path-mock-api-response
  {:status 200
   :api-response {:result [{:mandatory false
                            :updated "2017-01-30T16:10:20Z"
                            :default ""
                            :type "text"
                            :created "2017-01-30T16:10:20Z"
                            :active true
                            :label {:en-us "Name"}
                            :objectName "name"}
                           {:mandatory true
                            :updated "2017-01-30T16:10:20Z"
                            :default ""
                            :type "text"
                            :created "2017-01-30T16:10:20Z"
                            :active true
                            :label {:en-us "Phone"}
                            :objectName "phone"}]}})

(def list-attributes-happy-path-mock-consumer-response
  (get-in list-attributes-happy-path-mock-api-response [:api-response :result]))

(deftest list-attributes-test-1
  (testing "Contact module list-attributes function -- happy path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/list-attributes-request
           (fn []
             (go list-attributes-happy-path-mock-api-response)))
     (p/subscribe
      "cxengage/contacts/list-attributes-response"
      (fn [error topic response]
        (is (= (js->clj response :keywordize-keys true)
               list-attributes-happy-path-mock-consumer-response))
        (done)))
     (contacts/list-attributes))))

(deftest list-attributes-test-2
  (testing "Contact module list-attributes function -- happy path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/list-attributes-request
           (fn []
             (go list-attributes-happy-path-mock-api-response)))
     (contacts/list-attributes
      (fn [error topic response]
        (is (= list-attributes-happy-path-mock-consumer-response
               (js->clj response :keywordize-keys true)))
        (done))))))

(deftest list-attributes-test-3
  (testing "Contact module list-attributes function -- sad path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/list-attributes-request
           (fn []
             (go mock-sad-api-response)))
     (p/subscribe
      "cxengage/contacts/list-attributes-response"
      (fn [error topic response]
        (is (= nil response))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-list-contact-attributes-err mock-sad-api-response))))
        (done)))
     (contacts/list-attributes))))

(deftest list-attributes-test-4
  (testing "Contact module list-attributes function -- sad path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/list-attributes-request
           (fn []
             (go mock-sad-api-response)))
     (contacts/list-attributes
      (fn [error topic response]
        (is (= response nil))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-list-contact-attributes-err mock-sad-api-response))))
        (done))))))

;; -----------------------------------------------------------------------------------------------------------
;; Testing public-facing fn CxEngage.contacts.getLayout();
;; -----------------------------------------------------------------------------------------------------------

(def get-layout-happy-path-mock-api-response
  {:api-response {:result {:description ""
                           :layout [{:label {:en-us "1"}
                                     :attributes [(str (uuid/make-random-squuid))]}
                                    [{:label {:en-us "2"}
                                      :attributes [(str (uuid/make-random-squuid))]}]]
                           :updated mock-timestamp
                           :name "basic"
                           :id (str (uuid/make-random-squuid))
                           :created mock-timestamp}}
   :status 200})

(def get-layout-happy-path-mock-consumer-response
  (get-in get-layout-happy-path-mock-api-response [:api-response :result]))

(deftest get-layout-test-1
  (testing "Contact module get-layout function -- happy path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-layout-request
           (fn []
             (go get-layout-happy-path-mock-api-response)))
     (p/subscribe
      "cxengage/contacts/get-layout-response"
      (fn [error topic response]
        (is (= (js->clj response :keywordize-keys true)
               get-layout-happy-path-mock-consumer-response))
        (done)))
     (contacts/get-layout {:layoutId mock-contact-id}))))

(deftest get-layout-test-2
  (testing "Contact module get-layout function -- happy path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-layout-request
           (fn []
             (go get-layout-happy-path-mock-api-response)))
     (contacts/get-layout
      {:layoutId mock-contact-id}
      (fn [error topic response]
        (is (= get-layout-happy-path-mock-consumer-response
               (js->clj response :keywordize-keys true)))
        (done))))))

(deftest get-layout-test-3
  (testing "Contact module get-layout function -- sad path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-layout-request
           (fn []
             (go mock-sad-api-response)))
     (p/subscribe
      "cxengage/contacts/get-layout-response"
      (fn [error topic response]
        (is (= nil response))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-retrieve-contact-layout-err mock-contact-id mock-sad-api-response))))
        (done)))
     (contacts/get-layout {:layoutId mock-contact-id}))))

(deftest get-layout-test-4
  (testing "Contact module get-layout function -- sad path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/get-layout-request
           (fn []
             (go mock-sad-api-response)))
     (contacts/get-layout
      {:layoutId mock-contact-id}
      (fn [error topic response]
        (is (= response nil))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-retrieve-contact-layout-err mock-contact-id mock-sad-api-response))))
        (done))))))

;; -----------------------------------------------------------------------------------------------------------
;; Testing public-facing fn CxEngage.contacts.listLayouts();
;; -----------------------------------------------------------------------------------------------------------

(def list-layouts-happy-path-mock-api-response
  {:api-response {:result {:description ""
                           :layout [{:label {:en-us "1"}
                                     :attributes [(str (uuid/make-random-squuid))]}
                                    [{:label {:en-us "2"}
                                      :attributes [(str (uuid/make-random-squuid))]}]]
                           :updated mock-timestamp
                           :name "basic"
                           :id (str (uuid/make-random-squuid))
                           :created mock-timestamp}}
   :status 200})

(def list-layouts-happy-path-mock-consumer-response
  (get-in list-layouts-happy-path-mock-api-response [:api-response :result]))

(deftest list-layouts-test-1
  (testing "Contact module list-layouts function -- happy path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/list-layouts-request
           (fn []
             (go list-layouts-happy-path-mock-api-response)))
     (p/subscribe
      "cxengage/contacts/list-layouts-response"
      (fn [error topic response]
        (is (= (js->clj response :keywordize-keys true)
               list-layouts-happy-path-mock-consumer-response))
        (done)))
     (contacts/list-layouts))))

(deftest list-layouts-test-2
  (testing "Contact module list-layouts function -- happy path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/list-layouts-request
           (fn []
             (go list-layouts-happy-path-mock-api-response)))
     (contacts/list-layouts
      (fn [error topic response]
        (is (= list-layouts-happy-path-mock-consumer-response
               (js->clj response :keywordize-keys true)))
        (done))))))

(deftest list-layouts-test-3
  (testing "Contact module list-layouts function -- sad path -- pubsub response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/list-layouts-request
           (fn []
             (go mock-sad-api-response)))
     (p/subscribe
      "cxengage/contacts/list-layouts-response"
      (fn [error topic response]
        (is (= nil response))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-retrieve-contact-layouts-list-err mock-sad-api-response))))
        (done)))
     (contacts/list-layouts))))

(deftest list-layouts-test-4
  (testing "Contact module list-layouts function -- sad path -- callback response mechanism"
    (async
     done
     (reset! p/sdk-subscriptions {})
     (set! rest/list-layouts-request
           (fn []
             (go mock-sad-api-response)))
     (contacts/list-layouts
      (fn [error topic response]
        (is (= response nil))
        (is (= (js->clj error :keywordize-keys true)
               (camels (e/failed-to-retrieve-contact-layouts-list-err mock-sad-api-response))))
        (done))))))
