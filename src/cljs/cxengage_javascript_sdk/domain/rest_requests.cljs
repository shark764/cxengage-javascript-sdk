(ns cxengage-javascript-sdk.domain.rest-requests
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.api-utils :as api]
            [ajax.core :as ajax]
            [cljs.core.async :as a]))

(defn file-api-request [request-map]
  (let [response-channel (a/promise-chan)
        {:keys [method url body callback]} request-map
        request (merge {:uri url
                        :method method
                        :timeout 120000
                        :handler #(let [normalized-response (api/normalize-response-stucture % false true)]
                                    (if callback
                                      (callback normalized-response)
                                      (a/put! response-channel normalized-response)))
                        :format (ajax/json-request-format)
                        :response-format (ajax/json-response-format {:keywords? true})
                        :body body}
                       (when-let [token (state/get-token)]
                         {:headers {"Authorization" (str "Token " token)}}))]
    (ajax/ajax-request request)
    (when-not callback response-channel)))

(defn set-direction-request [direction]
  (let [tenant-id (state/get-active-tenant-id)
        resource-id (state/get-active-user-id)
        session-id (state/get-session-id)
        direction-request {:method :post
                           :url (iu/api-url
                                 "tenants/:tenant-id/presence/:resource-id/direction"
                                 {:tenant-id tenant-id
                                  :resource-id resource-id})
                           :body {:session-id session-id
                                  :direction direction
                                  :initiator-id resource-id}}]
    (api/api-request direction-request)))

(defn get-messaging-interaction-history-request [interaction-id]
  (let [tenant-id (state/get-active-tenant-id)
        history-request {:method :get
                         :url (iu/api-url
                               "messaging/tenants/:tenant-id/channels/:interaction-id/history"
                               {:tenant-id tenant-id
                                :interaction-id interaction-id})}]
    (api/api-request history-request)))

(defn get-messaging-interaction-metadata-request [interaction-id]
  (let [tenant-id (state/get-active-tenant-id)
        metadata-request {:method :get
                          :url (iu/api-url
                                "messaging/tenants/:tenant-id/channels/:interaction-id"
                                {:tenant-id tenant-id
                                 :interaction-id interaction-id})}]
    (api/api-request metadata-request)))

(defn get-config-request []
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        config-request {:method :get
                        :url (iu/api-url
                              "tenants/:tenant-id/users/:resource-id/config"
                              {:tenant-id tenant-id
                               :resource-id resource-id})}]
    (api/api-request config-request)))

(defn create-artifact-request [interaction-id artifact-body]
  (let [tenant-id (state/get-active-tenant-id)
        create-request {:method :post
                        :url (iu/api-url
                              "tenants/:tenant-id/interactions/:interaction-id/artifacts"
                              {:tenant-id tenant-id
                               :interaction-id interaction-id})
                        :body artifact-body}]
    (api/api-request create-request)))

(defn get-user-request []
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        get-user-request {:method :get
                          :url (iu/api-url
                                "tenants/:tenant-id/users/:resource-id"
                                {:tenant-id tenant-id
                                 :resource-id resource-id})}]
    (api/api-request get-user-request)))

(defn update-user-request [update-user-body]
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        update-user-request {:method :put
                             :url (iu/api-url
                                   "tenants/:tenant-id/users/:resource-id"
                                   {:tenant-id tenant-id
                                    :resource-id resource-id})
                             :body update-user-body}]
    (api/api-request update-user-request)))

(defn token-request [token-body]
  (let [token-request {:method :post
                       :authless-request? true
                       :url (iu/api-url "tokens")
                       :body token-body}]
    (api/api-request token-request)))

(defn login-request []
  (let [login-request {:method :post
                       :url (iu/api-url "login")}]
    (api/api-request login-request)))

(defn update-artifact-request [artifact-body artifact-id interaction-id]
  (let [tenant-id (state/get-active-tenant-id)
        artifact-req {:method :put
                      :url (iu/api-url
                            "tenants/:tenant-id/interactions/:interaction-id/artifacts/:artifact-id"
                            {:artifact-id artifact-id
                             :interaction-id interaction-id
                             :tenant-id tenant-id})
                      :body artifact-body}]
    (api/api-request artifact-req)))

