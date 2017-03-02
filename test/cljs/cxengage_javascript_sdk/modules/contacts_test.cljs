(ns cxengage-javascript-sdk.modules.contacts-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.modules.contacts :as contacts]
            [cljs.core.async :as a]
            [cljs.spec :as s]
            [cljs-uuid-utils.core :as uuid]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.modules.modules :as m]
            [cljs.test :refer-macros [deftest is testing run-tests async use-fixtures]]))

(deftest contact-request
  (testing "The contact request function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)]
      (with-redefs [iu/api-request (fn [request-map]
                                     (let [{:keys [url body method]} request-map
                                           contact-id (peek (clojure.string/split url #"[/]+"))
                                           resp-chan (a/promise-chan)]
                                       (when (and method url)
                                         (case method
                                           :get (a/put! resp-chan {:api-response {:result {:id contact-id
                                                                                           :attributes {:name "Ian Bishop"
                                                                                                        :mobile "+15554442222"
                                                                                                        :age 27}
                                                                                           :created date-time
                                                                                           :createdBy fake-user
                                                                                           :updated date-time
                                                                                           :updatedBy fake-user}}
                                                                   :status 200})
                                           (a/put! resp-chan {})))
                                       resp-chan))]
        (let [contact-id (str (uuid/make-random-uuid))
              tenant-id (str (uuid/make-random-uuid))
              url (str "v1/tenants/" tenant-id "/contacts/" contact-id)
              response (contacts/contact-request url nil :get)
              contacts-module (contacts/map->ContactsModule. (m/gen-new-initial-module-config (a/chan)))
              _ (s/def ::contact-params
                  (s/keys :req-un [:specs/contact-id]
                          :opt-un [:specs/callback]))
              url-map {:base :single-contact-url
                       :params {:tenant-id tenant-id
                                :contact-id contact-id}}
              important-response (contacts/contact-request url-map nil :get {:contact-id contact-id} :get-contact ::contact-params contacts-module)]
          (async done
                 (go
                   (is (= {:api-response {:result {:id contact-id
                                                   :attributes {:name "Ian Bishop"
                                                                :mobile "+15554442222"
                                                                :age 27}
                                                   :created date-time
                                                   :createdBy fake-user
                                                   :updated date-time
                                                   :updatedBy fake-user}}
                           :status 200} (a/<! response)))
                   (is (= nil important-response))
                   (done))))))))

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
          date-time (.toISOString date)]
      (with-redefs [contacts/contact-request (fn [url body method params topic-key spec module]
                                               (let [contact-id (get-in url [:params :contact-id])]
                                                 (when (and method url params topic-key spec module)
                                                   {:id contact-id
                                                    :attributes {:name "Ian Bishop"
                                                                 :mobile "+15554442222"
                                                                 :age 27}
                                                    :created date-time
                                                    :createdBy fake-user
                                                    :updated date-time
                                                    :updatedBy fake-user})))]
        (let [contact-id (str (uuid/make-random-uuid))
              tenant-id (str (uuid/make-random-uuid))
              params {:contact-id contact-id}
              contacts-module (contacts/map->ContactsModule. (m/gen-new-initial-module-config (a/chan)))
              get-response (contacts/get-contact contacts-module params)
              get-response-2 (contacts/get-contact contacts-module)
              get-response-3 (contacts/get-contact contacts-module {} "")
              get-response-4 (contacts/get-contact contacts-module params (fn [] "blah"))]
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
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} get-response-2))
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} get-response-3)))))))

(deftest get-contacts-test
  (testing "Contact module search contactsmodule function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)
          contact-id (str (uuid/make-random-uuid))]
      (with-redefs [contacts/contact-request (fn [url body method params topic-key spec module]
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
                                                             :updatedBy fake-user}]}))]
        (let [tenant-id (str (uuid/make-random-uuid))
              params {:tenant-id tenant-id}
              contacts-module (contacts/map->ContactsModule. (m/gen-new-initial-module-config (a/chan)))
              search-response (contacts/get-contacts contacts-module params)
              search-response-2 (contacts/get-contacts contacts-module)
              search-response-3 (contacts/get-contacts contacts-module {} "")
              search-response-4 (contacts/get-contacts contacts-module params (fn [] "blah"))]
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
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} search-response-2))
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} search-response-3)))))))

