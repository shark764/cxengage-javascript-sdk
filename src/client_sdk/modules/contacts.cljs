(ns client-sdk.modules.contacts
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [client-sdk-utils.core :as u]
            [lumbajack.core :as jack]
            [cljs-uuid-utils.core :as uuid]))

(def module-state (atom {}))

(defn ^:private contact-request
  [url body method token resp resp-chan]
  (let [request-map {:url url
                     :method method
                     :resp-chan resp}]
    (cond-> request-map 
            body (assoc :body body)
            token (assoc :token token)
            true (u/api-request))
    (go
      (a/>! resp-chan (:result (a/<! resp))))))

(defn ^:private get-query-str
  [query]
  (let [queryv (->> query 
                    (reduce (fn [acc x] 
                                    (if (keyword? x)  
                                      (conj acc (name x) "=") 
                                      (conj acc x "&"))) [])
                    (pop)
                    (into ["?"]))]
    (apply str queryv)))

(defn ^:private attributes-request
  [url body method token resp resp-chan ])

(defn get-contact
  [message]
  (let [{:keys [token contact-id tenant-id resp-chan]} message
        resp (a/promise-chan)
        method :get
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts/" contact-id))]
    (contact-request url nil method token resp resp-chan)))

(defn list-contacts
  [message]
  (let [{:keys [token tenant-id query resp-chan]} message
        resp (a/promise-chan)
        method :get
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts"))]
    (when query
      (let [query-url (str url (get-query-str query))]
        (contact-request query-url nil method token resp resp-chan)))
    (contact-request url nil method token resp resp-chan)))

(defn create-contact
  [message]
  (let [{:keys [token tenant-id resp-chan attributes]} message
        resp (a/promise-chan)
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts"))
        body {:attributes attributes}
        method :post]
    (contact-request url body method token resp resp-chan)))

(defn update-contact
  [message]
  (let [{:keys [token contact-id tenant-id resp-chan attributes]} message
        resp (a/promise-chan)
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts/" contact-id))
        body {:attributes attributes}
        method :put]
    (contact-request url body method token resp resp-chan)))

(defn delete-contact
  [message]
  (let [{:keys [token contact-id tenant-id resp-chan]} message
        resp (a/promise-chan)
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts/" contact-id))
        method :delete]
    (contact-request url nil method token resp resp-chan)))

(defn get-attribute
  [message]
  (let [{:keys [token tenant-id attribute-id resp-chan]} message
        resp (a/promise-chan)
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts/attributes/" attribute-id))
        method :get]
    (contact-request url nil method token resp resp-chan)))

(defn list-attributes
  [message]
  (let [{:keys [token tenant-id resp-chan]} message
        resp (a/promise-chan)
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts/attributes"))
        method :get]
    (contact-request url nil method token resp resp-chan)))

(defn create-attribute
  [message]
  (let [{:keys [token tenant-id resp-chan label object-name type mandatory default]} message
        resp (a/promise-chan)
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts/attributes"))
        body (cond-> {:label label
                      :objectName object-name
                      :type type}
                     (not (nil? mandatory)) (assoc :mandatory mandatory)
                     default (assoc :default default))
        method :post] 
    (contact-request url body method token resp resp-chan)))

(defn update-attribute
  [message]
  (let [{:keys [token tenant-id attribute-id resp-chan label mandatory default]} message
        resp (a/promise-chan)
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts/attributes/" attribute-id))
        body (cond-> {}
                     label (assoc :label label)
                     default (assoc :default default)
                     (not (nil? mandatory)) (assoc :mandatory mandatory))
        method :put]
    (contact-request url body method token resp resp-chan)))

(defn delete-attribute
  [message]
  (let [{:keys [token tenant-id attribute-id resp-chan]} message
        resp (a/promise-chan)
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts/attributes/" attribute-id))
        method :delete]
    (contact-request url nil method token resp resp-chan)))

(defn get-layout
  [message]
  (let [{:keys [token tenant-id layout-id resp-chan]} message
        resp (a/promise-chan)
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts/layouts/" layout-id))
        method :get]
    (contact-request url nil method token resp resp-chan)))

(defn list-layouts
  [message]
  (let [{:keys [token tenant-id resp-chan]} message
        resp (a/promise-chan)
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts/layouts"))
        method :get]
    (contact-request url nil method token resp resp-chan)))

(defn create-layout
  [message]
  (let [{:keys [token tenant-id resp-chan labels attributes name description active]} message
        resp (a/promise-chan)
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts/layouts"))
        layout (->> labels
                    (reduce-kv (fn [acc k v]
                                 (conj acc {:label v})) [])
                    (map #(assoc %2 :attributes %1) attributes))
        body (cond-> {:layout layout
                      :name name}
                     description (assoc :description description)
                     (not (nil? active)) (assoc :active active))
        method :post]
    (contact-request url body method token resp resp-chan)))

(defn update-layout
  [message]
  (let [{:keys [token tenant-id layout-id resp-chan labels attributes name description active]} message
        resp (a/promise-chan)
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts/layouts/" layout-id))
        layout (->> labels
                    (reduce-kv (fn [acc k v]
                                 (conj acc {:label v})) [])
                    (map #(assoc %2 :attributes %1) attributes))
        body (cond-> {}
                     (and labels attributes) (assoc :layout layout)
                     description (assoc :description description)
                     name (assoc :name name)
                     (not (nil? active)) (assoc :active active))
        method :put]
    (contact-request url body method token resp resp-chan)))

(defn delete-layout
  [message]
  (let [{:keys [token tenant-id layout-id resp-chan]} message
        resp (a/promise-chan)
        url (u/api-url (:env @module-state) (str "/tenants/" tenant-id "/contacts/layouts/" layout-id))
        method :delete]
    (contact-request url nil method token resp resp-chan)))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :CONTACTS/GET_CONTACT get-contact
                      :CONTACTS/CREATE_CONTACT create-contact
                      :CONTACTS/LIST_CONTACT list-contacts
                      :CONTACTS/UPDATE_CONTACT update-contact
                      :CONTACTS/DELETE_CONTACT delete-contact
                      :CONTACTS/GET_ATTRIBUTE get-attribute
                      :CONTACTS/LIST_ATTRIBUTES list-attributes
                      :CONTACTS/CREATE_ATTRIBUTE create-attribute
                      :CONTACTS/UPDATE_ATTRIBUTE update-attribute
                      :CONTACTS/GET_LAYOUT get-layout
                      :CONTACTS/LIST_LAYOUTS list-layouts
                      :CONTACTS/CREATE_LAYOUT create-layout
                      :CONTACTS/UPDATE_LAYOUT update-layout
                      :CONTACTS/DELETE_LAYOUT delete-layout
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in Contacts SDK module" (:type message)))))

(defn module-shutdown-handler [message]
  (reset! module-state)
  (log :info "Received shutdown message form Core - Contacts Module shutting down...."))

(defn init [env]
  (log :info "Initializing SDK module: Contacts")
  (swap! module-state assoc :env env)
  (let [module-inputs< (a/chan 1024)
        module-shutdown< (a/chan 1024)]
    (u/start-simple-consumer! module-inputs< module-router)
    (u/start-simple-consumer! module-shutdown< module-shutdown-handler)
    {:messages module-inputs<
     :shutdown module-shutdown<}))