(defn get-contact-interaction-history-request [contact-id page]
  (let [tenant-id (state/get-active-tenant-id)
        base-url "tenants/:tenant-id/contacts/:contact-id/interactions"
        url (if page
              (str base-url "?page=" page)
              base-url)
        get-history-request {:method :get
                             :url (iu/api-url
                                   url
                                   {:tenant-id tenant-id
                                    :contact-id contact-id})}]
    (api/api-request get-history-request)))

(defn get-available-stats-request []
  (let [tenant-id (state/get-active-tenant-id)
        locale (state/get-locale)
        url-string (str "tenants/:tenant-id/realtime-statistics/available?client=toolbar&locale=" locale)
        get-available-stats-req {:method :get
                                 :url (iu/api-url
                                       url-string
                                       {:tenant-id tenant-id})}]
    (api/api-request get-available-stats-req)))

(defn get-raw-url-request [url]
  (let [manifest-request {:method :get
                          :url url
                          :third-party-request? true}]
    (api/api-request manifest-request)))

(defn get-capacity-request
  ([]
   (get-capacity-request nil))
  ([resource-id]
   (let [tenant-id (state/get-active-tenant-id)
         ;; If resource-id is passed to the function, it will return the Capacity
         ;; for the specified resource-id. If no arguments are passed to the function
         ;; it will instead return the capacity for the active user's selected Tenant
         url-string (if resource-id
                      "tenants/:tenant-id/users/:resource-id/realtime-statistics/resource-capacity"
                      "tenants/:tenant-id/realtime-statistics/resource-capacity")
         url-params (if resource-id
                      {:tenant-id tenant-id :resource-id resource-id}
                      {:tenant-id tenant-id})
         capacity-request {:method :get
                           :url (iu/api-url url-string url-params)}]
     (api/api-request capacity-request))))

(defn batch-request [batch-body]
  (let [tenant-id (state/get-active-tenant-id)
        batch-request {:method :post
                       :preserve-casing? true
                       :body {:requests batch-body}
                       :url (iu/api-url
                             "tenants/:tenant-id/realtime-statistics/batch"
                             {:tenant-id tenant-id})}]
    (api/api-request batch-request)))

(defn get-groups-request []
  (let [tenant-id (state/get-active-tenant-id)
        groups-request {:method :get
                        :url (iu/api-url
                              "tenants/:tenant-id/groups"
                              {:tenant-id tenant-id})}]
    (api/api-request groups-request)))

(defn get-skills-request []
  (let [tenant-id (state/get-active-tenant-id)
        skills-request {:method :get
                        :url (iu/api-url
                              "tenants/:tenant-id/skills"
                              {:tenant-id tenant-id})}]
    (api/api-request skills-request)))

(defn get-artifact-by-id-request [artifact-id interaction-id tenant-id]
  (let [tenant-id (or tenant-id (state/get-active-tenant-id))
        artifact-request {:method :get
                          :url (iu/api-url
                                "tenants/:tenant-id/interactions/:interaction-id/artifacts/:artifact-id"
                                {:artifact-id artifact-id
                                 :interaction-id interaction-id
                                 :tenant-id tenant-id})}]
    (api/api-request artifact-request)))

(defn get-interaction-history-request [interaction-id]
  (let [tenant-id (state/get-active-tenant-id)
        history-request {:method :get
                         :url (iu/api-url
                               "tenants/:tenant-id/interactions/:interaction-id"
                               {:tenant-id tenant-id
                                :interaction-id interaction-id})}]
    (api/api-request history-request)))

(defn get-interaction-artifacts-request [interaction-id tenant-id]
  (let [tenant-id (or tenant-id (state/get-active-tenant-id))
        url (iu/api-url
             "tenants/:tenant-id/interactions/:interaction-id/artifacts"
             {:interaction-id interaction-id
              :tenant-id tenant-id})
        artifacts-request {:method :get
                           :url url}]
    (api/api-request artifacts-request)))

