(ns cxengage-javascript-sdk.modules.contacts
  (:require-macros [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]])
  (:require [cljs.core.async :as a]
            [cljs.spec.alpha :as s]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.specs :as specs]))

(defn- get-query-str
  [query]
  (let [queryv (->> query
                    (reduce-kv (fn [acc k v] (conj acc (name k) "=" v "&")) [])
                    (pop)
                    (into ["?"]))]
    (clojure.string/join queryv)))

;; -------------------------------------------------------------------------- ;;
;; Cxengage.contacts.get({contactId: "{{contact-id}}" });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-contact-params
  (s/keys :req-un [::specs/contact-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-contact
  ""
  {:validation ::get-contact-params
   :topic-key :get-contact}
  [params]
  (let [{:keys [contact-id callback topic]} params
        {:keys [api-response status] :as contacts-response} (a/<! (rest/get-contact-request contact-id))
        retrieved-contact (get api-response "result")]
    (if (= status 200)
      (p/publish {:topics topic
                  :response retrieved-contact
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-get-contact-err contact-id contacts-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; Cxengage.contacts.getAll();
;; -------------------------------------------------------------------------- ;;

(s/def ::get-contacts-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn get-contacts
  ""
  {:validation ::get-contacts-params
   :topic-key :get-contacts}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [api-response status] :as contacts-response} (a/<! (rest/get-contacts-request))
        retrieved-contacts (get api-response "result")]
    (if (= status 200)
      (p/publish {:topics topic
                  :response retrieved-contacts
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-list-all-contacts-err contacts-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; // Search on a specified attribute
;; CxEngage.contacts.search({query: {name: "Serenova"}});
;;
;; // Search all attributes for value by using the keyword q
;; CxEngage.contacts.search({query: {q: "Serenova"}});
;; -------------------------------------------------------------------------- ;;

(s/def ::search-contacts-params
  (s/keys :req-un [::specs/query]
          :opt-un [::specs/callback]))

(def-sdk-fn search-contacts
  ""
  {:validation ::search-contacts-params
   :topic-key :search-contacts
   :stringify-keys? true}
  [params]
  (let [{:keys [query callback topic]} params
        query-str (get-query-str query)
        {:keys [api-response status] :as contacts-response} (a/<! (rest/search-contacts-request query-str))
        found-contacts (get api-response "result")]
    (if (= status 200)
      (p/publish {:topics topic
                  :response found-contacts
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-search-contacts-err query contacts-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.contacts.create({attributes: {name: "Serenova, LLC."}});
;; -------------------------------------------------------------------------- ;;

(s/def ::create-contact-params
  (s/keys :req-un [::specs/attributes]
          :opt-un [::specs/callback]))

(def-sdk-fn create-contact
  ""
  {:validation ::create-contact-params
   :topic-key :create-contact
   :stringify-keys? true}
  [params]
  (let [{:keys [attributes callback topic]} params
        {:keys [api-response status] :as contacts-response} (a/<! (rest/create-contact-request {:attributes attributes}))
        created-contact (get api-response "result")]
    (if (= status 200)
      (p/publish {:topics topic
                  :response created-contact
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-create-contact-err attributes contacts-response)
                  :callback callback}))))

;; ----------------------------------------------------------------------------------------------- ;;
;; CxEngage.contacts.update({contactId: "{{contact-id}}", attributes: {name: "Serenova, LLC."}});
;; ----------------------------------------------------------------------------------------------- ;;

(s/def ::update-contact-params
  (s/keys :req-un [::specs/contactId
                   ::specs/attributes]
          :opt-un [::specs/callback]))

(def-sdk-fn update-contact
  ""
  {:validation ::update-contact-params
   :topic-key :update-contact
   :stringify-keys? true}
  [params]
  (let [{:keys [attributes contactId callback topic]} params
        {:keys [api-response status] :as contacts-response} (a/<! (rest/update-contact-request contactId {:attributes attributes}))
        updated-contact (get api-response "result")]
    (if (= status 200)
      (p/publish {:topics topic
                  :response updated-contact
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-update-contact-err contactId attributes contacts-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.contacts.delete({contactId: "{{contact-id}}"});
;; -------------------------------------------------------------------------- ;;

(s/def ::delete-contact-params
  (s/keys :req-un [::specs/contact-id]
          :opt-un [::specs/callback]))

(def-sdk-fn delete-contact
  ""
  {:validation ::delete-contact-params
   :topic-key :delete-contact}
  [params]
  (let [{:keys [contact-id callback topic]} params
        {:keys [api-response status] :as contacts-response} (a/<! (rest/delete-contact-request contact-id))
        deleted-contact? (:result api-response)]
    (if (= status 200)
      (p/publish {:topics topic
                  :response deleted-contact?
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-delete-contact-err contact-id contacts-response)
                  :callback callback}))))

;; ----------------------------------------------------------------------------------------------------------------- ;;
;; CxEngage.contacts.merge({contactIds: ["{{contact-id}}", "{{contact-id}}"], attributes: {name: "Serenova, LLC."}});
;; ----------------------------------------------------------------------------------------------------------------- ;;

(s/def ::merge-contacts-params
  (s/keys :req-un [::specs/contactIds
                   ::specs/attributes]
          :opt-un [::specs/callback]))

(def-sdk-fn merge-contacts
  ""
  {:validation ::merge-contacts-params
   :topic-key :merge-contacts
   :stringify-keys? true}
  [params]
  (let [{:keys [contactIds attributes callback topic]} params
        {:keys [api-response status] :as contacts-response} (a/<! (rest/merge-contact-request {:contactIds contactIds
                                                                                               :attributes attributes}))
        merged-contact (get api-response "result")]
    (if (= status 200)
      (p/publish {:topics topic
                  :response merged-contact
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-merge-contacts-err contactIds contacts-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.contacts.listAttributes();
;; -------------------------------------------------------------------------- ;;

(s/def ::list-attributes-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn list-attributes
  ""
  {:validation ::list-attributes-params
   :topic-key :list-attributes}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [api-response status] :as attributes-response} (a/<! (rest/list-attributes-request))
        retrieved-attributes (:result api-response)]
    (if (= status 200)
      (p/publish {:topics topic
                  :response retrieved-attributes
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-list-contact-attributes-err attributes-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.contacts.getLayout({layoutId: "{{layout-id}}"});
;; -------------------------------------------------------------------------- ;;

(s/def ::get-layout-params
  (s/keys :req-un [::specs/layout-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-layout
  ""
  {:validation ::get-layout-params
   :topic-key :get-layout}
  [params]
  (let [{:keys [layout-id callback topic]} params
        {:keys [api-response status] :as layouts-response} (a/<! (rest/get-layout-request layout-id))
        retrieved-layout (:result api-response)]
    (if (= status 200)
      (p/publish {:topics topic
                  :response retrieved-layout
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-retrieve-contact-layout-err layout-id layouts-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.contacts.listLayouts();
;; -------------------------------------------------------------------------- ;;

(s/def ::list-layouts-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn list-layouts
  ""
  {:validation ::list-layouts-params
   :topic-key :list-layouts}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [api-response status] :as layouts-response} (a/<! (rest/list-layouts-request))
        retrieved-layouts (:result api-response)]
    (if (= status 200)
      (p/publish {:topics topic
                  :response retrieved-layouts
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-retrieve-contact-layouts-list-err layouts-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; SDK Contacts Module
;; -------------------------------------------------------------------------- ;;

(defrecord ContactsModule []
  pr/SDKModule
  (start [this]
    (let [module-name :contacts]
      (ih/register {:api {module-name {:get get-contact
                                       :get-all get-contacts
                                       :search search-contacts
                                       :create create-contact
                                       :update update-contact
                                       :delete delete-contact
                                       :merge merge-contacts
                                       :list-attributes list-attributes
                                       :get-layout get-layout
                                       :list-layouts list-layouts}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