(deftest search-contacts-test
  (testing "Contact module search contactsmodule function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)
          contact-id (str (uuid/make-random-uuid))]
      (with-redefs [contacts/contact-request (fn [url body method params topic-key spec module query]
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
                                                             :updatedBy fake-user}]}))]
        (let [tenant-id (str (uuid/make-random-uuid))
              params {:tenant-id tenant-id
                      :query {:name "Ian"}}
              contacts-module (contacts/map->ContactsModule. (m/gen-new-initial-module-config (a/chan)))
              search-response (contacts/search-contacts contacts-module params)
              search-response-2 (contacts/search-contacts contacts-module)
              search-response-3 (contacts/search-contacts contacts-module {} "")
              search-response-4 (contacts/search-contacts contacts-module params (fn [] "blah"))]
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
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} search-response-2))
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} search-response-3)))))))

(deftest create-contact-test
  (testing "Contact module create contact function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)]
      (with-redefs [contacts/contact-request (fn [url body method params topic-key spec module]
                                               (let [{:keys [attributes]} body
                                                     contact-id (uuid/make-random-uuid)]
                                                 (when (and method body url params topic-key spec module)
                                                   {:id contact-id
                                                    :attributes attributes
                                                    :created date-time
                                                    :createdBy fake-user
                                                    :updated date-time
                                                    :updatedBy fake-user})))]
        (let [contact-id (str (uuid/make-random-uuid))
              tenant-id (str (uuid/make-random-uuid))
              params {:tenant-id tenant-id
                      :attributes {:name "Ian Bishop"
                                   :mobile "+15554442222"
                                   :age 27}}
              contacts-module (contacts/map->ContactsModule. (m/gen-new-initial-module-config (a/chan)))
              create-response (contacts/create-contact contacts-module params)
              create-response-2 (contacts/create-contact contacts-module)
              create-response-3 (contacts/create-contact contacts-module {} "")
              create-response-4 (contacts/create-contact contacts-module params (fn [] "blah"))]
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
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} create-response-2))
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} create-response-3)))))))

(deftest update-contact-test
  (testing "Contact module update contact function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)]
      (with-redefs [contacts/contact-request (fn [url body method params topic-key spec module]
                                               (let [{:keys [attributes]} body
                                                     contact-id (get-in url [:params :contact-id])]
                                                 (when (and method body url params topic-key spec module)
                                                   {:id contact-id
                                                    :attributes attributes
                                                    :created date-time
                                                    :createdBy fake-user
                                                    :updated date-time
                                                    :updatedBy fake-user})))]
        (let [contact-id (str (uuid/make-random-uuid))
              tenant-id (str (uuid/make-random-uuid))
              params {:contact-id contact-id
                      :attributes {:name "Ian Bishop"
                                   :mobile "+15554442222"
                                   :age 27}}
              contacts-module (contacts/map->ContactsModule. (m/gen-new-initial-module-config (a/chan)))
              update-response (contacts/update-contact contacts-module params)
              update-response-2 (contacts/update-contact contacts-module)
              update-response-3 (contacts/update-contact contacts-module {} "")
              update-response-4 (contacts/update-contact contacts-module params (fn [] "blah"))]
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
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} update-response-2))
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} update-response-3)))))))

(deftest delete-contact-test
  (testing "Contact module delete contact function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)]
      (with-redefs [contacts/contact-request (fn [url body method params topic-key spec module]
                                               (let [contact-id (get-in url [:params :contact-id])]
                                                 (when (and contact-id method url params topic-key spec module)
                                                   true)))]
        (let [contact-id (str (uuid/make-random-uuid))
              tenant-id (str (uuid/make-random-uuid))
              params {:contact-id contact-id}
              contacts-module (contacts/map->ContactsModule. (m/gen-new-initial-module-config (a/chan)))
              delete-response (contacts/delete-contact contacts-module params)
              delete-response-2 (contacts/delete-contact contacts-module)
              delete-response-3 (contacts/delete-contact contacts-module {} "")
              delete-response-4 (contacts/delete-contact contacts-module params (fn [] "blah"))]

          (is (true? delete-response))
          (is (true? delete-response-4))
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} delete-response-2))
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} delete-response-3)))))))