(defn save-logs-request [body]
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        save-logs-request {:url (iu/api-url
                                 "tenants/:tenant-id/users/:resource-id/logs"
                                 {:tenant-id (state/get-active-tenant-id)
                                  :resource-id (state/get-active-user-id)})
                           :method :post
                           :preserve-casing? true
                           :body body}]
    (api/api-request save-logs-request)))

(defn start-session-request []
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        start-session-req {:method :post
                           :url (iu/api-url
                                 "tenants/:tenant-id/presence/:resource-id/session"
                                 {:tenant-id tenant-id
                                  :resource-id resource-id})}]
    (api/api-request start-session-req)))

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
    (api/api-request heartbeat-req)))

(defn change-state-request [change-state-body]
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        change-state-request {:method :post
                              :url (iu/api-url
                                    "tenants/:tenant-id/presence/:resource-id"
                                    {:tenant-id tenant-id
                                     :resource-id resource-id})
                              :body change-state-body}]
    (api/api-request change-state-request)))

(defn get-contact-request [contact-id]
  (let [tenant-id (state/get-active-tenant-id)
        get-contact-request {:method :get
                             :preserve-casing? true
                             :url (iu/api-url
                                   "tenants/:tenant-id/contacts/:contact-id"
                                   {:tenant-id tenant-id
                                    :contact-id contact-id})}]
    (api/api-request get-contact-request)))

(defn get-contacts-request []
  (let [tenant-id (state/get-active-tenant-id)
        get-contact-request {:method :get
                             :preserve-casing? true
                             :url (iu/api-url
                                   "tenants/:tenant-id/contacts"
                                   {:tenant-id tenant-id})}]
    (api/api-request get-contact-request)))

(defn search-contacts-request [query]
  (let [tenant-id (state/get-active-tenant-id)
        search-contact-request {:method :get
                                :preserve-casing? true
                                :url (iu/api-url
                                      (str "tenants/:tenant-id/contacts" query)
                                      {:tenant-id tenant-id})}]
    (api/api-request search-contact-request)))

(defn create-contact-request [create-contact-body]
  (let [tenant-id (state/get-active-tenant-id)
        create-contact-request {:method :post
                                :preserve-casing? true
                                :url (iu/api-url
                                      "tenants/:tenant-id/contacts"
                                      {:tenant-id tenant-id})
                                :body create-contact-body}]
    (api/api-request create-contact-request)))

(defn update-contact-request [contact-id update-contact-body]
  (let [tenant-id (state/get-active-tenant-id)
        update-contact-request {:method :put
                                :preserve-casing? true
                                :url (iu/api-url
                                      "tenants/:tenant-id/contacts/:contact-id"
                                      {:tenant-id tenant-id
                                       :contact-id contact-id})
                                :body update-contact-body}]
    (api/api-request update-contact-request)))

(defn delete-contact-request [contact-id]
  (let [tenant-id (state/get-active-tenant-id)
        delete-contact-request {:method :delete
                                :preserve-casing? true
                                :url (iu/api-url
                                      "tenants/:tenant-id/contacts/:contact-id"
                                      {:tenant-id tenant-id
                                       :contact-id contact-id})}]
    (api/api-request delete-contact-request)))

(defn merge-contact-request [merge-contacts-body]
  (let [tenant-id (state/get-active-tenant-id)
        merge-contact-request {:method :post
                               :preserve-casing? true
                               :url (iu/api-url
                                     "tenants/:tenant-id/contacts/merge"
                                     {:tenant-id tenant-id})
                               :body merge-contacts-body}]
    (api/api-request merge-contact-request)))

(defn list-attributes-request []
  (let [tenant-id (state/get-active-tenant-id)
        list-attributes-request {:method :get
                                 :preserve-casing? true
                                 :url (iu/api-url
                                       "tenants/:tenant-id/contacts/attributes"
                                       {:tenant-id tenant-id})}]
    (api/api-request list-attributes-request)))

(defn get-layout-request [layout-id]
  (let [tenant-id (state/get-active-tenant-id)
        get-layout-request {:method :get
                            :preserve-casing? true
                            :url (iu/api-url
                                  "tenants/:tenant-id/contacts/layouts/:layout-id"
                                  {:tenant-id tenant-id
                                   :layout-id layout-id})}]
    (api/api-request get-layout-request)))

