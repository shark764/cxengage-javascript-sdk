(ns cxengage-javascript-sdk.modules.contacts-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.contacts :as contacts]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs-uuid-utils.core :as uuid]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as state]
            [cljs.test :refer-macros [deftest is testing run-tests async]]))

(deftest get-query-str-test
  (testing "The query string builder"
    (let [query {:q "name"
                 :mobile "+15554442222"
                 :limit "5"
                 :page 0}]
      (is (= "?q=name&mobile=+15554442222&limit=5&page=0" (contacts/get-query-str query))))))

(deftest get-contact-test
  (testing "Contact module get contact function"
    (async done
           (go (let [fake-user (str (uuid/make-random-squuid))
                     date (js/Date.)
                     date-time (.toISOString date)
                     old rest/get-contact-request
                     contact-id (str (uuid/make-random-uuid))
                     tenant-id (str (uuid/make-random-uuid))
                     params {:contact-id contact-id}]
                 (state/set-active-tenant! tenant-id)
                 (set! rest/get-contact-request (fn [contact-id]
                                                  (go {:api-response {:result {:id contact-id
                                                                               :attributes {:name "Ian Bishop"
                                                                                            :mobile "+15554442222"
                                                                                            :age 27}
                                                                               :created date-time
                                                                               :createdBy fake-user
                                                                               :updated date-time
                                                                               :updatedBy fake-user}}
                                                       :status 200})))
                 (p/subscribe "cxengage/contacts/get-contact-response" (fn [error topic response]
                                                                         (is (= {:id contact-id
                                                                                 :attributes {:name "Ian Bishop"
                                                                                              :mobile "+15554442222"
                                                                                              :age 27}
                                                                                 :created date-time
                                                                                 :createdBy fake-user
                                                                                 :updated date-time
                                                                                 :updatedBy fake-user} (js->clj response :keywordize-keys true)))
                                                                         (set! rest/get-contact-request old)
                                                                         (done)))
                 (contacts/get-contact params))))))

(deftest get-contacts-test
  (testing "Contact module get contacts function"
    (async done
           (go (let [fake-user (str (uuid/make-random-squuid))
                     date (js/Date.)
                     date-time (.toISOString date)
                     old rest/get-contacts-request
                     contact-id (str (uuid/make-random-uuid))
                     tenant-id (str (uuid/make-random-uuid))
                     params {:contact-id contact-id}]
                 (state/set-active-tenant! tenant-id)
                 (set! rest/get-contacts-request (fn []
                                                   (go {:api-response {:result [{:id contact-id
                                                                                 :attributes {:name "Ian Bishop"
                                                                                              :mobile "+15554442222"
                                                                                              :age 27}
                                                                                 :created date-time
                                                                                 :createdBy fake-user
                                                                                 :updated date-time
                                                                                 :updatedBy fake-user}]}
                                                        :status 200})))
                 (p/subscribe "cxengage/contacts/get-contacts-response" (fn [error topic response]
                                                                          (is (= [{:id contact-id
                                                                                   :attributes {:name "Ian Bishop"
                                                                                                :mobile "+15554442222"
                                                                                                :age 27}
                                                                                   :created date-time
                                                                                   :createdBy fake-user
                                                                                   :updated date-time
                                                                                   :updatedBy fake-user}] (js->clj response :keywordize-keys true)))
                                                                          (set! rest/get-contacts-request old)
                                                                          (done)))
                 (contacts/get-contacts params))))))

