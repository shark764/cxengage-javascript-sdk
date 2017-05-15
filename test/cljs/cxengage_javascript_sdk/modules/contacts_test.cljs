(ns cxengage-javascript-sdk.modules.contacts-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.contacts :as contacts]
            [cljs.core.async :as a]
            [cljs-uuid-utils.core :as uuid]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as state]
            [cljs.test :refer-macros [deftest is testing run-tests async]]))

(deftest contact-request-test-one
  (testing "The contact request function. Arity 4."
    (async done
           (go (let [old iu/api-request
                     the-chan (a/promise-chan)]
                 (set! iu/api-request (fn [request-map casing]
                                        (let [{:keys [url method]} request-map]
                                          (when (and url method)
                                            the-chan))))
                 (a/>! the-chan {:api-response {:result {:id "unit-test"}}
                                 :status 200})
                 (is (= {:api-response {:result {:id "unit-test"}}
                         :status 200} (a/<! (contacts/contact-request "dev-test.cxengagelab.net" nil :get true))))
                 (set! iu/api-request old)
                 (done))))))

(deftest get-query-str-test
  (testing "The query string builder"
    (let [query {:q "name"
                 :mobile "+15554442222"
                 :limit "5"
                 :page 0}]
      (is (= "?q=name&mobile=+15554442222&limit=5&page=0" (contacts/get-query-str query))))))

(deftest get-contact-test
  (testing "Contact module get contact function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)
          old contacts/contact-request]
      (set! contacts/contact-request (fn [url body method params topic-key spec module]
                                       (let [contact-id (get-in url [:params :contact-id])]
                                         (when (and method url params topic-key spec module)
                                           {:id contact-id
                                            :attributes {:name "Ian Bishop"
                                                         :mobile "+15554442222"
                                                         :age 27}
                                            :created date-time
                                            :createdBy fake-user
                                            :updated date-time
                                            :updatedBy fake-user}))))
      (let [contact-id (str (uuid/make-random-uuid))
            tenant-id (str (uuid/make-random-uuid))
            params {:contactId contact-id}
            get-response (contacts/get-contact params)
            get-response-4 (contacts/get-contact params (fn [] "blah"))]
        (is (= {:id contact-id
                :attributes {:name "Ian Bishop"
                             :mobile "+15554442222"
                             :age 27}
                :created date-time
                :createdBy fake-user
                :updated date-time
                :updatedBy fake-user} get-response))
        (is (= {:id contact-id
                :attributes {:name "Ian Bishop"
                             :mobile "+15554442222"
                             :age 27}
                :created date-time
                :createdBy fake-user
                :updated date-time
                :updatedBy fake-user} get-response-4))
        (set! contacts/contact-request old)))))

(deftest get-contacts-test
  (testing "Contact module search contactsmodule function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)
          contact-id (str (uuid/make-random-uuid))
          old contacts/contact-request]
      (set! contacts/contact-request (fn [url body method params topic-key spec module]
                                       (when (and method url params topic-key spec module)
                                         {:page 1
                                          :count 1
                                          :total-pages 1
                                          :results [{:id contact-id
                                                     :attributes {:name "Ian Bishop"
                                                                  :mobile "+15554442222"
                                                                  :age 27}
                                                     :created date-time
                                                     :createdBy fake-user
                                                     :updated date-time
                                                     :updatedBy fake-user}]})))
      (let [tenant-id (str (uuid/make-random-uuid))
            params {:tenant-id tenant-id}
            search-response (contacts/get-contacts params)
            search-response-2 (contacts/get-contacts)
            search-response-4 (contacts/get-contacts params (fn [] "blah"))]
        (is (= {:page 1
                :count 1
                :total-pages 1
                :results [{:id contact-id
                           :attributes {:name "Ian Bishop"
                                        :mobile "+15554442222"
                                        :age 27}
                           :created date-time
                           :createdBy fake-user
                           :updated date-time
                           :updatedBy fake-user}]} search-response))
        (is (= {:page 1
                :count 1
                :total-pages 1
                :results [{:id contact-id
                           :attributes {:name "Ian Bishop"
                                        :mobile "+15554442222"
                                        :age 27}
                           :created date-time
                           :createdBy fake-user
                           :updated date-time
                           :updatedBy fake-user}]} search-response-4))
        (is (= {:page 1
                :count 1
                :total-pages 1
                :results [{:id contact-id
                           :attributes {:name "Ian Bishop"
                                        :mobile "+15554442222"
                                        :age 27}
                           :created date-time
                           :createdBy fake-user
                           :updated date-time
                           :updatedBy fake-user}]} search-response-2))
        (set! contacts/contact-request old)))))