(defn list-layouts-request []
  (let [tenant-id (state/get-active-tenant-id)
        list-layouts-request {:method :get
                              :preserve-casing? true
                              :url (iu/api-url
                                    "tenants/:tenant-id/contacts/layouts"
                                    {:tenant-id tenant-id})}]
    (api/api-request list-layouts-request)))

(defn create-interaction-request [interaction-body]
  (let [tenant-id (state/get-active-tenant-id)
        create-interaction-request {:method :post
                                    :url (iu/api-url
                                          "tenants/:tenant-id/interactions"
                                          {:tenant-id tenant-id})
                                    :body interaction-body}]
    (api/api-request create-interaction-request)))

(defn get-note-request [interaction-id note-id]
  (let [tenant-id (state/get-active-tenant-id)
        get-note-request {:method :get
                          :url (iu/api-url
                                "tenants/:tenant-id/interactions/:interaction-id/notes/:note-id"
                                {:tenant-id tenant-id
                                 :interaction-id interaction-id
                                 :note-id note-id})}]
    (api/api-request get-note-request)))

(defn get-notes-request [interaction-id]
  (let [tenant-id (state/get-active-tenant-id)
        get-notes-request {:method :get
                           :url (iu/api-url
                                 "tenants/:tenant-id/interactions/:interaction-id/notes?contents=true"
                                 {:tenant-id tenant-id
                                  :interaction-id interaction-id})}]
    (api/api-request get-notes-request)))

(defn update-note-request [interaction-id note-id body]
  (let [tenant-id (state/get-active-tenant-id)
        update-note-request {:method :put
                             :body body
                             :url (iu/api-url
                                   "tenants/:tenant-id/interactions/:interaction-id/notes/:note-id"
                                   {:tenant-id tenant-id
                                    :interaction-id interaction-id
                                    :note-id note-id})}]
    (api/api-request update-note-request)))

(defn create-note-request [interaction-id body]
  (let [tenant-id (state/get-active-tenant-id)
        create-note-request {:method :post
                             :body body
                             :url (iu/api-url
                                   "tenants/:tenant-id/interactions/:interaction-id/notes?contents=true"
                                   {:tenant-id tenant-id
                                    :interaction-id interaction-id})}]
    (api/api-request create-note-request)))

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
     (api/api-request get-request))))

(defn send-flow-action-request [interaction-id action-id body]
  (let [tenant-id (state/get-active-tenant-id)
        action-request {:method :post
                        :url (iu/api-url
                              "tenants/:tenant-id/interactions/:interaction-id/actions/:action-id"
                              {:tenant-id tenant-id
                               :interaction-id interaction-id
                               :action-id action-id})
                        :body body}]
    (api/api-request action-request)))

(defn crud-entities-request [method entity-type]
  (let [tenant-id (state/get-active-tenant-id)
        get-url (iu/api-url
                 (str "tenants/:tenant-id/" entity-type "s")
                 {:tenant-id tenant-id})
        get-request {:method method
                     :url get-url}]
    (api/api-request get-request)))

(defn get-users-request [method entity-type exclude-offline]
  (let [tenant-id (state/get-active-tenant-id)
        url (str "tenants/:tenant-id/" entity-type "s")
        url (if exclude-offline
              (str url "?offline=false")
              url)
        get-url (iu/api-url
                 url
                 {:tenant-id tenant-id})
        get-request {:method method
                     :url get-url}]
    (api/api-request get-request)))

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
    (api/api-request interrupt-request)))

(defn get-branding-request []
  (let [tenant-id (state/get-active-tenant-id)
        get-branding-request {:method :get
                              :url (iu/api-url
                                    "tenants/:tenant-id/branding"
                                    {:tenant-id tenant-id})}]
    (api/api-request get-branding-request)))

(defn get-protected-branding-request []
  (let [tenant-id (state/get-active-tenant-id)
        get-protected-branding-request {:method :get
                                        :url (iu/api-url
                                              "tenants/:tenant-id/protected-brandings"
                                              {:tenant-id tenant-id})}]
    (api/api-request get-protected-branding-request)))

