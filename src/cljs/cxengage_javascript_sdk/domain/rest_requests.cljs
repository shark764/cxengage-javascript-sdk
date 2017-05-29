(ns cxengage-javascript-sdk.domain.rest-requests
  (:require [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cljs.core.async :as a]))

(defn get-config-request []
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        config-request {:method :get
                        :url (iu/api-url
                              "tenants/:tenant-id/users/:resource-id/config"
                              {:tenant-id tenant-id
                               :resource-id resource-id})}]
    (iu/api-request config-request)))

(defn get-user-request []
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        get-user-request {:method :get
                          :url (iu/api-url
                                "tenants/:tenant-id/users/:resource-id"
                                {:tenant-id tenant-id
                                 :resource-id resource-id})}]
    (iu/api-request get-user-request)))

(defn update-user-request [update-user-body]
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        update-user-request {:method :put
                             :url (iu/api-url
                                   "tenants/:tenant-id/users/:resource-id"
                                   {:tenant-id tenant-id
                                    :resource-id resource-id})
                             :body update-user-body}]
    (iu/api-request update-user-request)))

(defn token-request [token-body]
  (let [token-request {:method :post
                       :url (iu/api-url "tokens")
                       :body token-body}]
    (iu/api-request token-request)))

(defn login-request []
  (let [login-request {:method :post
                       :url (iu/api-url "login")}]
    (iu/api-request login-request)))

(defn update-artifact-request [artifact-body artifact-id interaction-id]
  (let [tenant-id (state/get-active-tenant-id)
        artifact-req {:method :put
                      :url (iu/api-url
                            "tenants/:tenant-id/interactions/:interaction-id/artifacts/:artifact-id"
                            {:artifact-id artifact-id
                             :interaction-id interaction-id
                             :tenant-id tenant-id})
                      :body artifact-body}]
    (iu/api-request artifact-req)))

(defn get-artifact-by-id-request [artifact-id interaction-id]
  (let [tenant-id (state/get-active-tenant-id)
        artifact-request {:method :get
                          :url (iu/api-url
                                "tenants/:tenant-id/interactions/:interaction-id/artifacts/:artifact-id"
                                {:artifact-id artifact-id
                                 :interaction-id interaction-id
                                 :tenant-id tenant-id})}]
    (iu/api-request artifact-request)))

(defn save-logs-request [body]
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        save-logs-request {:url (iu/api-url
                                 "tenants/:tenant-id/users/:resource-id/logs"
                                 {:tenant-id (state/get-active-tenant-id)
                                  :resource-id (state/get-active-user-id)})
                           :method :post
                           :body body}]
    (iu/api-request save-logs-request true)))

(defn start-session-request []
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        start-session-req {:method :post
                           :url (iu/api-url
                                 "tenants/:tenant-id/presence/:resource-id/session"
                                 {:tenant-id tenant-id
                                  :resource-id resource-id})}]
    (iu/api-request start-session-req)))

(defn heartbeat-request []
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        session-id (state/get-session-id)
        heartbeat-req {:method :post
                       :body {:session-id session-id}
                       :url (iu/api-url
                             "tenants/:tenant-id/presence/:resource-id/heartbeat"
                             {:tenant-id tenant-id
                              :resource-id resource-id})}]
    (iu/api-request heartbeat-req)))

(defn change-state-request [change-state-body]
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        change-state-request {:method :post
                              :url (iu/api-url
                                    "tenants/:tenant-id/presence/:resource-id"
                                    {:tenant-id tenant-id
                                     :resource-id resource-id})
                              :body change-state-body}]
    (iu/api-request change-state-request)))

(defn get-contact-request [contact-id]
  (let [tenant-id (state/get-active-tenant-id)
        get-contact-request {:method :get
                             :url (iu/api-url
                                   "tenants/:tenant-id/contacts/:contact-id"
                                   {:tenant-id tenant-id
                                    :contact-id contact-id})}]
    (iu/api-request get-contact-request true)))

(defn get-contacts-request []
  (let [tenant-id (state/get-active-tenant-id)
        get-contact-request {:method :get
                             :url (iu/api-url
                                   "tenants/:tenant-id/contacts"
                                   {:tenant-id tenant-id})}]
    (iu/api-request get-contact-request true)))

(defn search-contacts-request [query]
  (let [tenant-id (state/get-active-tenant-id)
        search-contact-request {:method :get
                                :url (iu/api-url
                                      (str "tenants/:tenant-id/contacts" query)
                                      {:tenant-id tenant-id})}]
    (iu/api-request search-contact-request true)))

(defn create-contact-request [create-contact-body]
  (let [tenant-id (state/get-active-tenant-id)
        create-contact-request {:method :post
                                :url (iu/api-url
                                      "tenants/:tenant-id/contacts"
                                      {:tenant-id tenant-id})
                                :body create-contact-body}]
    (iu/api-request create-contact-request true)))

(defn update-contact-request [contact-id update-contact-body]
  (let [tenant-id (state/get-active-tenant-id)
        update-contact-request {:method :put
                                :url (iu/api-url
                                      "tenants/:tenant-id/contacts/:contact-id"
                                      {:tenant-id tenant-id
                                       :contact-id contact-id})
                                :body update-contact-body}]
    (iu/api-request update-contact-request true)))