(deftest search-contacts-test
  (testing "Contact module search contactsmodule function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)
          contact-id (str (uuid/make-random-uuid))
          old contacts/contact-request]
      (set! contacts/contact-request (fn [url body method params topic-key spec module query]
                                       (when (and method url params topic-key spec module query)
                                         {:page 1
                                          :count 1
                                          :total-pages 1
                                          :results [{:id contact-id
                                                     :attributes {:name "Ian Bishop"
                                                                  :mobile "+15554442222"
                                                                  :age 27}
                                                     :created date-time
                                                     :createdBy fake-user
                                                     :updated date-time
                                                     :updatedBy fake-user}]})))
      (let [tenant-id (str (uuid/make-random-uuid))
            params {:tenant-id tenant-id
                    :query {:name "Ian"}}
            search-response (contacts/search-contacts params)
            search-response-4 (contacts/search-contacts params (fn [] "blah"))]
        (is (= {:page 1
                :count 1
                :total-pages 1
                :results [{:id contact-id
                           :attributes {:name "Ian Bishop"
                                        :mobile "+15554442222"
                                        :age 27}
                           :created date-time
                           :createdBy fake-user
                           :updated date-time
                           :updatedBy fake-user}]} search-response))
        (is (= {:page 1
                :count 1
                :total-pages 1
                :results [{:id contact-id
                           :attributes {:name "Ian Bishop"
                                        :mobile "+15554442222"
                                        :age 27}
                           :created date-time
                           :createdBy fake-user
                           :updated date-time
                           :updatedBy fake-user}]} search-response-4))
        (set! contacts/contact-request old)))))

(deftest create-contact-test
  (testing "Contact module create contact function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)
          old contacts/contact-request]
      (set! contacts/contact-request (fn [url body method params topic-key spec module]
                                       (let [{:keys [attributes]} body
                                             contact-id (uuid/make-random-uuid)]
                                         (when (and method body url params topic-key spec module)
                                           {:id contact-id
                                            :attributes attributes
                                            :created date-time
                                            :createdBy fake-user
                                            :updated date-time
                                            :updatedBy fake-user}))))
      (let [contact-id (str (uuid/make-random-uuid))
            tenant-id (str (uuid/make-random-uuid))
            params {:tenant-id tenant-id
                    :attributes {:name "Ian Bishop"
                                 :mobile "+15554442222"
                                 :age 27}}
            create-response (contacts/create-contact params)
            create-response-4 (contacts/create-contact params (fn [] "blah"))]
        (is (= {:attributes {:name "Ian Bishop"
                             :mobile "+15554442222"
                             :age 27}
                :created date-time
                :createdBy fake-user
                :updated date-time
                :updatedBy fake-user} (dissoc create-response :id)))
        (is (uuid/valid-uuid? (:id create-response)))
        (is (= {:attributes {:name "Ian Bishop"
                             :mobile "+15554442222"
                             :age 27}
                :created date-time
                :createdBy fake-user
                :updated date-time
                :updatedBy fake-user} (dissoc create-response-4 :id)))
        (is (uuid/valid-uuid? (:id create-response-4)))
        (set! contacts/contact-request old)))))

(deftest update-contact-test
  (testing "Contact module update contact function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)
          old contacts/contact-request]
      (set! contacts/contact-request (fn [url body method params topic-key spec module]
                                       (let [{:keys [attributes]} body
                                             contact-id (get-in url [:params :contact-id])]
                                         (when (and method body url params topic-key spec module)
                                           {:id contact-id
                                            :attributes attributes
                                            :created date-time
                                            :createdBy fake-user
                                            :updated date-time
                                            :updatedBy fake-user}))))
      (let [contact-id (str (uuid/make-random-uuid))
            tenant-id (str (uuid/make-random-uuid))
            params {:contactId contact-id
                    :attributes {:name "Ian Bishop"
                                 :mobile "+15554442222"
                                 :age 27}}
            update-response (contacts/update-contact params)
            update-response-4 (contacts/update-contact params (fn [] "blah"))]
        (is (= {:id contact-id
                :attributes {:name "Ian Bishop"
                             :mobile "+15554442222"
                             :age 27}
                :created date-time
                :createdBy fake-user
                :updated date-time
                :updatedBy fake-user} update-response))
        (is (= {:id contact-id
                :attributes {:name "Ian Bishop"
                             :mobile "+15554442222"
                             :age 27}
                :created date-time
                :createdBy fake-user
                :updated date-time
                :updatedBy fake-user} update-response-4))
        (set! contacts/contact-request old)))))