(defn get-tenant-request [tenant-id]
  (let [get-tenant-request {:method :get
                            :url (iu/api-url
                                  "tenants/:tenant-id"
                                  {:tenant-id tenant-id})}]
    (api/api-request get-tenant-request)))

(defn get-region-request [region-id]
  (let [get-region-request {:method :get
                            :url (iu/api-url
                                  "regions/:region-id"
                                  {:region-id region-id})}]
    (api/api-request get-region-request)))

(defn get-sso-details-request [details]
  (let [{:keys [tenant-id idp-id]} details
        url (cond
              (and (map? details) idp-id) (str tenant-id "/" idp-id)
              (and (map? details) (not idp-id)) tenant-id
              (string? details) details)
        get-sso-details-request {:method :get
                                 :url (iu/api-url (str "auth-info/" url))}]
    (api/api-request get-sso-details-request)))

(defn get-crm-interactions-request [id crm sub-type page]
  (let [tenant-id (state/get-active-tenant-id)
        base-url (str "tenants/:tenant-id/interactions?hookId=" id "&hookType=" crm "&hookSubType=" sub-type)
        url (if page
              (str base-url "&page=" page)
              base-url)
        get-crm-interactions-request {:method :get
                                      :url (iu/api-url
                                            url
                                            {:tenant-id tenant-id})}]
    (api/api-request get-crm-interactions-request)))

(defn get-tenant-details-request []
  (let [get-tenant-details-request {:method :get
                                    :url (iu/api-url "me")}]
    (api/api-request get-tenant-details-request)))

(defn get-dashboards-request [method entity-type exclude-inactive]
  (let [tenant-id (state/get-active-tenant-id)
        url (str "tenants/:tenant-id/" entity-type "s")
        url (if exclude-inactive
              (str url "?active=true")
              url)
        get-url (iu/api-url
                 url
                 {:tenant-id tenant-id})
        get-request {:method method
                     :url get-url}]
    (api/api-request get-request)))

;;--------------------------------------------------------------------------- ;;
;; Lists
;;--------------------------------------------------------------------------- ;;

(defn get-list-item-request [list-id list-item-key]
  (let [tenant-id (state/get-active-tenant-id)
        get-list-item-request {:method :get
                               :url (iu/api-url
                                     "tenants/:tenant-id/lists/:list-id/:list-item-key"
                                     {:tenant-id tenant-id
                                      :list-id list-id
                                      :list-item-key list-item-key})}]

    (api/api-request get-list-item-request)))

(defn get-lists-request [list-type-id name]
  (let [tenant-id (state/get-active-tenant-id)
        url (str "tenants/:tenant-id/lists")
        url (cond list-type-id (str url "?list-type-id=" list-type-id)
                  name         (str url "?name=" name)
                  :else url)
        get-lists-request {:method :get
                           :url    (iu/api-url
                                    url
                                    {:tenant-id tenant-id})}]
    (api/api-request get-lists-request)))

(defn get-list-types-request []
  (let [tenant-id (state/get-active-tenant-id)
        url (str "tenants/:tenant-id/list-types")
        get-list-types-request {:method :get
                                :url    (iu/api-url
                                         url
                                         {:tenant-id tenant-id})}]
    (api/api-request get-list-types-request)))

(defn create-list-request [list-type-id name items active]
  (let [tenant-id (state/get-active-tenant-id)
        create-list-request {:method :post
                             :url (iu/api-url
                                   "tenants/:tenant-id/lists"
                                   {:tenant-id tenant-id})
                             :body {:list-type-id list-type-id
                                    :name name
                                    :items []
                                    :active active}}]
    (api/api-request create-list-request)))

(defn create-list-item-request [list-id item-value]
  (let [tenant-id (state/get-active-tenant-id)
        create-list-item-request {:method :post
                                  :url (iu/api-url
                                        "tenants/:tenant-id/lists/:list-id"
                                        {:tenant-id tenant-id
                                         :list-id list-id})
                                  :body {:list-id list-id
                                         :item-value item-value}}]
    (api/api-request create-list-item-request)))