(defn delete-contact-request [contact-id]
  (let [tenant-id (state/get-active-tenant-id)
        delete-contact-request {:method :delete
                                :url (iu/api-url
                                      "tenants/:tenant-id/contacts/:contact-id"
                                      {:tenant-id tenant-id
                                       :contact-id contact-id})}]
    (iu/api-request delete-contact-request true)))

(defn merge-contact-request [merge-contacts-body]
  (let [tenant-id (state/get-active-tenant-id)
        merge-contact-request {:method :post
                               :url (iu/api-url
                                     "tenants/:tenant-id/contacts/merge"
                                     {:tenant-id tenant-id})
                               :body merge-contacts-body}]
    (iu/api-request merge-contact-request true)))

(defn list-attributes-request []
  (let [tenant-id (state/get-active-tenant-id)
        list-attributes-request {:method :get
                                 :url (iu/api-url
                                       "tenants/:tenant-id/contacts/attributes"
                                       {:tenant-id tenant-id})}]
    (iu/api-request list-attributes-request true)))

(defn get-layout-request [layout-id]
  (let [tenant-id (state/get-active-tenant-id)
        get-layout-request {:method :get
                            :url (iu/api-url
                                  "tenants/:tenant-id/contacts/layouts/:layout-id"
                                  {:tenant-id tenant-id
                                   :layout-id layout-id})}]
    (iu/api-request get-layout-request true)))

(defn list-layouts-request []
  (let [tenant-id (state/get-active-tenant-id)
        list-layouts-request {:method :get
                              :url (iu/api-url
                                    "tenants/:tenant-id/contacts/layouts"
                                    {:tenant-id tenant-id})}]
    (iu/api-request list-layouts-request true)))

(defn create-interaction-request [interaction-body]
  (let [tenant-id (state/get-active-tenant-id)
        create-interaction-request {:method :post
                                    :url (iu/api-url
                                          "tenants/:tenant-id/interactions"
                                          {:tenant-id tenant-id})
                                    :body interaction-body}]
    (iu/api-request create-interaction-request)))

(defn get-note-request [interaction-id note-id]
  (let [tenant-id (state/get-active-tenant-id)
        get-note-request {:method :get
                          :url (iu/api-url
                                "tenants/:tenant-id/interactions/:interaction-id/notes/:note-id"
                                {:tenant-id tenant-id
                                 :interaction-id interaction-id
                                 :note-id note-id})}]
    (iu/api-request get-note-request)))

(defn get-notes-request [interaction-id]
  (let [tenant-id (state/get-active-tenant-id)
        get-notes-request {:method :get
                           :url (iu/api-url
                                 "tenants/:tenant-id/interactions/:interaction-id/notes?contents=true"
                                 {:tenant-id tenant-id
                                  :interaction-id interaction-id})}]
    (iu/api-request get-notes-request)))

(defn update-note-request [interaction-id note-id body]
  (let [tenant-id (state/get-active-tenant-id)
        update-note-request {:method :put
                             :body body
                             :url (iu/api-url
                                   "tenants/:tenant-id/interactions/:interaction-id/notes/:note-id"
                                   {:tenant-id tenant-id
                                    :interaction-id interaction-id
                                    :note-id note-id})}]
    (iu/api-request update-note-request)))

(defn create-note-request [interaction-id body]
  (let [tenant-id (state/get-active-tenant-id)
        create-note-request {:method :post
                             :body body
                             :url (iu/api-url
                                   "tenants/:tenant-id/interactions/:interaction-id/notes?contents=true"
                                   {:tenant-id tenant-id
                                    :interaction-id interaction-id})}]
    (iu/api-request create-note-request)))

(defn crud-entity-request
  ([method entity-type entity-id]
   (crud-entity-request method entity-type entity-id nil))
  ([method entity-type entity-id entity-body]
   (let [tenant-id (state/get-active-tenant-id)
         entity-key (keyword (str entity-type "-id"))
         get-url (iu/api-url
                  (str "tenants/:tenant-id/" entity-type "s/:" entity-type "-id")
                  (assoc {:tenant-id tenant-id} entity-key entity-id))
         get-request (cond-> {:method method
                              :url get-url}
                       entity-body (assoc :body entity-body))]
     (iu/api-request get-request))))

(defn crud-entities-request [method entity-type]
  (let [tenant-id (state/get-active-tenant-id)
        get-url (iu/api-url
                 (str "tenants/:tenant-id/" entity-type "s")
                 {:tenant-id tenant-id})
        get-request {:method method
                     :url get-url}]
    (iu/api-request get-request)))

(defn send-interrupt-request [interaction-id interrupt-type interrupt-body]
  (let [tenant-id (state/get-active-tenant-id)
        interrupt-request {:method :post
                           :url (iu/api-url
                                 "tenants/:tenant-id/interactions/:interaction-id/interrupts"
                                 {:tenant-id tenant-id
                                  :interaction-id interaction-id})
                           :body {:source "client"
                                  :interrupt-type interrupt-type
                                  :interrupt interrupt-body}}]
    (iu/api-request interrupt-request)))

(defn get-branding-request []
  (let [tenant-id (state/get-active-tenant-id)
        get-branding-request {:method :get
                              :url (iu/api-url
                                    "tenants/:tenant-id/branding"
                                    {:tenant-id tenant-id})}]
    (iu/api-request get-branding-request)))
