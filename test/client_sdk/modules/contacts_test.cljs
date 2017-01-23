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

(deftest attributes-test
  (let [fake-user (str (uuid/make-random-squuid))
        date (js/Date.)
        date-time (.toISOString date)
        generic-attribute-one {:id (str (uuid/make-random-squuid))
                               :label {:en-US "Name"
                                       :fr "Nom"}
                               :objectName "name"
                               :type "text"
                               :mandatory true
                               :default "Jane Doe"}
        generic-attribute-two {:id (str (uuid/make-random-squuid))
                               :label {:en-US "Email"
                                       :fr "email"}
                               :objectName "email"
                               :type "text"
                               :mandatory false}
        dummy-id (str (uuid/make-random-squuid))]
    (with-redefs [contacts/contact-request (fn [url body method token resp resp-chan]
                                             (let [attribute-id (peek (clojure.string/split url #"[/]+"))]
                                               (when (and url method resp-chan token)
                                                 (cond
                                                   (and (= method :get) (uuid/valid-uuid? attribute-id)) (a/put! resp-chan {:id attribute-id
                                                                                                                            :label {:en-US "Name"
                                                                                                                                    :fr "Nom"}
                                                                                                                            :objectName "name"
                                                                                                                            :type "text"
                                                                                                                            :mandatory true
                                                                                                                            :default "Jane Doe"})
                                                   (= method :get) (a/put! resp-chan [generic-attribute-one
                                                                                      generic-attribute-two])
                                                   (= method :post) (when-let [{:keys [label objectName type mandatory default]} body]
                                                                      (a/put! resp-chan (cond-> {:id dummy-id
                                                                                                 :label label
                                                                                                 :objectName objectName
                                                                                                 :type type}
                                                                                                mandatory (assoc :mandatory mandatory)
                                                                                                default (assoc :default default))))
                                                   (= method :put) (when-let [{:keys [label default mandatory]} body]
                                                                     (a/put! resp-chan (cond-> {:id attribute-id}
                                                                                               label (assoc :label label)
                                                                                               default (assoc :default default)
                                                                                               (not (nil? mandatory)) (assoc :mandatory mandatory))))
                                                   
                                                   :default (a/put! resp-chan {})))))]
      (testing "Get, list, create, and update, attribute functions"
        (let [get-response (a/promise-chan)
              list-response (a/promise-chan)
              create-response (a/promise-chan)
              create-response-2 (a/promise-chan)
              update-response (a/promise-chan)
              request-template {:token "imaginearealtokenhere"
                                :tenant-id (str (uuid/make-random-squuid))}
              attribute-id (str (uuid/make-random-squuid))]
          (swap! contacts/module-state assoc :env "dev")
          (contacts/get-attribute (-> request-template
                                      (assoc :attribute-id attribute-id)
                                      (assoc :resp-chan get-response)))
          (contacts/list-attributes (-> request-template
                                       (assoc :resp-chan list-response)))
          (contacts/create-attribute (-> request-template
                                          (assoc :label {:en-US "Name"
                                                         :fr "Nom"})
                                          (assoc :object-name "name")
                                          (assoc :type "text")
                                          (assoc :resp-chan create-response)))
          (contacts/create-attribute (-> request-template
                                          (assoc :label {:en-US "Name"
                                                         :fr "Nom"})
                                          (assoc :object-name "name")
                                          (assoc :type "text")
                                          (assoc :mandatory true)
                                          (assoc :default "Jane Doe")
                                          (assoc :resp-chan create-response-2)))
          (contacts/update-attribute (-> request-template
                                         (assoc :attribute-id attribute-id)
                                         (assoc :label {:en-US "Name"
                                                        :fr-FR "Nom"})
                                         (assoc :default "John Doe")
                                         (assoc :mandatory false)
                                         (assoc :resp-chan update-response)))
          (async done
                 (go
                   (let [get-resp (a/<! get-response)
                         id (:id get-resp)
                         [first-attribute second-attribute] (a/<! list-response)
                         create-resp (a/<! create-response)
                         create-resp-2 (a/<! create-response-2)
                         update-resp (a/<! update-response)]
                     (is (= id attribute-id))
                     (is (= get-resp {:id attribute-id
                                      :label {:en-US "Name"
                                              :fr "Nom"}
                                      :objectName "name"
                                      :type "text"
                                      :mandatory true
                                      :default "Jane Doe"}))
                     (is (= first-attribute generic-attribute-one))
                     (is (= second-attribute generic-attribute-two))
                     (is (= create-resp {:id dummy-id
                                         :label {:en-US "Name"
                                                 :fr "Nom"}
                                         :objectName "name"
                                         :type "text"}))
                     (is (= create-resp-2 {:id dummy-id
                                           :label {:en-US "Name"
                                                   :fr "Nom"}
                                           :objectName "name"
                                           :type "text"
                                           :mandatory true
                                           :default "Jane Doe"}))
                     (is (= update-resp {:id attribute-id
                                         :label {:en-US "Name"
                                                 :fr-FR "Nom"}
                                         :default "John Doe"
                                         :mandatory false})))
                   (done))))))))

(deftest layouts-test
   (let [fake-user (str (uuid/make-random-squuid))
         date (js/Date.)
         date-time (.toISOString date)
         generic-layout-one {:id (str (uuid/make-random-squuid))
                             :layout {}
                             :active true
                             :created date-time
                             :createdBy fake-user
                             :updated date-time
                             :updateBy fake-user}
         generic-layout-two {:id (str (uuid/make-random-squuid))
                             :layout {} 
                             :active false
                             :created date-time
                             :createdBy fake-user
                             :updated date-time
                             :updateBy fake-user}
         dummy-id (str (uuid/make-random-squuid))
         attribute-id (str (uuid/make-random-squuid))
         attribute-id-2 (str (uuid/make-random-squuid))]
     (with-redefs [contacts/contact-request (fn [url body method token resp resp-chan]
                                              (let [layout-id (peek (clojure.string/split url #"[/]+"))]
                                                (when (and url method resp-chan token)
                                                  (let [{:keys [layout active description name]} body]
                                                    (cond
                                                     (and (= method :get) (uuid/valid-uuid? layout-id)) (a/put! resp-chan (-> generic-layout-one
                                                                                                                              (assoc :id layout-id)))
                                                     (= method :get) (a/put! resp-chan [generic-layout-one generic-layout-two])
                                                     (= method :post) (a/put! resp-chan (cond-> {:id dummy-id
                                                                                                 :layout layout
                                                                                                 :created date-time
                                                                                                 :createdBy fake-user
                                                                                                 :updated date-time
                                                                                                 :updateBy fake-user}
                                                                                                description (assoc :description description)
                                                                                                (not (nil? active)) (assoc :active active)))
                                                     (= method :put) (a/put! resp-chan (cond-> {:id layout-id
                                                                                                :created date-time
                                                                                                :createdBy fake-user
                                                                                                :updated date-time
                                                                                                :updateBy fake-user}
                                                                                               layout (assoc :layout layout)
                                                                                               (not (nil? active)) (assoc :active active)
                                                                                               name (assoc :name name)
                                                                                               description (assoc :description description)))
                                                     (= method :delete) (a/put! resp-chan (-> generic-layout-one
                                                                                              (assoc :id layout-id)))
                                                     :default (a/put! resp-chan {}))))))]
       (testing "Get, list, create, update, and delete layout functions"
         (let [get-response (a/promise-chan)
               list-response (a/promise-chan)
               create-response (a/promise-chan)
               update-response (a/promise-chan)
               delete-response (a/promise-chan)
               request-template {:token "imaginearealtokenhere"
                                 :tenant-id  (str (uuid/make-random-squuid))}
               layout-id (str (uuid/make-random-squuid))]
           (swap! contacts/module-state assoc :env "dev")
           (contacts/list-layouts (-> request-template
                                      (assoc :resp-chan list-response)))
           (contacts/get-layout (-> request-template
                                    (assoc :resp-chan get-response)
                                    (assoc :layout-id layout-id)))
           (contacts/create-layout (-> request-template
                                       (assoc :layout-id layout-id)
                                       (assoc :resp-chan create-response)
                                       (assoc :labels {:label1 {:en "Contact info"} :label2 {:FR "Contact info"}})
                                       (assoc :attributes [[attribute-id attribute-id-2] []])))
           (contacts/update-layout (-> request-template
                                       (assoc :layout-id layout-id)
                                       (assoc :resp-chan update-response)
                                       (assoc :labels {:label1 {:en "Contact info"} :label2 {:FR "Contact info"}})
                                       (assoc :attributes [[attribute-id attribute-id-2] []])
                                       (assoc :active true)))
           (contacts/delete-layout (-> request-template
                                       (assoc :resp-chan delete-response)
                                       (assoc :layout-id layout-id)))
           (async done
                  (go (let [list-resp (a/<! list-response)
                            get-resp (a/<! get-response)
                            create-resp (a/<! create-response)
                            update-resp (a/<! update-response)
                            delete-resp (a/<! delete-response)]
                        (is (= list-resp [generic-layout-one generic-layout-two]))
                        (is (= get-resp (-> generic-layout-one
                                            (assoc :id layout-id))))
                        (is (= create-resp {:id dummy-id
                                            :layout [{:label {:en "Contact info"} :attributes [attribute-id attribute-id-2]} {:label {:FR "Contact info"} :attributes []}]
                                            :created date-time
                                            :createdBy fake-user
                                            :updated date-time
                                            :updateBy fake-user}))
                        (is (= update-resp {:id layout-id
                                            :layout [{:label {:en "Contact info"} :attributes [attribute-id attribute-id-2]} {:label {:FR "Contact info"} :attributes []}]
                                            :active true
                                            :created date-time
                                            :createdBy fake-user
                                            :updated date-time
                                            :updateBy fake-user}))
                        (is (= delete-resp (-> generic-layout-one
                                               (assoc :id layout-id)))))
                      (done))))))))