(deftest delete-contact-test
  (testing "Contact module delete contact function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)
          old contacts/contact-request]
      (set! contacts/contact-request (fn [url body method params topic-key spec module]
                                       (let [contact-id (get-in url [:params :contact-id])]
                                         (when (and contact-id method url params topic-key spec module)
                                           true))))
      (let [contact-id (str (uuid/make-random-uuid))
            tenant-id (str (uuid/make-random-uuid))
            params {:contactId contact-id}
            delete-response (contacts/delete-contact params)
            delete-response-4 (contacts/delete-contact params (fn [] "blah"))]

        (is (true? delete-response))
        (is (true? delete-response-4))
        (set! contacts/contact-request old)))))

(deftest list-attributes-test
  (testing "Contact module list attributes function"
    (async done
           (go (let [fake-user (str (uuid/make-random-squuid))
                     date (js/Date.)
                     date-time (.toISOString date)
                     old iu/api-request
                     the-chan (a/promise-chan)
                     api-resp {:status 200
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
                                                        :object-name "phone"}]}}
                     tenant-id (str (uuid/make-random-uuid))]
                 (p/subscribe "cxengage/contacts/list-attributes-response" (fn [error topic response]
                                                                             (is (= (get-in api-resp [:api-response :result]) (js->clj response :keywordize-keys true)))
                                                                             (done)))
                 (a/>! the-chan api-resp)
                 (set! iu/api-request (fn [request-body preserve-casing]
                                        the-chan))
                 (state/set-active-tenant! {:tenant-id tenant-id})
                 (let [contact-id (str (uuid/make-random-uuid))]
                   (contacts/list-attributes)
                   (set! contacts/contact-request old)))))))

(deftest get-layout-test
  (testing "Contact module get layout contact function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)
          attribute-id-1 (uuid/make-random-uuid)
          attribute-id-2 (uuid/make-random-uuid)
          old contacts/contact-request]
      (set! contacts/contact-request (fn [url body method params topic-key spec module]
                                       (let [layout-id (get-in url [:params :layout-id])]
                                         (when (and method url params topic-key spec module)
                                           {:description ""
                                            :layout [{:label {:en-US "1"}
                                                      :attributes [attribute-id-1]}
                                                     [{:label {:en-US "2"}
                                                       :attributes [attribute-id-2]}]]
                                            :updated date-time
                                            :name "basic"
                                            :id layout-id
                                            :created date-time}))))
      (let [layout-id (str (uuid/make-random-uuid))
            tenant-id (str (uuid/make-random-uuid))
            params {:layoutId layout-id}
            get-response (contacts/get-layout params)
            get-response-4 (contacts/get-layout params (fn [] "blah"))]
        (is (= {:description ""
                :layout [{:label {:en-US "1"}
                          :attributes [attribute-id-1]}
                         [{:label {:en-US "2"}
                           :attributes [attribute-id-2]}]]
                :updated date-time
                :name "basic"
                :id layout-id
                :created date-time} get-response))
        (is (= {:description ""
                :layout [{:label {:en-US "1"}
                          :attributes [attribute-id-1]}
                         [{:label {:en-US "2"}
                           :attributes [attribute-id-2]}]]
                :updated date-time
                :name "basic"
                :id layout-id
                :created date-time} get-response-4))
        (set! contacts/contact-request old)))))

(deftest list-layouts-test
  (testing "Contact module list attributes function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)
          layout-id (uuid/make-random-uuid)
          layout-id-2 (uuid/make-random-uuid)
          attribute-id-1 (uuid/make-random-uuid)
          attribute-id-2 (uuid/make-random-uuid)
          old contacts/contact-request]
      (set! contacts/contact-request (fn [url body method params topic-key spec module preserve?]
                                       (when (and method url params topic-key spec module)
                                         [{:description ""
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
                                           :created date-time}])))
      (let [contact-id (str (uuid/make-random-uuid))
            tenant-id (str (uuid/make-random-uuid))
            params {}
            list-response (contacts/list-layouts params)
            list-response-2 (contacts/list-layouts)
            list-response-4 (contacts/list-layouts params (fn [] "blah"))]
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
                 :created date-time}] list-response))
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
                 :created date-time}] list-response-4))
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
                 :created date-time}] list-response-2))
        (set! contacts/contact-request old)))))
