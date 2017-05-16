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
  ([url body method params topic-key spec preserve-casing?]
   (contact-request url body method params topic-key spec preserve-casing? nil))
  ([url body method params topic-key spec preserve-casing? query]
   (let [{:keys [callback]} params
         urls {:single-contact-url "tenants/:tenant-id/contacts/:contact-id"
               :multiple-contact-url "tenants/:tenant-id/contacts"
               :merge-contacts-url "tenants/:tenant-id/contacts/merge"
               :multiple-attribute-url "tenants/:tenant-id/contacts/attributes"
               :multiple-layout-url "tenants/:tenant-id/contacts/layouts"
               :single-layout-url "tenants/:tenant-id/contacts/layouts/:layout-id"}
         request-url (iu/api-url
                      (get urls (:base url))
                      (:params url))
         topic (p/get-topic topic-key)]
     (if-not (s/valid? spec params)
       (p/publish {:topics topic
                   :error (e/args-failed-spec-err)
                   :callback callback})
       (do (go (let [response (a/<! (contact-request request-url body method preserve-casing?))
                     {:keys [status api-response]} response
                     {:keys [result]} api-response]
                 (when (= status 200)
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
  ([] (e/wrong-number-of-sdk-fn-args-err))
  ([params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (get-contact (merge (ih/extract-params params true) {:callback (first others)}))))
  ([params]
   (let [{:keys [contactId callback] :as params} (ih/extract-params params true)
         url {:base :single-contact-url
              :params {:tenant-id (state/get-active-tenant-id)
                       :contact-id contactId}}
         method :get]
     (contact-request url nil method params :get-contact ::get-contact-params true))))

(s/def ::get-contacts-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(defn get-contacts
  ([]
   (get-contacts {}))
  ([params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (get-contacts (merge (ih/extract-params params true) {:callback (first others)}))))
  ([params]
   (let [params (if (fn? params) {:callback params} params)
         {:keys [callback] :as params} (ih/extract-params params)
         url {:base :multiple-contact-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :get]
     (contact-request url nil method params :get-contacts ::get-contacts-params true))))

(s/def ::search-contacts-params
  (s/keys :req-un [::specs/query]
          :opt-un [::specs/callback]))

(defn search-contacts
  ([] (e/wrong-number-of-sdk-fn-args-err))
  ([params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (search-contacts (merge (ih/extract-params params true) {:callback (first others)}))))
  ([params]
   (let [{:keys [query callback] :as params} (ih/extract-params params true)
         url {:base :multiple-contact-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :get
         query (get-query-str query)]
     (contact-request url nil method params :search-contacts ::search-contacts-params true query))))

(s/def ::create-contact-params
  (s/keys :req-un [::specs/attributes]
          :opt-un [::specs/callback]))

(defn create-contact
  ([] (e/wrong-number-of-sdk-fn-args-err))
  ([params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (create-contact (merge (ih/extract-params params true) {:callback (first others)}))))
  ([params]
   (let [{:keys [attributes callback] :as params} (ih/extract-params params true)
         url {:base :multiple-contact-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :post
         body {:attributes attributes}]
     (contact-request url body method params :create-contact ::create-contact-params true))))

(s/def ::update-contact-params
  (s/keys :req-un [::specs/contactId
                   ::specs/attributes]
          :opt-un [::specs/callback]))

(defn update-contact
  ([] (e/wrong-number-of-sdk-fn-args-err))
  ([params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (update-contact (merge (ih/extract-params params true) {:callback (first others)}))))
  ([params]
   (let [{:keys [attributes contactId callback] :as params} (ih/extract-params params true)
         url {:base :single-contact-url
              :params {:tenant-id (state/get-active-tenant-id)
                       :contact-id contactId}}
         method :put
         body {:attributes attributes}]
     (contact-request url body method params :update-contact ::update-contact-params true))))

(s/def ::delete-contact-params
  (s/keys :req-un [::specs/contactId]
          :opt-un [::specs/callback]))

(defn delete-contact
  ([] (e/wrong-number-of-sdk-fn-args-err))
  ([params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (delete-contact (merge (ih/extract-params params true) {:callback (first others)}))))
  ([params]
   (let [{:keys [contactId callback] :as params} (ih/extract-params params true)
         url {:base :single-contact-url
              :params {:tenant-id (state/get-active-tenant-id)
                       :contact-id contactId}}
         method :delete]
     (contact-request url nil method params :delete-contact ::delete-contact-params true))))

(s/def ::merge-contacts-params
  (s/keys :req-un [::specs/contactIds
                   ::specs/attributes]
          :opt-un [::specs/callback]))

(defn merge-contacts
  ([] (e/wrong-number-of-sdk-fn-args-err))
  ([params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (merge-contacts (merge (ih/extract-params params true) {:callback (first others)}))))
  ([params]
   (let [{:keys [contactIds attributes callback] :as params} (ih/extract-params params true)
         url {:base :merge-contacts-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :post
         body {:attributes attributes
               :contactIds contactIds}]
     (contact-request url body method params :merge-contacts ::merge-contacts-params true))))

(s/def ::list-attributes-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(defn list-attributes
  ([]
   (list-attributes {}))
  ([params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (list-attributes (merge (ih/extract-params params true) {:callback (first others)}))))
  ([params]
   (let [params (if (fn? params) {:callback params} params)
         {:keys [callback] :as params} (ih/extract-params params true)
         method :get
         request-url (iu/api-url
                      "tenants/:tenant-id/contacts/attributes"
                      {:tenant-id (state/get-active-tenant-id)})
         topic (p/get-topic :list-attributes)]
     (if-not (s/valid? ::list-attributes-params params)
       (p/publish {:topics topic
                   :error (e/args-failed-spec-err)
                   :callback callback})
       (go (let [request-map {:url request-url
                              :method method}
                 response (a/<! (iu/api-request request-map true))
                 {:keys [status api-response]} response
                 {:keys [result]} api-response]
             (when (= status 200)
               (p/publish {:topics topic
                           :response result
                           :callback callback} true)))
           nil)))))

(s/def ::get-layout-params
  (s/keys :req-un [::specs/layoutId]
          :opt-un [::specs/callback]))

(defn get-layout
  ([] (e/wrong-number-of-sdk-fn-args-err))
  ([params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (get-layout (merge (ih/extract-params params true) {:callback (first others)}))))
  ([params]
   (let [{:keys [layoutId callback] :as params} (ih/extract-params params true)
         url {:base :single-layout-url
              :params {:tenant-id (state/get-active-tenant-id)
                       :layout-id layoutId}}
         method :get]
     (contact-request url nil method params :get-layout ::get-layout-params true))))

(s/def ::list-layouts-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(defn list-layouts
  ([]
   (list-layouts {}))
  ([params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-sdk-fn-args-err)
     (list-layouts (merge (ih/extract-params params true) {:callback (first others)}))))
  ([params]
   (let [params (if (fn? params) {:callback params} params)
         {:keys [callback] :as params} (ih/extract-params params true)
         url {:base :multiple-layout-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :get]
     (contact-request url nil method params :list-layouts ::list-layouts-params true))))

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