(deftest search-contacts-test
  (testing "Contact module search contacts function"
    (async done
           (go (let [fake-user (str (uuid/make-random-squuid))
                     date (js/Date.)
                     date-time (.toISOString date)
                     old rest/search-contacts-request
                     contact-id (str (uuid/make-random-uuid))
                     tenant-id (str (uuid/make-random-uuid))
                     params {:query {:name "Ian"}}]
                 (state/set-active-tenant! tenant-id)
                 (set! rest/search-contacts-request (fn [query]
                                                      (go {:api-response {:result {:page 1
                                                                                   :count 1
                                                                                   :totalPages 1
                                                                                   :results [{:id contact-id
                                                                                              :attributes {:name "Ian Bishop"
                                                                                                           :mobile "+15554442222"
                                                                                                           :age 27}
                                                                                              :created date-time
                                                                                              :createdBy fake-user
                                                                                              :updated date-time
                                                                                              :updatedBy fake-user}]}}
                                                           :status 200})))
                 (p/subscribe "cxengage/contacts/search-contacts-response" (fn [error topic response]
                                                                             (is (= {:page 1
                                                                                     :count 1
                                                                                     :totalPages 1
                                                                                     :results [{:id contact-id
                                                                                                :attributes {:name "Ian Bishop"
                                                                                                             :mobile "+15554442222"
                                                                                                             :age 27}
                                                                                                :created date-time
                                                                                                :createdBy fake-user
                                                                                                :updated date-time
                                                                                                :updatedBy fake-user}]} (js->clj response :keywordize-keys true)))
                                                                             (set! rest/search-contacts-request old)
                                                                             (done)))
                 (contacts/search-contacts params))))))

(deftest create-contact-test
  (testing "Contact module create contact function"
    (async done
           (go (let [fake-user (str (uuid/make-random-squuid))
                     date (js/Date.)
                     date-time (.toISOString date)
                     old rest/create-contact-request
                     contact-id (str (uuid/make-random-uuid))
                     tenant-id (str (uuid/make-random-uuid))
                     params {:attributes {:name "Serenova"}}]
                 (state/set-active-tenant! tenant-id)
                 (set! rest/create-contact-request (fn [body]
                                                     (let [{:keys [attributes]} body]
                                                       (go {:api-response {:result {:id contact-id
                                                                                    :attributes attributes
                                                                                    :created date-time
                                                                                    :createdBy fake-user
                                                                                    :updated date-time
                                                                                    :updatedBy fake-user}}
                                                            :status 200}))))
                 (p/subscribe "cxengage/contacts/create-contact-response" (fn [error topic response]
                                                                            (is (= {:id contact-id
                                                                                    :attributes {:name "Serenova"}
                                                                                    :created date-time
                                                                                    :createdBy fake-user
                                                                                    :updated date-time
                                                                                    :updatedBy fake-user} (js->clj response :keywordize-keys true)))
                                                                            (set! rest/create-contact-request old)
                                                                            (done)))
                 (contacts/create-contact params))))))

(deftest update-contact-test
  (testing "Contact module update contact function"
    (async done
           (go (let [fake-user (str (uuid/make-random-squuid))
                     date (js/Date.)
                     date-time (.toISOString date)
                     old rest/update-contact-request
                     contact-id (str (uuid/make-random-uuid))
                     tenant-id (str (uuid/make-random-uuid))
                     params {:contactId contact-id
                             :attributes {:name "Serenova"}}]
                 (state/set-active-tenant! tenant-id)
                 (set! rest/update-contact-request (fn [contact-id body]
                                                     (let [{:keys [attributes]} body]
                                                       (go {:api-response {:result {:id contact-id
                                                                                    :attributes attributes
                                                                                    :created date-time
                                                                                    :createdBy fake-user
                                                                                    :updated date-time
                                                                                    :updatedBy fake-user}}
                                                            :status 200}))))
                 (p/subscribe "cxengage/contacts/update-contact-response" (fn [error topic response]
                                                                            (is (= {:id contact-id
                                                                                    :attributes {:name "Serenova"}
                                                                                    :created date-time
                                                                                    :createdBy fake-user
                                                                                    :updated date-time
                                                                                    :updatedBy fake-user} (js->clj response :keywordize-keys true)))
                                                                            (set! rest/update-contact-request old)
                                                                            (done)))
                 (contacts/update-contact params))))))

(deftest delete-contact-test
  (testing "Contact module delete contact function"
    (async done
           (go (let [old rest/delete-contact-request
                     contact-id (str (uuid/make-random-uuid))
                     tenant-id (str (uuid/make-random-uuid))
                     params {:contact-id contact-id}]
                 (state/set-active-tenant! tenant-id)
                 (set! rest/delete-contact-request (fn [contact-id]
                                                     (go {:api-response {:result true}
                                                          :status 200})))
                 (p/subscribe "cxengage/contacts/delete-contact-response" (fn [error topic response]
                                                                            (is (true? (js->clj response :keywordize-keys true)))
                                                                            (set! rest/delete-contact-request old)
                                                                            (done)))
                 (contacts/delete-contact params))))))

