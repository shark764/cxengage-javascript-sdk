(ns cxengage-javascript-sdk.modules.contacts
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-cljs-utils.core :as cxu]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [lumbajack.core :as jack]
            [cxengage-javascript-sdk.state :as state]
            [cljs-uuid-utils.core :as uuid]))

(defn contact-request
  ([url body method params topic-key spec module]
   (contact-request url body method params topic-key spec module nil))
  ([url body method params topic-key spec module query]
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
       (do (go (let [response (a/<! (contact-request request-url body method))
                     {:keys [status api-response]} response
                     {:keys [result]} api-response]
                 (if (not= status 200)
                   (p/publish {:topics topic
                               :error (e/api-error api-response)
                               :callback callback})
                   (p/publish {:topics topic
                               :response result
                               :callback callback}))))
           nil))))
  ([url body method]
   (let [request-map {:url url
                      :method method}]
     (cond-> request-map
       body (assoc :body body)
       true (iu/api-request)))))

(defn get-query-str
  [query]
  (let [queryv (->> query
                    (reduce-kv (fn [acc k v] (conj acc (name k) "=" v "&")) [])
                    (pop)
                    (into ["?"]))]
    (clojure.string/join queryv)))


(s/def ::get-contact-params
  (s/keys :req-un [:specs/contact-id]
          :opt-un [:specs/callback]))

(defn get-contact
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (get-contact module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [{:keys [contact-id callback] :as params} (iu/extract-params params)
         url {:base :single-contact-url
              :params {:tenant-id (state/get-active-tenant-id)
                       :contact-id contact-id}}
         method :get]
     (contact-request url nil method params :get-contact ::get-contact-params module))))

(s/def ::get-contacts-params
  (s/keys :req-un []
          :opt-un [:specs/callback]))

(defn get-contacts
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (get-contacts module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [{:keys [callback] :as params} (iu/extract-params params)
         url {:base :multiple-contact-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :get]
     (contact-request url nil method params :get-contacts ::get-contacts-params module))))

(s/def ::search-contacts-params
  (s/keys :req-un [:specs/query]
          :opt-un [:specs/callback]))

(defn search-contacts
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (search-contacts module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [{:keys [query callback] :as params} (iu/extract-params params)
         url {:base :multiple-contact-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :get
         query (get-query-str query)]
     (contact-request url nil method params :search-contacts ::search-contacts-params module query))))

(s/def ::create-contact-params
  (s/keys :req-un [:specs/attributes]
          :opt-un [:specs/callback]))

(defn create-contact
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (create-contact module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [{:keys [attributes callback] :as params} (iu/extract-params params)
         url {:base :multiple-contact-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :post
         body {:attributes attributes}]
     (contact-request url body method params :create-contact ::create-contact-params module))))


(s/def ::update-contact-params
  (s/keys :req-un [:specs/contact-id
                   :specs/attributes]
          :opt-un [:specs/callback]))

(defn update-contact
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (update-contact module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [{:keys [attributes contact-id callback] :as params} (iu/extract-params params)
         url {:base :single-contact-url
              :params {:tenant-id (state/get-active-tenant-id)
                       :contact-id contact-id}}
         method :put
         body {:attributes attributes}]
     (contact-request url body method params :update-contact ::update-contact-params module))))

(s/def ::delete-contact-params
  (s/keys :req-un [:specs/contact-id]
          :opt-un [:specs/callback]))

(defn delete-contact
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (delete-contact module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [{:keys [contact-id callback] :as params} (iu/extract-params params)
         url {:base :single-contact-url
              :params {:tenant-id (state/get-active-tenant-id)
                       :contact-id contact-id}}
         method :delete]
     (contact-request url nil method params :delete-contact ::delete-contact-params module))))

(s/def ::list-attributes-params
  (s/keys :req-un []
          :opt-un [:specs/callback]))

(defn list-attributes
  ([module]
   (list-attributes module {}))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (list-attributes module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (if (fn? params) {:callback params} params)
         {:keys [callback] :as params} (iu/extract-params params)
         url {:base :multiple-attribute-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :get]
     (contact-request url nil method params :list-attributes ::list-attributes-params module))))

(s/def ::get-layout-params
  (s/keys :req-un [:specs/layout-id]
          :opt-un [:specs/callback]))

(defn get-layout
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (get-layout module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [{:keys [layout-id callback] :as params} (iu/extract-params params)
         url {:base :single-layout-url
              :params {:tenant-id (state/get-active-tenant-id)
                       :layout-id layout-id}}
         method :get]
     (contact-request url nil method params :get-layout ::get-layout-params module))))

(s/def ::list-layouts-params
  (s/keys :req-un []
          :opt-un [:specs/callback]))

(defn list-layouts
  ([module]
   (list-layouts module {}))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (list-layouts module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (if (fn? params) {:callback params} params)
         {:keys [callback] :as params} (iu/extract-params params)
         url {:base :single-layout-url
              :params {:tenant-id (state/get-active-tenant-id)}}
         method :get]
     (contact-request url nil method params :list-layouts ::list-layouts-params module))))

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
          :multiple-attribute-url "tenants/tenant-id/contacts/attributes"
          :single-layout-url "tenants/tenant-id/contacts/layouts"
          :multiple-layout-url "tenants/tenant-id/contacts/layouts/layout-id"}})

(defrecord ContactsModule [config state]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)]
      (register {:api {module-name {:get (partial get-contact this)
                                    :get-all (partial get-contacts this)
                                    :search (partial search-contacts this)
                                    :create (partial create-contact this)
                                    :update (partial update-contact this)
                                    :delete (partial delete-contact this)
                                    :list-attributes (partial list-attributes this)
                                    :get-layout (partial get-layout this)
                                    :list-layouts (partial list-layouts this)}}
                 :module-name module-name})
      (log :info (str "<----- Started " (name module-name) " SDK module! ----->"))))
  (stop [this]))
