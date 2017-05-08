(ns cxengage-javascript-sdk.modules.contacts
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.state :as state]))

(defn contact-request
  ([url body method params topic-key spec module preserve-casing?]
   (contact-request url body method params topic-key spec module preserve-casing? nil))
  ([url body method params topic-key spec module preserve-casing? query]
   (let [api-url (get-in module [:config :api-url])
         {:keys [callback]} params
         module-state @(:state module)
         base-url (str api-url (get-in module-state [:urls (:base url)]) query)
         request-url (iu/build-api-url-with-params
                      base-url
                      (:params url))
         topic (p/get-topic topic-key)]
     (if-not (s/valid? spec params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data spec params))
                   :callback callback})
       (do (go (let [response (a/<! (contact-request request-url body method preserve-casing?))
                     {:keys [status api-response]} response
                     {:keys [result]} api-response]
                 (if (not= status 200)
                   (p/publish {:topics topic
                               :error (e/api-error api-response)
                               :callback callback})
                   (p/publish {:topics topic
                               :response result
                               :callback callback} true))))
           nil))))
  ([url body method preserve-casing?]
   (let [request-map {:url url
                      :method method}]
     (cond-> request-map
       body (assoc :body body)
       true (iu/api-request preserve-casing?)))))

(defn get-query-str
  [query]
  (let [queryv (->> query
                    (reduce-kv (fn [acc k v] (conj acc (name k) "=" v "&")) [])
                    (pop)
                    (into ["?"]))]
    (clojure.string/join queryv)))


(s/def ::get-contact-params
  (s/keys :req-un [::specs/contactId]
          :opt-un [::specs/callback]))

(defn get-contact
  ([module] (e/wrong-number-of-sdk-fn-args-err))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (get-contact module (merge (ih/extract-params params true) {:callback (first others)}))))
  ([module params]
   (let [{:keys [contactId callback] :as params} (ih/extract-params params true)
         url {:base :single-contact-url
              :params {:tenant-id (state/get-active-tenant-id)
                       :contact-id contactId}}
         method :get]
     (contact-request url nil method params :get-contact ::get-contact-params module true))))

(s/def ::get-contacts-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(defn get-contacts
  ([module]
   (get-contacts module {}))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (get-contacts module (merge (ih/extract-params params true) {:callback (first others)}))))
  ([module params]
   (let [{:keys [callback] :as params} (ih/extract-params params)
         url {:base :multiple-contact-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :get]
     (contact-request url nil method params :get-contacts ::get-contacts-params module true))))

(s/def ::search-contacts-params
  (s/keys :req-un [::specs/query]
          :opt-un [::specs/callback]))

(defn search-contacts
  ([module] (e/wrong-number-of-sdk-fn-args-err))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (search-contacts module (merge (ih/extract-params params true) {:callback (first others)}))))
  ([module params]
   (let [{:keys [query callback] :as params} (ih/extract-params params true)
         url {:base :multiple-contact-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :get
         query (get-query-str query)]
     (contact-request url nil method params :search-contacts ::search-contacts-params module true query))))

(s/def ::create-contact-params
  (s/keys :req-un [::specs/attributes]
          :opt-un [::specs/callback]))

(defn create-contact
  ([module] (e/wrong-number-of-sdk-fn-args-err))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (create-contact module (merge (ih/extract-params params true) {:callback (first others)}))))
  ([module params]
   (let [{:keys [attributes callback] :as params} (ih/extract-params params true)
         url {:base :multiple-contact-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :post
         body {:attributes attributes}]
     (contact-request url body method params :create-contact ::create-contact-params module true))))

(s/def ::update-contact-params
  (s/keys :req-un [::specs/contactId
                   ::specs/attributes]
          :opt-un [::specs/callback]))

(defn update-contact
  ([module] (e/wrong-number-of-sdk-fn-args-err))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (update-contact module (merge (ih/extract-params params true) {:callback (first others)}))))
  ([module params]
   (let [{:keys [attributes contactId callback] :as params} (ih/extract-params params true)
         url {:base :single-contact-url
              :params {:tenant-id (state/get-active-tenant-id)
                       :contact-id contactId}}
         method :put
         body {:attributes attributes}]
     (contact-request url body method params :update-contact ::update-contact-params module true))))

(s/def ::delete-contact-params
  (s/keys :req-un [::specs/contactId]
          :opt-un [::specs/callback]))