(deftest merge-contact-test
  (testing "Contact module merge contact function"
    (async done
           (go (let [fake-user (str (uuid/make-random-squuid))
                     date (js/Date.)
                     date-time (.toISOString date)
                     old rest/merge-contact-request
                     contact-id (str (uuid/make-random-uuid))
                     tenant-id (str (uuid/make-random-uuid))
                     params {:attributes {:name "Serenova"}
                             :contactIds [(str (uuid/make-random-squuid)) (str (uuid/make-random-squuid))]}]
                 (state/set-active-tenant! tenant-id)
                 (set! rest/merge-contact-request (fn [body]
                                                    (let [{:keys [attributes]} body]
                                                      (go {:api-response {:result {:id contact-id
                                                                                   :attributes attributes
                                                                                   :created date-time
                                                                                   :createdBy fake-user
                                                                                   :updated date-time
                                                                                   :updatedBy fake-user}}
                                                           :status 200}))))
                 (p/subscribe "cxengage/contacts/merge-contacts-response" (fn [error topic response]
                                                                            (is (= {:id contact-id
                                                                                    :attributes {:name "Serenova"}
                                                                                    :created date-time
                                                                                    :createdBy fake-user
                                                                                    :updated date-time
                                                                                    :updatedBy fake-user} (js->clj response :keywordize-keys true)))
                                                                            (set! rest/merge-contact-request old)
                                                                            (done)))
                 (contacts/merge-contacts params))))))

(deftest list-attributes-test
  (testing "Contact module list attributes function"
    (async done
           (go (let [fake-user (str (uuid/make-random-squuid))
                     date (js/Date.)
                     date-time (.toISOString date)
                     old rest/list-attributes-request
                     tenant-id (str (uuid/make-random-uuid))]
                 (state/set-active-tenant! tenant-id)
                 (p/subscribe "cxengage/contacts/list-attributes-response" (fn [error topic response]
                                                                             (is (= [{:mandatory false
                                                                                      :updated "2017-01-30T16:10:20Z"
                                                                                      :default ""
                                                                                      :type "text"
                                                                                      :created "2017-01-30T16:10:20Z"
                                                                                      :active true
                                                                                      :label {:en-US "Name"}
                                                                                      :object-name "name"}
                                                                                     {:mandatory true
                                                                                      :updated "2017-01-30T16:10:20Z"
                                                                                      :default ""
                                                                                      :type "text"
                                                                                      :created "2017-01-30T16:10:20Z"
                                                                                      :active true
                                                                                      :label {:en-US "Phone"}
                                                                                      :object-name "phone"}] (js->clj response :keywordize-keys true)))
                                                                             (set! rest/list-attributes-request old)
                                                                             (done)))
                 (set! rest/list-attributes-request (fn []
                                                      (go {:status 200
                                                           :api-response {:result [{:mandatory false
                                                                                    :updated "2017-01-30T16:10:20Z"
                                                                                    :default ""
                                                                                    :type "text"
                                                                                    :created "2017-01-30T16:10:20Z"
                                                                                    :active true
                                                                                    :label {:en-US "Name"}
                                                                                    :object-name "name"}
                                                                                   {:mandatory true
                                                                                    :updated "2017-01-30T16:10:20Z"
                                                                                    :default ""
                                                                                    :type "text"
                                                                                    :created "2017-01-30T16:10:20Z"
                                                                                    :active true
                                                                                    :label {:en-US "Phone"}
                                                                                    :object-name "phone"}]}})))
                 (state/set-active-tenant! {:tenant-id tenant-id})
                 (contacts/list-attributes))))))


