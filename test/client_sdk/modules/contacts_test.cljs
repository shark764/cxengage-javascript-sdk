(ns client-sdk.modules.contacts-test
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.test :refer-macros [deftest is testing run-tests async use-fixtures]]
            [cljs.core.async :as a]
            [client-sdk-utils.core :as u]
            [client-sdk.modules.contacts :as contacts]
            [cljs-uuid-utils.core :as uuid]))



(deftest contact-request-and-delete-contact-test
  (testing "The contact request function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)]
      (with-redefs [u/api-request (fn [request-map]
                                    (let [{:keys [url method token resp-chan]} request-map
                                          contact-id (peek (clojure.string/split url #"[/]+"))]
                                      (when (and method url resp-chan)
                                        (case method
                                          :get (a/put! resp-chan {:result {:id contact-id
                                                                      :attributes {:name "Ian Bishop"
                                                                                   :mobile "+15554442222"
                                                                                   :age 27}
                                                                      :created date-time
                                                                      :createdBy fake-user
                                                                      :updated date-time
                                                                      :updatedBy fake-user}})
                                          (a/put! resp-chan {})))))]
        (let [resp (a/promise-chan)
              resp-chan (a/promise-chan)
              contact-id (str (uuid/make-random-uuid))
              tenant-id (str (uuid/make-random-uuid))
              delete-response (a/promise-chan)
              _ (swap! contacts/module-state assoc :env "dev")
              url (u/api-url (:env @contacts/module-state) (str "/tenants/" tenant-id "/contacts/" contact-id))]
          (client-sdk.modules.contacts/contact-request url nil :get "auoki;.paoe" resp resp-chan)
          (async done
                 (go
                   (is (= (a/<! resp-chan) {:id contact-id
                                            :attributes {:name "Ian Bishop"
                                                         :mobile "+15554442222"
                                                         :age 27}
                                            :created date-time
                                            :createdBy fake-user
                                            :updated date-time
                                            :updatedBy fake-user}))
                   
                   (done))))))))


(deftest get-query-str-test
  (testing "The query string builder"
    (let [query [:q "name"
                 :mobile "+15554442222"
                 :limit "5"
                 :page 0]]
      (is (= "?q=name&mobile=+15554442222&limit=5&page=0" (client-sdk.modules.contacts/get-query-str query))))))

(deftest get-contact-test
  (testing "Contact module get contact function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)]
      (with-redefs [contacts/contact-request (fn [url body method token resp resp-chan]
                                               (let [contact-id (peek (clojure.string/split url #"[/]+"))]
                                                 (when (and method url token resp-chan)
                                                   (a/put! resp-chan{:id contact-id
                                                                     :attributes {:name "Ian Bishop"
                                                                                  :mobile "+15554442222"
                                                                                  :age 27}
                                                                     :created date-time
                                                                     :createdBy fake-user
                                                                     :updated date-time
                                                                     :updatedBy fake-user}))))]
        
        (let [resp (a/promise-chan)
              contact-id (str (uuid/make-random-squuid))]
          (swap! contacts/module-state assoc :env "dev")
          (contacts/get-contact {:token "asoikthgcda;.pslhtri"
                                 :contact-id contact-id
                                 :tenant-id (str (uuid/make-random-squuid))
                                 :resp-chan resp})
          (async done
                 (go
                   (is (= (a/<! resp) {:id contact-id
                                       :attributes {:name "Ian Bishop"
                                                    :mobile "+15554442222"
                                                    :age 27}
                                       :created date-time
                                       :createdBy fake-user
                                       :updated date-time
                                       :updatedBy fake-user}))
                   (done))))))))

(deftest list-contacts-test
  (testing "Contact module list contact function"
    (let [fake-user (str (uuid/make-random-squuid))
          date (js/Date.)
          date-time (.toISOString date)
          resp (a/promise-chan)
          resp2 (a/promise-chan)]
      (with-redefs [contacts/contact-request (fn [url body method token resp resp-chan]
                                              (let [contact-id (peek (clojure.string/split url #"[/]+"))]
                                                (when (and method url resp-chan token)
                                                  (if (< 1 (count (clojure.string/split url #"[&]+")))
                                                    (a/put! resp-chan [{:id "c2260887-8231-45f3-83f2-729f5d63f313"
                                                                       :attributes {:name "Ian Bishop"
                                                                                    :mobile "+15554442222"
                                                                                    :age 27}
                                                                       :created date-time
                                                                       :createdBy fake-user
                                                                       :updated date-time
                                                                       :updatedBy fake-user}
                                                                      {:id "c2260887-8231-45f3-83f2-729f5d63f313"
                                                                       :attributes {:name "Sam Stiles"
                                                                                    :mobile "+15554442222"}
                                                                       :created date-time
                                                                       :createdBy fake-user
                                                                       :updated date-time
                                                                       :updatedBy fake-user}])
                                                    (a/put! resp-chan  [{:id "c2260887-8231-45f3-83f2-729f5d63f313"
                                                                         :attributes {:name "Ian Bishop"
                                                                                      :mobile "+15554442222"
                                                                                      :age 27}
                                                                         :created date-time
                                                                         :createdBy fake-user
                                                                         :updated date-time
                                                                         :updatedBy fake-user}
                                                                        {:id "c2260887-8231-45f3-83f2-729f5d63f313"
                                                                         :attributes {:name "Sam Stiles"}
                                                                         :created date-time
                                                                         :createdBy fake-user
                                                                         :updated date-time
                                                                         :updatedBy fake-user}])))))]
        (swap! contacts/module-state assoc :env "dev")
        (contacts/list-contacts {:token "asoetnibk{lraui"
                                 :tenant-id (str (uuid/make-random-squuid))
                                 :resp-chan resp})
        (contacts/list-contacts {:token "wtahodxeubhtna"
                                 :tenant-id (str (uuid/make-random-squuid))
                                 :resp-chan resp2
                                 :query [:q "name" :mobile "+15554442222" :limit "1" :page 0]})
        (async done
               (go
                 (is (= (a/<! resp) [{:id "c2260887-8231-45f3-83f2-729f5d63f313"
                                      :attributes {:name "Ian Bishop"
                                                   :mobile "+15554442222"
                                                   :age 27}
                                      :created date-time
                                      :createdBy fake-user
                                      :updated date-time
                                      :updatedBy fake-user}
                                     {:id "c2260887-8231-45f3-83f2-729f5d63f313"
                                      :attributes {:name "Sam Stiles"}
                                      :created date-time
                                      :createdBy fake-user
                                      :updated date-time
                                      :updatedBy fake-user}]))
                 (is (= (a/<! resp2) [{:id "c2260887-8231-45f3-83f2-729f5d63f313"
                                      :attributes {:name "Ian Bishop"
                                                   :mobile "+15554442222"
                                                   :age 27}
                                      :created date-time
                                      :createdBy fake-user
                                      :updated date-time
                                      :updatedBy fake-user}
                                     {:id "c2260887-8231-45f3-83f2-729f5d63f313"
                                      :attributes {:name "Sam Stiles"
                                                   :mobile "+15554442222"}
                                      :created date-time
                                      :createdBy fake-user
                                      :updated date-time
                                      :updatedBy fake-user}]))
                 (done)))))))

(deftest create-update-contact-test
  (let [fake-user (str (uuid/make-random-squuid))
        date (js/Date.)
        date-time (.toISOString date)]
    (with-redefs [contacts/contact-request (fn [url body method token resp resp-chan]
                                             (when (and url method resp-chan)
                                                   (case method
                                                     :post (a/put! resp-chan {:id (str (uuid/make-random-squuid))
                                                                              :attributes (:attributes body)
                                                                              :created date-time
                                                                              :createdBy fake-user
                                                                              :updated date-time
                                                                              :updatedBy fake-user})
                                                     :put (a/put! resp-chan {:id (peek (clojure.string/split url #"[/]+"))
                                                                             :attributes (:attributes body)
                                                                             :created date-time
                                                                             :createdBy fake-user
                                                                             :updated date-time
                                                                             :updatedBy fake-user})
                                                     :delete (a/put! resp-chan {:id (peek (clojure.string/split url #"[/]+"))
                                                                                :attributes {:name "Bob Saget"}
                                                                                :created date-time
                                                                                :createdBy fake-user
                                                                                :updated date-time
                                                                                :updatedBy fake-user})
                                                     (a/put! resp-chan {}))))]
      (testing "The update contact function"
        (let [create-response (a/promise-chan)
              update-response (a/promise-chan)
              delete-response (a/promise-chan)
              contact-id (str (uuid/make-random-squuid))]
          (swap! contacts/module-state assoc :env "dev")
          (contacts/create-contact {:token "tastao,;.py.p"
                                    :tenant-id (str (uuid/make-random-squuid))
                                    :resp-chan create-response
                                    :attributes {:name "Bob Saget"}})
          (contacts/update-contact {:token "ast.hpxkiubd"
                                    :contact-id contact-id
                                    :tenant-id (str (uuid/make-random-squuid))
                                    :resp-chan update-response
                                    :attributes {:name "Bob Saget"
                                                 :age 55}})
          (contacts/delete-contact {:contact-id contact-id
                                    :tenant-id (str (uuid/make-random-squuid))
                                    :token "sbkl;r.punstah"
                                    :method :delete
                                    :resp-chan delete-response})
          (async done
                 (go
                   (is (= (:attributes (a/<! create-response)) {:name "Bob Saget"}))
                   (is (= (:attributes (a/<! update-response)) {:name "Bob Saget"
                                                                :age 55}))
                   (is (= (:attributes (a/<! delete-response)) {:name "Bob Saget"}))
                   (done))))))))
