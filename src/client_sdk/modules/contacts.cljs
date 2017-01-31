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

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :CONTACTS/GET_CONTACT get-contact
                      :CONTACTS/CREATE_CONTACT create-contact
                      :CONTACTS/LIST_CONTACT list-contacts
                      :CONTACTS/UPDATE_CONTACT update-contact
                      :CONTACTS/DELETE_CONTACT delete-contact
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