(deftest get-layout-test
  (testing "Contact module get layout function"
    (async done
           (go (let [fake-user (str (uuid/make-random-squuid))
                     date (js/Date.)
                     date-time (.toISOString date)
                     old rest/get-contact-request
                     layout-id (str (uuid/make-random-uuid))
                     tenant-id (str (uuid/make-random-uuid))
                     attribute-id-1 (str (uuid/make-random-uuid))
                     attribute-id-2 (str (uuid/make-random-uuid))
                     params {:layout-id layout-id}]
                 (state/set-active-tenant! tenant-id)
                 (set! rest/get-layout-request (fn [layout-id]
                                                 (go {:api-response {:result {:description ""
                                                                              :layout [{:label {:en-US "1"}
                                                                                        :attributes [attribute-id-1]}
                                                                                       [{:label {:en-US "2"}
                                                                                         :attributes [attribute-id-2]}]]
                                                                              :updated date-time
                                                                              :name "basic"
                                                                              :id layout-id
                                                                              :created date-time}}
                                                      :status 200})))
                 (p/subscribe "cxengage/contacts/get-layout-response" (fn [error topic response]
                                                                        (is (= {:description ""
                                                                                :layout [{:label {:en-US "1"}
                                                                                          :attributes [attribute-id-1]}
                                                                                         [{:label {:en-US "2"}
                                                                                           :attributes [attribute-id-2]}]]
                                                                                :updated date-time
                                                                                :name "basic"
                                                                                :id layout-id
                                                                                :created date-time}  (js->clj response :keywordize-keys true)))
                                                                        (set! rest/get-layout-request old)
                                                                        (done)))
                 (contacts/get-layout params))))))

(deftest list-layouts-test
  (testing "Contact module list layouts function"
    (async done
           (go (let [fake-user (str (uuid/make-random-squuid))
                     date (js/Date.)
                     date-time (.toISOString date)
                     old rest/list-layouts-request
                     tenant-id (str (uuid/make-random-uuid))
                     attribute-id-1 (str (uuid/make-random-uuid))
                     attribute-id-2 (str (uuid/make-random-uuid))
                     layout-id (str (uuid/make-random-uuid))
                     layout-id-2 (str (uuid/make-random-uuid))]
                 (state/set-active-tenant! tenant-id)
                 (p/subscribe "cxengage/contacts/list-layouts-response" (fn [error topic response]
                                                                          (is (= [{:description ""
                                                                                   :layout [{:label {:en-US "1"}
                                                                                             :attributes [attribute-id-1]}
                                                                                            [{:label {:en-US "2"}
                                                                                              :attributes [attribute-id-2]}]]
                                                                                   :updated date-time
                                                                                   :name "basic"
                                                                                   :id layout-id
                                                                                   :created date-time}
                                                                                  {:description ""
                                                                                   :layout [{:label {:en-US "1"}
                                                                                             :attributes [attribute-id-1]}
                                                                                            [{:label {:en-US "2"}
                                                                                              :attributes [attribute-id-2]}]]
                                                                                   :updated date-time
                                                                                   :name "basic"
                                                                                   :id layout-id-2
                                                                                   :created date-time}] (js->clj response :keywordize-keys true)))
                                                                          (set! rest/list-layouts-request old)
                                                                          (done)))
                 (set! rest/list-layouts-request (fn []
                                                   (go {:status 200
                                                        :api-response {:result [{:description ""
                                                                                 :layout [{:label {:en-US "1"}
                                                                                           :attributes [attribute-id-1]}
                                                                                          [{:label {:en-US "2"}
                                                                                            :attributes [attribute-id-2]}]]
                                                                                 :updated date-time
                                                                                 :name "basic"
                                                                                 :id layout-id
                                                                                 :created date-time}
                                                                                {:description ""
                                                                                 :layout [{:label {:en-US "1"}
                                                                                           :attributes [attribute-id-1]}
                                                                                          [{:label {:en-US "2"}
                                                                                            :attributes [attribute-id-2]}]]
                                                                                 :updated date-time
                                                                                 :name "basic"
                                                                                 :id layout-id-2
                                                                                 :created date-time}]}})))
                 (state/set-active-tenant! {:tenant-id tenant-id})
                 (contacts/list-layouts))))))