(defn update-list-request [list-id name active]
  (let [tenant-id (state/get-active-tenant-id)
        update-list-request (cond-> {:method :put
                                     :url (iu/api-url "tenants/:tenant-id/lists/:list-id"
                                                      {:tenant-id tenant-id
                                                       :list-id list-id})}
                              list-id             (assoc-in [:body :list-id] list-id)
                              name                (assoc-in [:body :name]    name)
                              (not (nil? active)) (assoc-in [:body :active] active))]
    (api/api-request update-list-request)))

(defn update-list-item-request [list-item-id list-item-key item-value]
  (let [tenant-id (state/get-active-tenant-id)
        update-list-item-request {:method :put
                                  :url (iu/api-url
                                        "tenants/:tenant-id/lists/:list-item-id/:list-item-key"
                                        {:tenant-id tenant-id
                                         :list-item-id list-item-id
                                         :list-item-key list-item-key})
                                  :body {:item-value item-value}}]
    (api/api-request update-list-item-request)))

(defn delete-list-item-request [list-id list-item-key]
  (let [tenant-id (state/get-active-tenant-id)
        delete-list-item-request {:method :delete
                                  :preserve-casing? true
                                  :url (iu/api-url
                                        "tenants/:tenant-id/lists/:list-id/:list-item-key"
                                        {:tenant-id tenant-id
                                         :list-id list-id
                                         :list-item-key list-item-key})}]
    (api/api-request delete-list-item-request)))

(defn download-list-request [list-id]
  (let [tenant-id (state/get-active-tenant-id)
        url (iu/api-url
             (str "tenants/:tenant-id/lists/:list-id/download/list-items.csv")
             {:tenant-id tenant-id
              :list-id list-id})
        download-list-request {:method :get
                               :csv-download? true
                               :url url}]
    (api/api-request download-list-request)))

;;--------------------------------------------------------------------------- ;;
;; Email templates
;;--------------------------------------------------------------------------- ;;

(defn get-email-types-request [email-type-id]
  (let [url (if email-type-id
              "email-types/:email-type-id"
              "email-types")
        get-email-types-request {:method :get
                                 :url    (iu/api-url
                                           url
                                           {:email-type-id email-type-id})}]
    (api/api-request get-email-types-request)))

(defn get-email-templates-request [email-type-id fallback]
  (let [tenant-id (state/get-active-tenant-id)
        url "tenants/:tenant-id/email-templates"
        url (if email-type-id
              (if fallback
                (str url "/:email-type-id/fallback")
                (str url "/:email-type-id"))
              url)
        get-email-templates-request {:method :get
                                     :url    (iu/api-url
                                               url
                                               {:tenant-id tenant-id
                                                :email-type-id email-type-id})}]
    (api/api-request get-email-templates-request)))

(defn create-email-template-request [email-type-id active shared body subject]
  (let [tenant-id (state/get-active-tenant-id)
        create-email-template-request {:method :post
                                       :url (iu/api-url
                                              "tenants/:tenant-id/email-templates/:email-type-id"
                                              {:tenant-id tenant-id
                                               :email-type-id email-type-id})
                                       :body {:active active
                                              :shared shared
                                              :body body
                                              :subject subject}}]
    (api/api-request create-email-template-request)))

(defn update-email-template-request [email-type-id active shared body subject]
  (let [tenant-id (state/get-active-tenant-id)
        update-email-template-request (cond-> {:method :put
                                               :url (iu/api-url "tenants/:tenant-id/email-templates/:email-type-id"
                                                                {:tenant-id tenant-id
                                                                 :email-type-id email-type-id})}
                                        (not (nil? active)) (assoc-in [:body :active]  active)
                                        (not (nil? shared)) (assoc-in [:body :shared]  shared)
                                        body                (assoc-in [:body :body]    body)
                                        subject             (assoc-in [:body :subject] subject))]
    (api/api-request update-email-template-request)))

(defn delete-email-template-request [email-type-id]
  (let [tenant-id (state/get-active-tenant-id)
        delete-email-template-request {:method :delete
                                       :url (iu/api-url
                                              "tenants/:tenant-id/email-templates/:email-type-id"
                                              {:tenant-id tenant-id
                                               :email-type-id email-type-id})}]
    (api/api-request delete-email-template-request)))