(deftest list-attributes-test
  (testing "Contact module list attributes function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)]
      (with-redefs [contacts/contact-request (fn [url body method params topic-key spec module]
                                               (when (and method url params topic-key spec module)
                                                 [{:mandatory false :updated "2017-01-30T16:10:20Z" :default "" :type "text" :created "2017-01-30T16:10:20Z" :active true :label {:en-US "Name"} :object-name "name"}
                                                  {:mandatory true :updated "2017-01-30T16:10:20Z" :default "" :type "text" :created "2017-01-30T16:10:20Z" :active true :label {:en-US "Phone"} :object-name "phone"}]))]
        (let [contact-id (str (uuid/make-random-uuid))
              tenant-id (str (uuid/make-random-uuid))
              params {}
              contacts-module (contacts/map->ContactsModule. (m/gen-new-initial-module-config (a/chan)))
              list-response (contacts/list-attributes contacts-module params)
              list-response-2 (contacts/list-attributes contacts-module)
              list-response-3 (contacts/list-attributes contacts-module {} "")
              list-response-4 (contacts/list-attributes contacts-module params (fn [] "blah"))]
          (is (= [{:mandatory false :updated "2017-01-30T16:10:20Z" :default "" :type "text" :created "2017-01-30T16:10:20Z" :active true :label {:en-US "Name"} :object-name "name"}
                  {:mandatory true :updated "2017-01-30T16:10:20Z" :default "" :type "text" :created "2017-01-30T16:10:20Z" :active true :label {:en-US "Phone"} :object-name "phone"}]  list-response))
          (is (= [{:mandatory false :updated "2017-01-30T16:10:20Z" :default "" :type "text" :created "2017-01-30T16:10:20Z" :active true :label {:en-US "Name"} :object-name "name"}
                  {:mandatory true :updated "2017-01-30T16:10:20Z" :default "" :type "text" :created "2017-01-30T16:10:20Z" :active true :label {:en-US "Phone"} :object-name "phone"}] list-response-4))
          (is (= [{:mandatory false :updated "2017-01-30T16:10:20Z" :default "" :type "text" :created "2017-01-30T16:10:20Z" :active true :label {:en-US "Name"} :object-name "name"}
                  {:mandatory true :updated "2017-01-30T16:10:20Z" :default "" :type "text" :created "2017-01-30T16:10:20Z" :active true :label {:en-US "Phone"} :object-name "phone"}] list-response-2))
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} list-response-3)))))))

(deftest get-layout-test
  (testing "Contact module get layout contact function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)
          attribute-id-1 (uuid/make-random-uuid)
          attribute-id-2 (uuid/make-random-uuid)]
      (with-redefs [contacts/contact-request (fn [url body method params topic-key spec module]
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
                                                    :created date-time})))]
        (let [layout-id (str (uuid/make-random-uuid))
              tenant-id (str (uuid/make-random-uuid))
              params {:layout-id layout-id}
              contacts-module (contacts/map->ContactsModule. (m/gen-new-initial-module-config (a/chan)))
              get-response (contacts/get-layout contacts-module params)
              get-response-2 (contacts/get-layout contacts-module)
              get-response-3 (contacts/get-layout contacts-module {} "")
              get-response-4 (contacts/get-layout contacts-module params (fn [] "blah"))]
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
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} get-response-2))
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} get-response-3)))))))

(deftest list-layouts-test
  (testing "Contact module list attributes function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)
          layout-id (uuid/make-random-uuid)
          layout-id-2 (uuid/make-random-uuid)
          attribute-id-1 (uuid/make-random-uuid)
          attribute-id-2 (uuid/make-random-uuid)]
      (with-redefs [contacts/contact-request (fn [url body method params topic-key spec module]
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
                                                   :created date-time}]))]
        (let [contact-id (str (uuid/make-random-uuid))
              tenant-id (str (uuid/make-random-uuid))
              params {}
              contacts-module (contacts/map->ContactsModule. (m/gen-new-initial-module-config (a/chan)))
              list-response (contacts/list-attributes contacts-module params)
              list-response-2 (contacts/list-attributes contacts-module)
              list-response-3 (contacts/list-attributes contacts-module {} "")
              list-response-4 (contacts/list-attributes contacts-module params (fn [] "blah"))]
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
                   :created date-time}]  list-response))
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
          (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} list-response-3)))))))