(defn delete-contact
  ([module] (e/wrong-number-of-sdk-fn-args-err))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (delete-contact module (merge (ih/extract-params params true) {:callback (first others)}))))
  ([module params]
   (let [{:keys [contactId callback] :as params} (ih/extract-params params true)
         url {:base :single-contact-url
              :params {:tenant-id (state/get-active-tenant-id)
                       :contact-id contactId}}
         method :delete]
     (contact-request url nil method params :delete-contact ::delete-contact-params module true))))

(s/def ::merge-contacts-params
  (s/keys :req-un [::specs/contactIds
                   ::specs/attributes]
          :opt-un [::specs/callback]))

(defn merge-contacts
  ([module] (e/wrong-number-of-sdk-fn-args-err))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (merge-contacts module (merge (ih/extract-params params true) {:callback (first others)}))))
  ([module params]
   (let [{:keys [contactIds attributes callback] :as params} (ih/extract-params params true)
         url {:base :merge-contacts-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :post
         body {:attributes attributes
               :contactIds contactIds}]
     (contact-request url body method params :merge-contacts ::merge-contacts-params module true))))

(s/def ::list-attributes-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(defn list-attributes
  ([module]
   (list-attributes module {}))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (list-attributes module (merge (ih/extract-params params true) {:callback (first others)}))))
  ([module params]
   (let [params (if (fn? params) {:callback params} params)
         {:keys [callback] :as params} (ih/extract-params params true)
         url {:base :multiple-attribute-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :get
         module-state @(:state module)
         api-url (get-in module [:config :api-url])
         base-url (str api-url (get-in module-state [:urls (:base url)]))
         request-url (iu/build-api-url-with-params
                      base-url
                      (:params url))
         topic (p/get-topic :list-attributes)]
     (if-not (s/valid? ::list-attributes-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::list-attributes-params params))
                   :callback callback})
       (let [request-map {:url request-url
                          :method method}]
         (go (let [response (a/<! (iu/api-request request-map true))
                   {:keys [status api-response]} response
                   {:keys [result]} api-response]
               (if (not= status 200)
                 (p/publish {:topics topic
                             :error (e/api-error api-response)
                             :callback callback})
                 (p/publish {:topics topic
                             :response result
                             :callback callback} true))))
         nil)))))

(s/def ::get-layout-params
  (s/keys :req-un [::specs/layoutId]
          :opt-un [::specs/callback]))

(defn get-layout
  ([module] (e/wrong-number-of-sdk-fn-args-err))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (get-layout module (merge (ih/extract-params params true) {:callback (first others)}))))
  ([module params]
   (let [{:keys [layoutId callback] :as params} (ih/extract-params params true)
         url {:base :single-layout-url
              :params {:tenant-id (state/get-active-tenant-id)
                       :layout-id layoutId}}
         method :get]
     (contact-request url nil method params :get-layout ::get-layout-params module true))))

(s/def ::list-layouts-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(defn list-layouts
  ([module]
   (list-layouts module {}))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (list-layouts module (merge (ih/extract-params params true) {:callback (first others)}))))
  ([module params]
   (let [params (if (fn? params) {:callback params} params)
         {:keys [callback] :as params} (ih/extract-params params true)
         url {:base :multiple-layout-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :get]
     (contact-request url nil method params :list-layouts ::list-layouts-params module true))))

(def initial-state
  {:module-name :contacts
   :topics {:get-contact "contacts/get"
            :search-contacts "contacts/search"
            :create-contact "contacts/create"
            :update-contact "contacts/update"
            :delete-contact "contacts/delete"
            :list-attributes "contacts/attributes"
            :get-layout "contact/layouts/get"
            :list-layouts "contacts/layouts"}
   :urls {:single-contact-url "tenants/tenant-id/contacts/contact-id"
          :multiple-contact-url "tenants/tenant-id/contacts"
          :merge-contacts-url "tenants/tenant-id/contacts/merge"
          :multiple-attribute-url "tenants/tenant-id/contacts/attributes"
          :multiple-layout-url "tenants/tenant-id/contacts/layouts"
          :single-layout-url "tenants/tenant-id/contacts/layouts/layout-id"}})

(defrecord ContactsModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [module-name (get @(:state this) :module-name)]
      (ih/register {:api {module-name {:get (partial get-contact this)
                                    :get-all (partial get-contacts this)
                                    :search (partial search-contacts this)
                                    :create (partial create-contact this)
                                    :update (partial update-contact this)
                                    :delete  (partial delete-contact this)
                                    :merge (partial merge-contacts this)
                                    :list-attributes (partial list-attributes this)
                                    :get-layout (partial get-layout this)
                                    :list-layouts (partial list-layouts this)}}
                 :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
