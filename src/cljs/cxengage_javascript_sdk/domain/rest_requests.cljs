(ns cxengage-javascript-sdk.domain.rest-requests
  (:require-macros [cxengage-javascript-sdk.domain.macros :refer [log]])
  (:require [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.api-utils :as api]
            [clojure.string :as string]
            [ajax.core :as ajax]
            [cljs.core.async :as a]
            [cljs-uuid-utils.core :as id]
            [camel-snake-kebab.core :as camel]))

(defn file-api-request [request-map]
  (let [response-channel (a/promise-chan)
        {:keys [method url body callback]} request-map
        request (merge {:uri url
                        :method method
                        :timeout 120000
                        :handler #(let [normalized-response (api/normalize-response-stucture % false true false)]
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

(defn set-direction-request [direction agent-id session-id]
  (let [tenant-id (state/get-active-tenant-id)
        initiator-id (state/get-active-user-id)
        resource-id (or agent-id initiator-id)
        session-id (or session-id (state/get-session-id))
        direction-request {:method :post
                           :url (iu/api-url
                                 "tenants/:tenant-id/presence/:resource-id/direction"
                                 {:tenant-id tenant-id
                                  :resource-id resource-id})
                           :body {:session-id session-id
                                  :direction direction
                                  :initiator-id initiator-id}}]
    (api/api-request direction-request)))

(defn get-smooch-interaction-history-request [interaction-id]
  (let [tenant-id (state/get-active-tenant-id)
        history-request {:method :get
                         :url (iu/api-url
                               "smooch/tenants/:tenant-id/interactions/:interaction-id/conversation"
                               {:tenant-id tenant-id
                                :interaction-id interaction-id})}]
    (api/api-request history-request)))

(defn send-smooch-message [interaction-id agent-message-id message]
  (let [tenant-id (state/get-active-tenant-id)
        agent-id (state/get-active-user-id)
        agent-name (state/get-active-user-name)
        send-request {:method :post
                      :url (iu/api-url
                            "smooch/tenants/:tenant-id/interactions/:interaction-id/message"
                            {:tenant-id tenant-id
                             :interaction-id interaction-id})
                      :body {:type "agent"
                             :resource-id agent-id
                             :agent-message-id agent-message-id
                             :from agent-name
                             :message message}}]
    (api/api-request send-request)))

(defn send-smooch-attachment [interaction-id agent-message-id file]
  (let [tenant-id (state/get-active-tenant-id)
        form-data (doto
                      (js/FormData.)
                      (.append (.-name file) file (.-name file))
                      (.append "agentMessageId" agent-message-id))
        send-request {:method :post
                      :url (iu/api-url
                            "smooch/tenants/:tenant-id/interactions/:interaction-id/attachment"
                            {:tenant-id tenant-id
                             :interaction-id interaction-id})
                      :body form-data}]
    (file-api-request send-request)))

(defn send-smooch-conversation-read [interaction-id]
  (let [tenant-id (state/get-active-tenant-id)
        agent-id (state/get-active-user-id)
        agent-name (state/get-active-user-name)
        send-request {:method :put
                      :url (iu/api-url
                            "smooch/tenants/:tenant-id/interactions/:interaction-id/conversation"
                            {:tenant-id tenant-id
                             :interaction-id interaction-id})
                      :body {:resource-id agent-id
                             :from agent-name
                             :event "conversation-read"}}]
    (api/api-request send-request)))

(defn send-smooch-typing [interaction-id typing]
  (let [tenant-id (state/get-active-tenant-id)
        agent-id (state/get-active-user-id)
        agent-name (state/get-active-user-name)
        send-request {:method :put
                      :url (iu/api-url
                            "smooch/tenants/:tenant-id/interactions/:interaction-id/conversation"
                            {:tenant-id tenant-id
                             :interaction-id interaction-id})
                      :body {:resource-id agent-id
                             :from agent-name
                             :event (if typing "typing-start" "typing-stop")}}]
    (api/api-request send-request)))

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

(defn get-config-request
  ([]
   (get-config-request nil))
  ([silent-monitor]
   (let [resource-id (state/get-active-user-id)
         tenant-id (state/get-active-tenant-id)
         url-string (if silent-monitor
                      "tenants/:tenant-id/users/:resource-id/config?silent-monitor=true"
                      "tenants/:tenant-id/users/:resource-id/config")
         config-request {:method :get
                         :url (iu/api-url
                                url-string
                                {:tenant-id tenant-id
                                 :resource-id resource-id})}]
    (api/api-request config-request))))

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

(defn update-user-request [user-id update-user-body]
  (let [tenant-id (state/get-active-tenant-id)
        update-user-request {:method :put
                             :url (iu/api-url
                                   "tenants/:tenant-id/users/:user-id"
                                   {:tenant-id tenant-id
                                    :user-id user-id})
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
                                       {:tenant-id tenant-id})
                                 :retry-logic :retry-indefinitely}]
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
                             {:tenant-id tenant-id})
                       :retry-logic :skip-retries}]
    (api/api-request batch-request)))

(defn get-groups-request []
  (let [tenant-id (state/get-active-tenant-id)
        groups-request {:method :get
                        :url (iu/api-url
                              "tenants/:tenant-id/groups"
                              {:tenant-id tenant-id})}]
    (api/api-request groups-request)))

(defn get-platform-roles-request []
  (let [platform-roles-request {:method :get
                                :url (iu/api-url "roles")}]
    (api/api-request platform-roles-request)))

(defn get-platform-user-request [user-id]
 (api/api-request {:method :get
                   :url (iu/construct-api-url ["users" user-id])}))

(defn get-platform-user-email-request [email]
  (let [platform-user-email-request {:method :get
                                     :url (iu/api-url "users?email=:email"
                                            {:email email})}]
    (api/api-request platform-user-email-request)))

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

(defn start-session-request [silent-monitoring sf-user-id]
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        start-session-req (cond-> {:method :post
                                   :url (iu/api-url
                                          "tenants/:tenant-id/presence/:resource-id/session"
                                          {:tenant-id tenant-id
                                           :resource-id resource-id})}
                              (not (nil? silent-monitoring)) (assoc-in [:body :silent-monitoring] true)
                              (not (nil? sf-user-id)) (assoc-in [:body :metadata :sf-user-id] sf-user-id))]
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
                              :resource-id resource-id})
                       :retry-logic :retry-indefinitely}]
    (api/api-request heartbeat-req)))

(defn change-state-request [change-state-body agent-id]
  (let [resource-id (or agent-id (state/get-active-user-id))
        tenant-id (state/get-active-tenant-id)
        change-state-request {:method :post
                              :url (iu/api-url
                                    "tenants/:tenant-id/presence/:resource-id"
                                    {:tenant-id tenant-id
                                     :resource-id resource-id})
                              :body change-state-body}]
    (api/api-request change-state-request)))

(defn get-user-presence-info []
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        get-user-presence-info {:method :get
                                :url (iu/api-url
                                      "tenants/:tenant-id/presence/:resource-id"
                                      {:tenant-id tenant-id
                                       :resource-id resource-id})}]
    (api/api-request get-user-presence-info)))

(defn get-contact-request [contact-id]
  (let [tenant-id (state/get-active-tenant-id)
        get-contact-request {:method :get
                             :stringify-keys? true
                             :url (iu/api-url
                                   "tenants/:tenant-id/contacts/:contact-id"
                                   {:tenant-id tenant-id
                                    :contact-id contact-id})}]
    (api/api-request get-contact-request)))

(defn get-contacts-request []
  (let [tenant-id (state/get-active-tenant-id)
        get-contact-request {:method :get
                             :stringify-keys? true
                             :url (iu/api-url
                                   "tenants/:tenant-id/contacts"
                                   {:tenant-id tenant-id})}]
    (api/api-request get-contact-request)))

(defn search-contacts-request [query]
  (let [tenant-id (state/get-active-tenant-id)
        search-contact-request {:method :get
                                 :stringify-keys? true
                                :url (iu/api-url
                                      (str "tenants/:tenant-id/contacts" query)
                                      {:tenant-id tenant-id})}]
    (api/api-request search-contact-request)))

(defn create-contact-request [create-contact-body]
  (let [tenant-id (state/get-active-tenant-id)
        create-contact-request {:method :post
                                :stringify-keys? true
                                :url (iu/api-url
                                      "tenants/:tenant-id/contacts"
                                      {:tenant-id tenant-id})
                                :body create-contact-body}]
    (api/api-request create-contact-request)))

(defn update-contact-request [contact-id update-contact-body]
  (let [tenant-id (state/get-active-tenant-id)
        update-contact-request {:method :put
                                :stringify-keys? true
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
                               :stringify-keys? true
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

(defn create-sla-request [name description active shared active-sla]
  (let [tenant-id (state/get-active-tenant-id)
        create-sla-request (cond-> {:method :post
                                            :url (iu/api-url "tenants/:tenant-id/slas"
                                                   {:tenant-id tenant-id})
                                            :body {:active (if (nil? active-sla) false active)
                                                   :shared (or shared false)}}
                                   (not (nil? name))        (assoc-in [:body :name] name)
                                   (not (nil? description)) (assoc-in [:body :description] description)
                                   (not (nil? active-sla))  (assoc-in [:body :active-sla] active-sla))]
    (api/api-request create-sla-request)))

(defn update-sla-request [sla-id name description active shared active-version]
  (let [tenant-id (state/get-active-tenant-id)
        update-sla-request (cond-> {:method :put
                                            :url (iu/api-url "tenants/:tenant-id/slas/:sla-id"
                                                   {:tenant-id tenant-id :sla-id sla-id})}
                                   (not (nil? name))            (assoc-in [:body :name] name)
                                   (not (nil? description))     (assoc-in [:body :description] description)
                                   (not (nil? active))          (assoc-in [:body :active] active)
                                   (not (nil? shared))          (assoc-in [:body :shared] shared)
                                   (not (nil? active-version))  (assoc-in [:body :active-version] active-version))]
    (api/api-request update-sla-request)))

(defn create-sla-version-request [sla-id version-name sla-threshold abandon-type abandon-threshold description]
  (let [tenant-id (state/get-active-tenant-id)
        create-sla-version-request (cond-> {:method :post
                                            :url (iu/api-url "tenants/:tenant-id/slas/:sla-id/versions"
                                                  {:tenant-id tenant-id :sla-id sla-id})}
                                         (not (nil? version-name))            (assoc-in [:body :version-name] version-name)
                                         (not (nil? description))             (assoc-in [:body :description] description)
                                         (not (nil? abandon-type))            (assoc-in [:body :abandon-type] abandon-type)
                                         (not (nil? sla-threshold))           (assoc-in [:body :sla-threshold] sla-threshold)
                                         (= abandon-type "ignore-abandons")   (assoc-in [:body :abandon-threshold] abandon-threshold)
                                         (= abandon-type "count-against-sla") (assoc-in [:body :abandon-threshold] 0))]
    (api/api-request create-sla-version-request)))

(defn create-integration-request [name description active integration-type properties]
  (let [tenant-id (state/get-active-tenant-id)
        create-integration-request (cond-> {:method :post
                                            :url (iu/api-url "tenants/:tenant-id/integrations"
                                                    {:tenant-id tenant-id})
                                            :body {:active (or active false)}}
                                      (not (nil? name))             (assoc-in [:body :name] name)
                                      (not (nil? description))      (assoc-in [:body :description] description)
                                      (not (nil? integration-type)) (assoc-in [:body :type] integration-type)
                                      (not (nil? properties))       (assoc-in [:body :properties] properties))]
    (api/api-request create-integration-request)))

(defn update-integration-request [integration-id name description active properties]
  (let [tenant-id (state/get-active-tenant-id)
        update-integration-request (cond-> {:method :put
                                            :preserve-casing? true
                                            :url (iu/api-url "tenants/:tenant-id/integrations/:integration-id"
                                                    {:tenant-id tenant-id :integration-id integration-id})}
                                      (not (nil? name))         (assoc-in [:body :name] name)
                                      (not (nil? description))  (assoc-in [:body :description] description)
                                      (not (nil? active))       (assoc-in [:body :active] active)
                                      (not (nil? properties))   (assoc-in [:body :properties] properties))]
    (api/api-request update-integration-request)))

(defn create-integration-listener-request [integration-id name active properties listener-type]
  (let [tenant-id (state/get-active-tenant-id)
        create-integration-listener-request (cond-> {:method :post
                                                     :url (iu/api-url "tenants/:tenant-id/integrations/:integration-id/listeners"
                                                            {:tenant-id tenant-id :integration-id integration-id})}
                                              (not (nil? name))       (assoc-in [:body :name] name)
                                              (not (nil? active))     (assoc-in [:body :active] active)
                                              (not (nil? properties)) (assoc-in [:body :properties] properties)
                                              listener-type (assoc-in [:body :listener-type] listener-type))]
    (api/api-request create-integration-listener-request)))

(defn update-integration-listener-request [integration-id listener-id name active properties listener-type]
  (let [tenant-id (state/get-active-tenant-id)
        update-integration-listener-request (cond-> {:method :put
                                                     :url (iu/api-url "tenants/:tenant-id/integrations/:integration-id/listeners/:listener-id"
                                                            {:tenant-id tenant-id :integration-id integration-id :listener-id listener-id})}
                                              (not (nil? name))       (assoc-in [:body :name] name)
                                              (not (nil? active))     (assoc-in [:body :active] active)
                                              (not (nil? properties)) (assoc-in [:body :properties] properties)
                                              listener-type (assoc-in [:body :listener-type] listener-type))]
    (api/api-request update-integration-listener-request)))

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

(defn platform-crud-entity-request
  ([method entity-type entity-id entity-body]
   (let [url (iu/api-url
              (str entity-type "s/" entity-id))
         request (cond-> {:method method
                          :url url}
                  entity-body (assoc :body entity-body))]
    (api/api-request request))))

(defn get-crud-entity-request 
  ([entity-map]
   (get-crud-entity-request entity-map {}))
  ([entity-map options-map]
   (let [tenant-id-param (first (filterv (fn [k] (:tenant-id k)) entity-map))
         tenant-id (or (:tenant-id tenant-id-param) (state/get-active-tenant-id))
         url (iu/construct-api-url (into ["tenants" tenant-id] (remove (fn [k] (map? k)) entity-map)))
         get-request {
               :method :get
               :url url
               :preserve-casing? (= (first entity-map) "integrations")}]
     (api/api-request (merge get-request options-map)))))

(defn crud-url
  ([entity-vector api-version]
   (crud-url entity-vector api-version nil nil))
  ([entity-vector api-version tenant-id platform-entity]
   (let [tenant-id (or tenant-id (state/get-active-tenant-id))
         url (if platform-entity
                (iu/construct-api-url entity-vector)
                (iu/construct-api-url (into ["tenants" tenant-id] entity-vector)))]
    (if api-version
      (string/replace-first url #"v\d{1}" api-version)
      url))))

(defn api-create-request [entity-vector body api-version]
    (api/api-request {:method :post
                      :preserve-casing? (ih/is-entity-request-and-response-preserve-casing (first entity-vector))
                      :url (crud-url entity-vector api-version)
                      :body body}))

(defn api-read-request [entity-vector api-version tenant-id platform-entity]
    (api/api-request {:method :get 
                      :preserve-casing? (ih/is-entity-request-and-response-preserve-casing (first entity-vector))
                      :url (crud-url entity-vector api-version tenant-id platform-entity)}))

(defn api-update-request [entity-vector body api-version]
    (api/api-request {:method :put 
                      :preserve-casing? (ih/is-entity-request-and-response-preserve-casing (first entity-vector))
                      :url (crud-url entity-vector api-version) 
                      :body body}))

(defn api-delete-request [entity-vector api-version]
    (api/api-request {:method :delete :url (crud-url entity-vector api-version)}))

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

(defn get-users-request [method entity-type exclude-offline tenant-id]
  (let [tenant-id (or tenant-id (state/get-active-tenant-id))
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

(defn get-branding-request [tenant-id]
  (let [tenant-id (or tenant-id (state/get-active-tenant-id))
        get-branding-request {:method :get
                              :url (iu/api-url
                                    "tenants/:tenant-id/branding"
                                    {:tenant-id tenant-id})}]
    (api/api-request get-branding-request)))

(defn get-protected-branding-request [tenant-id]
  (let [tenant-id (or tenant-id (state/get-active-tenant-id))
        get-protected-branding-request {:method :get
                                        :url (iu/api-url
                                              "tenants/:tenant-id/protected-brandings"
                                              {:tenant-id tenant-id})}]
    (api/api-request get-protected-branding-request)))

(defn update-branding-request [tenant-id styles logo favicon]
  (let [update-branding-request (cond-> {:method :put
                                         :url (iu/api-url "tenants/:tenant-id/branding"
                                                  {:tenant-id tenant-id})}
                                 styles               (assoc-in [:body :styles] (str styles))
                                 (not (nil? logo))    (assoc-in [:body :logo] logo)
                                 (not (nil? favicon)) (assoc-in [:body :favicon] favicon))]
      (api/api-request update-branding-request)))

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

(defn query-params
  "Turn a map of parameters into a url query string."
  [params]
  (string/join "&"
               (for [[k v] params]
                 (str (name k) "=" v))))

(defn get-dashboards-request [method entity-type exclude-inactive without-active-dashboard]
   (let [query-parameters (cond-> {}
                                (true? exclude-inactive) (assoc :active true)
                                (true? without-active-dashboard) (assoc :without-active-dashboard true))]
    (let [tenant-id (state/get-active-tenant-id)
          url (str "tenants/:tenant-id/" entity-type "s")
          url (str url "?" (query-params query-parameters))
          get-url (iu/api-url
                   url
                   {:tenant-id tenant-id})
          get-request {:method method
                       :url get-url}]
      (api/api-request get-request))))

(defn get-timezones-request []
  (let [url (iu/api-url "timezones")
        request {:method "get"
                 :url url}]
    (api/api-request request)))

(defn get-regions-request []
  (let [url (iu/api-url "regions")
        request {:method "get"
                 :url url}]
    (api/api-request request)))

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

(defn create-list-request [list-type-id name shared items active]
  (let [tenant-id (state/get-active-tenant-id)
        create-list-request {:method :post
                             :url (iu/api-url
                                   "tenants/:tenant-id/lists"
                                   {:tenant-id tenant-id})
                             :body {:list-type-id list-type-id
                                    :name name
                                    :shared shared
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

(defn update-list-request [list-id name shared active]
  (let [tenant-id (state/get-active-tenant-id)
        update-list-request (cond-> {:method :put
                                     :url (iu/api-url "tenants/:tenant-id/lists/:list-id"
                                                      {:tenant-id tenant-id
                                                       :list-id list-id})}
                              list-id             (assoc-in [:body :list-id] list-id)
                              name                (assoc-in [:body :name]    name)
                              (not (nil? shared)) (assoc-in [:body :shared] shared)
                              (not (nil? active)) (assoc-in [:body :active] active))]
    (api/api-request update-list-request)))

(defn update-outbound-identifier-request [outbound-identifier-id name active value flow-id channel-type description]
  (let [tenant-id (state/get-active-tenant-id)
        update-outbound-identifier-request (cond-> {:method :put
                                                    :url (iu/api-url "tenants/:tenant-id/outbound-identifiers/:outbound-identifier-id"
                                                                    {:tenant-id tenant-id
                                                                     :outbound-identifier-id outbound-identifier-id})}
                                            outbound-identifier-id    (assoc-in [:body :outbound-identifier-id] outbound-identifier-id)
                                            (not (nil? name))         (assoc-in [:body :name] name)
                                            (not (nil? value))        (assoc-in [:body :value] value)
                                            (not (nil? flow-id))      (assoc-in [:body :flow-id] flow-id)
                                            (not (nil? channel-type)) (assoc-in [:body :channel-type] channel-type)
                                            (not (nil? description))  (assoc-in [:body :description] description)
                                            (not (nil? active))       (assoc-in [:body :active] active))]
    (api/api-request update-outbound-identifier-request)))

(defn update-outbound-identifier-list-request [outbound-identifier-list-id active name description]
  (let [tenant-id (state/get-active-tenant-id)
        update-outbound-identifier-list-request (cond-> {:method :put
                                                         :url (iu/api-url "tenants/:tenant-id/outbound-identifier-lists/:outbound-identifier-list-id"
                                                                             {:tenant-id tenant-id
                                                                              :outbound-identifier-list-id outbound-identifier-list-id})}
                                                 outbound-identifier-list-id (assoc-in [:body :outbound-identifier-list-id] outbound-identifier-list-id)
                                                 (not (nil? active))         (assoc-in [:body :active] active)
                                                 (not (nil? name))           (assoc-in [:body :name] name)
                                                 (not (nil? description))    (assoc-in [:body :description] description))]
    (api/api-request update-outbound-identifier-list-request)))

(defn create-outbound-identifier-request [name active value flow-id channel-type description]
  (let [tenant-id (state/get-active-tenant-id)
        create-outbound-identifier-request (cond-> {:method :post
                                                    :url (iu/api-url "tenants/:tenant-id/outbound-identifiers"
                                                                    {:tenant-id tenant-id})}
                                            name                     (assoc-in [:body :name] name)
                                            value                    (assoc-in [:body :value] value)
                                            flow-id                  (assoc-in [:body :flow-id] flow-id)
                                            channel-type             (assoc-in [:body :channel-type] channel-type)
                                            (not (nil? description)) (assoc-in [:body :description] description)
                                            active                   (assoc-in [:body :active] active))]
    (api/api-request create-outbound-identifier-request)))

(defn create-outbound-identifier-list-request [active name description]
  (let [tenant-id (state/get-active-tenant-id)
        create-outbound-identifier-request (cond-> {:method :post
                                                    :url (iu/api-url "tenants/:tenant-id/outbound-identifier-lists"
                                                                    {:tenant-id tenant-id})}
                                            (not (nil? active))      (assoc-in [:body :active] active)
                                            name                     (assoc-in [:body :name] name)
                                            (not (nil? description)) (assoc-in [:body :description] description))]
    (api/api-request create-outbound-identifier-request)))

(defn create-data-access-report-request [name description active realtime-report-id report-type realtime-report-type realtime-report-name historical-catalog-name users]
  (let [tenant-id (state/get-active-tenant-id)
        create-data-access-report-request (cond-> {:method :post
                                                   :url (iu/api-url "tenants/:tenant-id/data-access-reports"
                                                            {:tenant-id tenant-id})
                                                   :body    {:members (or users [])}}
                                            (not (nil? name))             (assoc-in [:body :name] name)
                                            (not (nil? description))      (assoc-in [:body :description] description)
                                            (not (nil? active))           (assoc-in [:body :active] active)
                                            (not (nil? report-type))      (assoc-in [:body :report-type] report-type)
                                            (= report-type "realtime")    (assoc-in [:body :realtime-report-id] realtime-report-id)
                                            (= report-type "realtime")    (assoc-in [:body :realtime-report-type] realtime-report-type)
                                            (= report-type "realtime")    (assoc-in [:body :realtime-report-name] realtime-report-name)
                                            (= report-type "realtime")    (assoc-in [:body :historical-catalog-name] nil)
                                            (= report-type "historical")  (assoc-in [:body :realtime-report-id] nil)
                                            (= report-type "historical")  (assoc-in [:body :realtime-report-type] nil)
                                            (= report-type "historical")  (assoc-in [:body :realtime-report-name] nil)
                                            (= report-type "historical")  (assoc-in [:body :historical-catalog-name] historical-catalog-name))]
    (api/api-request create-data-access-report-request)))

(defn update-data-access-report-request [data-access-report-id name description active realtime-report-id report-type realtime-report-type realtime-report-name historical-catalog-name users]
  (let [tenant-id (state/get-active-tenant-id)
        update-data-access-report-request (cond-> {:method :put
                                                   :url (iu/api-url "tenants/:tenant-id/data-access-reports/:data-access-report-id"
                                                            {:tenant-id tenant-id
                                                             :data-access-report-id data-access-report-id})}
                                            (not (nil? name))             (assoc-in [:body :name] name)
                                            (not (nil? description))      (assoc-in [:body :description] description)
                                            (not (nil? active))           (assoc-in [:body :active] active)
                                            (not (nil? report-type))      (assoc-in [:body :report-type] report-type)
                                            (not (nil? users))            (assoc-in [:body :members] users)
                                            (= report-type "realtime")    (assoc-in [:body :realtime-report-id] realtime-report-id)
                                            (= report-type "realtime")    (assoc-in [:body :realtime-report-type] realtime-report-type)
                                            (= report-type "realtime")    (assoc-in [:body :realtime-report-name] realtime-report-name)
                                            (= report-type "realtime")    (assoc-in [:body :historical-catalog-name] nil)
                                            (= report-type "historical")  (assoc-in [:body :realtime-report-id] nil)
                                            (= report-type "historical")  (assoc-in [:body :realtime-report-type] nil)
                                            (= report-type "historical")  (assoc-in [:body :realtime-report-name] nil)
                                            (= report-type "historical")  (assoc-in [:body :historical-catalog-name] historical-catalog-name))]
    (api/api-request update-data-access-report-request)))

(defn create-skill-request [name description active has-proficiency]
  (let [tenant-id (state/get-active-tenant-id)
        create-skill-request (cond-> {:method :post
                                      :url (iu/api-url "tenants/:tenant-id/skills"
                                                       {:tenant-id tenant-id})}
                               (not (nil? name))            (assoc-in [:body :name] name)
                               (not (nil? description))     (assoc-in [:body :description] description)
                               (not (nil? active))          (assoc-in [:body :active] active)
                               (not (nil? has-proficiency)) (assoc-in [:body :has-proficiency] has-proficiency))]
    (api/api-request create-skill-request)))

(defn update-skill-request [skill-id name description active has-proficiency]
  (let [tenant-id (state/get-active-tenant-id)
        update-skill-request (cond-> {:method :put
                                      :url (iu/api-url "tenants/:tenant-id/skills/:skill-id"
                                                       {:tenant-id tenant-id :skill-id skill-id})}
                               (not (nil? name))            (assoc-in [:body :name] name)
                               (not (nil? description))     (assoc-in [:body :description] description)
                               (not (nil? active))          (assoc-in [:body :active] active)
                               (not (nil? has-proficiency)) (assoc-in [:body :has-proficiency] has-proficiency))]
    (api/api-request update-skill-request)))

(defn update-user-skill-member-request [user-id skill-id proficiency]
  (let [tenant-id (state/get-active-tenant-id)
        update-user-skill-member-request {:method :put
                                          :url (iu/api-url "tenants/:tenant-id/users/:user-id/skills/:skill-id"
                                                           {:tenant-id tenant-id :user-id user-id :skill-id skill-id})
                                          :body {:proficiency proficiency}}]
    (api/api-request update-user-skill-member-request)))

(defn update-reason-request [reason-id name description external-id active shared]
  (let [tenant-id (state/get-active-tenant-id)
        update-reason-request (cond-> {:method :put
                                       :url (iu/api-url "tenants/:tenant-id/reasons/:reason-id"
                                                        {:tenant-id tenant-id :reason-id reason-id})}
                                (not (nil? name))            (assoc-in [:body :name] name)
                                (not (nil? description))     (assoc-in [:body :description] description)
                                (not (nil? external-id))     (assoc-in [:body :external-id] external-id)
                                (not (nil? active))          (assoc-in [:body :active] active)
                                (not (nil? shared))          (assoc-in [:body :shared] shared))]
    (api/api-request update-reason-request)))

(defn create-dispatch-mapping-request [name description value flow-id version channel-type interaction-field active]
  (let [tenant-id (state/get-active-tenant-id)
        create-dispatch-mapping-request (cond-> {:method :post
                                                 :url (iu/api-url "tenants/:tenant-id/dispatch-mappings"
                                                                  {:tenant-id tenant-id})
                                                 :body {:version (or version nil)}}
                                          (not (nil? name))                             (assoc-in [:body :name] name)
                                          (not (nil? description))                      (assoc-in [:body :description] description)
                                          (not (nil? value))                            (assoc-in [:body :value] value)
                                          (not (nil? flow-id))                          (assoc-in [:body :flow-id] flow-id)
                                          (not (nil? channel-type))                     (assoc-in [:body :channel-type] channel-type)
                                          (not (nil? interaction-field))                (assoc-in [:body :interaction-field] interaction-field)
                                          (not (nil? active))                           (assoc-in [:body :active] active))]
    (api/api-request create-dispatch-mapping-request)))
  
(defn update-dispatch-mapping-request [dispatch-mapping-id name description value flow-id version interaction-field channel-type active]
  (let [tenant-id (state/get-active-tenant-id)
        update-dispatch-mapping-request (cond-> {:method :put
                                                 :url (iu/api-url "tenants/:tenant-id/dispatch-mappings/:dispatch-mapping-id"
                                                                  {:tenant-id tenant-id :dispatch-mapping-id dispatch-mapping-id})
                                                 :body {:version (or version nil)}}
                                          (not (nil? name))                             (assoc-in [:body :name] name)
                                          (not (nil? description))                      (assoc-in [:body :description] description)
                                          (not (nil? value))                            (assoc-in [:body :value] value)
                                          (not (nil? flow-id))                          (assoc-in [:body :flow-id] flow-id)
                                          (not (nil? channel-type))                     (assoc-in [:body :channel-type] channel-type)
                                          (not (nil? interaction-field))                (assoc-in [:body :interaction-field] interaction-field)
                                          (not (nil? active))                           (assoc-in [:body :active] active))]
    (api/api-request update-dispatch-mapping-request)))

(defn create-reason-request [name description external-id active shared]
  (let [tenant-id (state/get-active-tenant-id)
        create-reason-request (cond-> {:method :post
                                       :url (iu/api-url "tenants/:tenant-id/reasons"
                                                        {:tenant-id tenant-id})}
                                (not (nil? name))            (assoc-in [:body :name] name)
                                (not (nil? description))     (assoc-in [:body :description] description)
                                (not (nil? external-id))     (assoc-in [:body :external-id] external-id)
                                (not (nil? active))          (assoc-in [:body :active] active)
                                (not (nil? shared))          (assoc-in [:body :shared] shared))]
    (api/api-request create-reason-request)))

(defn create-reason-list-request [name description external-id active shared reasons is-default]
  (let [tenant-id (state/get-active-tenant-id)
        create-reason-list-request (cond-> {:method :post
                                            :url (iu/api-url "tenants/:tenant-id/reason-lists"
                                                        {:tenant-id tenant-id})}
                                    (not (nil? name))            (assoc-in [:body :name] name)
                                    (not (nil? description))     (assoc-in [:body :description] description)
                                    (not (nil? external-id))     (assoc-in [:body :external-id] external-id)
                                    (not (nil? active))          (assoc-in [:body :active] active)
                                    (not (nil? shared))          (assoc-in [:body :shared] shared)
                                    (not (nil? reasons))         (assoc-in [:body :reasons] reasons)
                                    (not (nil? is-default))      (assoc-in [:body :is-default] is-default))]
    (api/api-request create-reason-list-request)))

(defn update-reason-list-request [reason-list-id name description external-id active shared reasons is-default]
  (let [tenant-id (state/get-active-tenant-id)
        update-reason-list-request (cond-> {:method :put
                                             :url (iu/api-url "tenants/:tenant-id/reason-lists/:reason-list-id"
                                                              {:tenant-id tenant-id :reason-list-id reason-list-id})}
                                    (not (nil? name))            (assoc-in [:body :name] name)
                                    (not (nil? description))     (assoc-in [:body :description] description)
                                    (not (nil? external-id))     (assoc-in [:body :external-id] external-id)
                                    (not (nil? active))          (assoc-in [:body :active] active)
                                    (not (nil? shared))          (assoc-in [:body :shared] shared)
                                    (not (nil? reasons))         (assoc-in [:body :reasons] reasons)
                                    (not (nil? is-default))      (assoc-in [:body :is-default] is-default))]
    (api/api-request update-reason-list-request)))

(defn create-disposition-list-request [name description external-id active shared dispositions]
  (let [tenant-id (state/get-active-tenant-id)
        create-disposition-list-request (cond-> {:method :post
                                                 :url (iu/api-url "tenants/:tenant-id/disposition-lists"
                                                            {:tenant-id tenant-id})}
                                          (not (nil? name))            (assoc-in [:body :name] name)
                                          (not (nil? description))     (assoc-in [:body :description] description)
                                          (not (nil? external-id))     (assoc-in [:body :external-id] external-id)
                                          (not (nil? active))          (assoc-in [:body :active] active)
                                          (not (nil? shared))          (assoc-in [:body :shared] shared)
                                          (not (nil? dispositions))    (assoc-in [:body :dispositions] dispositions))]
    (api/api-request create-disposition-list-request)))

(defn update-disposition-list-request [disposition-list-id name description external-id active shared dispositions]
  (let [tenant-id (state/get-active-tenant-id)
        update-disposition-list-request (cond-> {:method :put
                                                 :url (iu/api-url "tenants/:tenant-id/disposition-lists/:disposition-list-id"
                                                              {:tenant-id tenant-id :disposition-list-id disposition-list-id})}
                                          (not (nil? name))            (assoc-in [:body :name] name)
                                          (not (nil? description))     (assoc-in [:body :description] description)
                                          (not (nil? external-id))     (assoc-in [:body :external-id] external-id)
                                          (not (nil? active))          (assoc-in [:body :active] active)
                                          (not (nil? shared))          (assoc-in [:body :shared] shared)
                                          (not (nil? dispositions))    (assoc-in [:body :dispositions] dispositions))]
    (api/api-request update-disposition-list-request)))

(defn create-flow-request [flow-type name active description]
  (let [tenant-id (state/get-active-tenant-id)
        create-flow-request (cond-> {:method :post
                                     :url (iu/api-url "tenants/:tenant-id/flows"
                                                       {:tenant-id tenant-id})
                                     :body {:active (or active false)}}
                                    (not (nil? flow-type))    (assoc-in [:body :type] flow-type)
                                    (not (nil? name))         (assoc-in [:body :name] name)
                                    (not (nil? description))  (assoc-in [:body :description] description))]
    (api/api-request create-flow-request)))

(defn update-flow-request [flow-id flow-type name active-version active description]
  (let [tenant-id (state/get-active-tenant-id)
        update-flow-request (cond-> {:method :put
                                     :url (iu/api-url "tenants/:tenant-id/flows/:flow-id"
                                                       {:tenant-id tenant-id :flow-id flow-id})}
                                    (not (nil? flow-type))      (assoc-in [:body :type] flow-type)
                                    (not (nil? name))           (assoc-in [:body :name] name)
                                    (not (nil? active-version)) (assoc-in [:body :active-version] active-version)
                                    (not (nil? description))    (assoc-in [:body :description] description)
                                    (not (nil? active))         (assoc-in [:body :active] active))]
    (api/api-request update-flow-request)))

(defn create-flow-draft-request [flow-id name flow metadata description]
  (let [tenant-id (state/get-active-tenant-id)
        create-flow-draft-request (cond-> {:method :post
                                           :url (iu/api-url "tenants/:tenant-id/flows/:flow-id/drafts"
                                                  {:tenant-id tenant-id :flow-id flow-id})
                                           :body  {:flow (or flow "[]")}}
                                          (not (nil? name))         (assoc-in [:body :name] name)
                                          (not (nil? metadata))     (assoc-in [:body :metadata] metadata)
                                          (not (nil? description))  (assoc-in [:body :description] description))]
    (api/api-request create-flow-draft-request)))

(defn remove-flow-draft-request [flow-id draft-id]
  (let [tenant-id (state/get-active-tenant-id)
        remove-flow-draft-request {:method :delete
                                   :preserve-casing? true
                                   :url (iu/api-url
                                         "tenants/:tenant-id/flows/:flow-id/drafts/:draft-id"
                                         {:tenant-id tenant-id :flow-id flow-id :draft-id draft-id})}]
    (api/api-request remove-flow-draft-request)))

(defn create-disposition-request [name description external-id active shared]
  (let [tenant-id (state/get-active-tenant-id)
        create-disposition-request (cond-> {:method :post
                                            :url (iu/api-url "tenants/:tenant-id/dispositions"
                                                        {:tenant-id tenant-id})}
                                    (not (nil? name))            (assoc-in [:body :name] name)
                                    (not (nil? description))     (assoc-in [:body :description] description)
                                    (not (nil? external-id))     (assoc-in [:body :external-id] external-id)
                                    (not (nil? active))          (assoc-in [:body :active] active)
                                    (not (nil? shared))          (assoc-in [:body :shared] shared))]
    (api/api-request create-disposition-request)))

(defn update-disposition-request [disposition-id name description external-id active shared]
  (let [tenant-id (state/get-active-tenant-id)
        update-disposition-request (cond-> {:method :put
                                            :url (iu/api-url "tenants/:tenant-id/dispositions/:disposition-id"
                                                              {:tenant-id tenant-id :disposition-id disposition-id})}
                                    (not (nil? name))            (assoc-in [:body :name] name)
                                    (not (nil? description))     (assoc-in [:body :description] description)
                                    (not (nil? external-id))     (assoc-in [:body :external-id] external-id)
                                    (not (nil? active))          (assoc-in [:body :active] active)
                                    (not (nil? shared))          (assoc-in [:body :shared] shared))]
    (api/api-request update-disposition-request)))

(defn dissociate-request [origin-entity destination-entity]
  (let [tenant-id (state/get-active-tenant-id)
        request-data {:method :delete
                      :preserve-casing? true
                      :url (iu/construct-api-url (into ["tenants" (state/get-active-tenant-id)] [(:name origin-entity) (:id origin-entity) (:name destination-entity) (:id destination-entity)]))}]
    (api/api-request request-data)))

(defn update-users-capacity-request [user-id id]
  (let [tenant-id (state/get-active-tenant-id)
        update {:method :post
                :url (iu/construct-api-url (into ["tenants" tenant-id] ["users" user-id "capacity-rules"]))
                :body {:capacity-rule-id id}}]
    (api/api-request update)))

(defn associate-request [origin-entity destination-entity]
  (let [tenant-id (state/get-active-tenant-id)
        url (iu/construct-api-url (into ["tenants" tenant-id] [(:name origin-entity) (:id origin-entity) (:name destination-entity)]))

        body {:tenant-id tenant-id}
        body (assoc body (keyword (str (string/join "" (drop-last (:name origin-entity))) "-id")) (:id origin-entity))
        body (assoc body (keyword (str (string/join "" (drop-last (:name destination-entity))) "-id")) (:id destination-entity))
        request-data {:method :post
                      :url url
                      :body body}]
    (api/api-request request-data)))

(defn create-group-request [name description active]
  (let [tenant-id (state/get-active-tenant-id)
        create-group-request (cond-> {:method :post
                                      :url (iu/api-url "tenants/:tenant-id/groups"
                                                       {:tenant-id tenant-id})
                                      :body {:owner (state/get-active-user-id)}}
                               (not (nil? name))        (assoc-in [:body :name] name)
                               (not (nil? description)) (assoc-in [:body :description] description)
                               (not (nil? active))      (assoc-in [:body :active] active))]
    (api/api-request create-group-request)))

(defn update-group-request [group-id name description active]
  (let [tenant-id (state/get-active-tenant-id)
        update-group-request (cond-> {:method :put
                                      :url (iu/api-url "tenants/:tenant-id/groups/:group-id"
                                                       {:tenant-id tenant-id :group-id group-id})}
                               (not (nil? name))            (assoc-in [:body :name] name)
                               (not (nil? description))     (assoc-in [:body :description] description)
                               (not (nil? active))          (assoc-in [:body :active] active))]
    (api/api-request update-group-request)))

(defn create-user-request [email, role-id, platform-role-id, default-identity-provider, no-password, status, work-station-id, external-id, extensions, first-name, last-name, capacity-rule-id]
  (let [tenant-id (state/get-active-tenant-id)
        create-user-request (cond-> {:method :post
                                      :url (iu/api-url "tenants/:tenant-id/users"
                                                       {:tenant-id tenant-id})}
                                    (not (nil? email))                      (assoc-in [:body :email] email)
                                    (not (nil? role-id))                    (assoc-in [:body :role-id] role-id)
                                    (not (nil? platform-role-id))           (assoc-in [:body :platform-role-id] platform-role-id)
                                    (not (nil? default-identity-provider))  (assoc-in [:body :default-identity-provider] default-identity-provider)
                                    (not (nil? no-password))                (assoc-in [:body :no-password] no-password)
                                    (not (nil? status))                     (assoc-in [:body :status] status)
                                    (not (nil? work-station-id))            (assoc-in [:body :work-station-id] work-station-id)
                                    (not (nil? external-id))                (assoc-in [:body :external-id] external-id)
                                    (and
                                      (not (nil? extensions))
                                      (not (empty? extensions)))            (assoc-in [:body :extensions] extensions)
                                    (not (nil? first-name))                 (assoc-in [:body :first-name] first-name)
                                    (not (nil? last-name))                  (assoc-in [:body :last-name] last-name)
                                    (not (nil? capacity-rule-id))           (assoc-in [:body :capacity-rule-id] capacity-rule-id))]
    (api/api-request create-user-request)))

(defn create-role-request [name description permissions active shared]
  (let [tenant-id (state/get-active-tenant-id)
        create-role-request (cond-> {:method :post
                                     :url (iu/api-url "tenants/:tenant-id/roles"
                                                      {:tenant-id tenant-id})
                                     :body            {:permissions (or permissions [])}}
                             name                      (assoc-in [:body :name] name)
                             (not (nil? description))  (assoc-in [:body :description] description)
                             (not (nil? active))       (assoc-in [:body :active] active)
                             (not (nil? shared))       (assoc-in [:body :shared] shared))]
    (api/api-request create-role-request)))

(defn update-role-request [role-id name description permissions active shared tenant-id]
  (let [tenant-id (or tenant-id (state/get-active-tenant-id))
        update-role-request (cond-> {:method :put
                                     :url (iu/api-url "tenants/:tenant-id/roles/:role-id"
                                                      {:tenant-id tenant-id
                                                       :role-id role-id})}
                             (not (nil? permissions))  (assoc-in [:body :permissions]  permissions)
                             (not (nil? name))         (assoc-in [:body :name] name)
                             (not (nil? description))  (assoc-in [:body :description] description)
                             (not (nil? active))       (assoc-in [:body :active] active)
                             (not (nil? shared))       (assoc-in [:body :shared] shared))]
    (api/api-request update-role-request)))

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

(defn delete-outbound-identifier-request [outbound-identifier-id]
  (let [tenant-id (state/get-active-tenant-id)
        delete-outbound-identifier-request {
                                            :method :delete
                                            :preserve-casing? true
                                            :url (iu/api-url
                                                  "tenants/:tenant-id/outbound-identifiers/:outbound-identifier-id"
                                                  {:tenant-id tenant-id
                                                   :outbound-identifier-id outbound-identifier-id})}]
    (api/api-request delete-outbound-identifier-request)))

(defn add-outbound-identifier-list-member-request [outbound-identifier-list-id outbound-identifier-id]
  (let [tenant-id (state/get-active-tenant-id)
        add-outbound-identifier-list-member-request {
                                                     :method :post
                                                     :url (iu/api-url
                                                           "tenants/:tenant-id/outbound-identifier-lists/:outbound-identifier-list-id/members/:outbound-identifier-id"
                                                           {:tenant-id tenant-id
                                                            :outbound-identifier-list-id outbound-identifier-list-id
                                                            :outbound-identifier-id outbound-identifier-id})
                                                     :body {:outbound-identifier-list outbound-identifier-list-id
                                                            :outbound-identifier outbound-identifier-id}}]
    (api/api-request add-outbound-identifier-list-member-request)))

(defn remove-outbound-identifier-list-member-request [outbound-identifier-list-id outbound-identifier-id]
  (let [tenant-id (state/get-active-tenant-id)
        remove-outbound-identifier-list-member-request {
                                                        :method :delete
                                                        :preserve-casing? true
                                                        :url (iu/api-url
                                                              "tenants/:tenant-id/outbound-identifier-lists/:outbound-identifier-list-id/members/:outbound-identifier-id"
                                                              {:tenant-id tenant-id
                                                               :outbound-identifier-list-id outbound-identifier-list-id
                                                               :outbound-identifier-id outbound-identifier-id})}]
    (api/api-request remove-outbound-identifier-list-member-request)))

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

(defn update-default-tenant-request [tenant-id]
  (let [resource-id (state/get-active-user-id)
        tenant-request {:method :put
                        :url (iu/api-url
                              "users/:resource-id"
                              {:resource-id resource-id})
                        :body {:default-tenant tenant-id}}]
    (api/api-request tenant-request)))

;;--------------------------------------------------------------------------- ;;
;; Transfer Lists
;;--------------------------------------------------------------------------- ;;

(defn create-transfer-list-request [name description active endpoints]
  (let [tenant-id (state/get-active-tenant-id)
        create-transfer-list-request (cond-> {:method :post
                                              :url (iu/api-url "tenants/:tenant-id/transfer-lists"
                                                               {:tenant-id tenant-id})}
                                          (not (nil? name))                             (assoc-in [:body :name] name)
                                          (not (nil? description))                      (assoc-in [:body :description] description)
                                          (not (nil? active))                           (assoc-in [:body :active] active)
                                          (not (nil? endpoints))                        (assoc-in [:body :endpoints] endpoints))]                                     
    (api/api-request create-transfer-list-request)))

(defn update-transfer-list-request [transfer-list-id name description active endpoints]
  (let [tenant-id (state/get-active-tenant-id)
        update-transfer-list-request (cond-> {:method :put
                                              :url (iu/api-url "tenants/:tenant-id/transfer-lists/:transfer-list-id"
                                                               {:tenant-id tenant-id 
                                                                :transfer-list-id transfer-list-id})}
                                          (not (nil? name))                             (assoc-in [:body :name] name)
                                          (not (nil? description))                      (assoc-in [:body :description] description)
                                          (not (nil? active))                           (assoc-in [:body :active] active)
                                          (not (nil? endpoints))                        (assoc-in [:body :endpoints] endpoints))]                                     
    (api/api-request update-transfer-list-request)))

;;--------------------------------------------------------------------------- ;;
;; Tenants Lists
;;--------------------------------------------------------------------------- ;;

(defn get-tenants-request [region-id]
  (let [get-tenants-request {:method :get
                             :url (iu/api-url
                                    "tenants?regionId=:region-id"
                                    {:region-id region-id})}]
    (api/api-request get-tenants-request)))

(defn create-tenant-request [name admin-user-id parent-id region-id timezone active description]
  (let [create-tenant-request (cond-> {:method :post
                                       :url (iu/api-url "tenants")}

                                name                     (assoc-in [:body :name] name)
                                admin-user-id            (assoc-in [:body :admin-user-id] admin-user-id)
                                parent-id                (assoc-in [:body :parent-id] parent-id)
                                region-id                (assoc-in [:body :region-id] region-id)
                                timezone                 (assoc-in [:body :timezone] timezone)
                                active                   (assoc-in [:body :active] active)
                                (not (nil? description)) (assoc-in [:body :description] description))]
    (api/api-request create-tenant-request)))

(defn update-tenant-request [tenant-id name description admin-user-id timezone outbound-integration-id cxengage-identity-provider default-identity-provider default-sla-id active]
  (let [update-tenant-request (cond-> {:method :put
                                       :url (iu/api-url "tenants/:tenant-id"
                                                      {:tenant-id tenant-id})}
                                    (not (nil? name))                       (assoc-in [:body :name] name)
                                    (not (nil? description))                (assoc-in [:body :description] description)
                                    (not (nil? admin-user-id))              (assoc-in [:body :admin-user-id] admin-user-id)
                                    (not (nil? timezone))                   (assoc-in [:body :timezone] timezone)
                                    (not (nil? outbound-integration-id))    (assoc-in [:body :outbound-integration-id] outbound-integration-id)
                                    (not (nil? cxengage-identity-provider)) (assoc-in [:body :cxengage-identity-provider] cxengage-identity-provider)
                                    (not (nil? default-identity-provider))  (assoc-in [:body :default-identity-provider] default-identity-provider)
                                    (not (nil? default-sla-id))             (assoc-in [:body :default-sla-id] default-sla-id)
                                    (not (nil? active))                     (assoc-in [:body :active] active))]
    (api/api-request update-tenant-request)))

;;--------------------------------------------------------------------------- ;;
;; Message Templates
;;--------------------------------------------------------------------------- ;;

(defn create-message-template-request [name description channels template-text-type template active]
  (let [tenant-id (state/get-active-tenant-id)
        create-message-template-request (cond-> {:method :post
                                                 :url (iu/api-url "tenants/:tenant-id/message-templates"
                                                                  {:tenant-id tenant-id})}
                                          name                            (assoc-in [:body :name] name)
                                          (not (nil? description))        (assoc-in [:body :description] description)
                                          channels                        (assoc-in [:body :channels] channels)
                                          template-text-type              (assoc-in [:body :type] template-text-type)
                                          template                        (assoc-in [:body :template] template)
                                          active                          (assoc-in [:body :active] active))]
    (api/api-request create-message-template-request)))

(defn update-message-template-request [message-template-id name description channels template-text-type template active]
  (let [tenant-id (state/get-active-tenant-id)
        update-message-template-request (cond-> {:method :put
                                                 :url (iu/api-url "tenants/:tenant-id/message-templates/:message-template-id"
                                                                  {:tenant-id tenant-id 
                                                                   :message-template-id message-template-id})}
                                          (not (nil? name))                             (assoc-in [:body :name] name)
                                          (not (nil? description))                      (assoc-in [:body :description] description)
                                          (not (nil? channels))                         (assoc-in [:body :channels] channels)
                                          (not (nil? template-text-type))               (assoc-in [:body :type] template-text-type)
                                          (not (nil? template))                         (assoc-in [:body :template] template)
                                          (not (nil? active))                           (assoc-in [:body :active] active))]
    (api/api-request update-message-template-request)))

;;--------------------------------------------------------------------------- ;;
;; Business Hours
;;--------------------------------------------------------------------------- ;;

(defn create-business-hour-request [name timezone time-minutes active description]
      (let [tenant-id (state/get-active-tenant-id)
            create-business-hour-request (cond->  {:method :post
                                                    :url (iu/api-url "tenants/:tenant-id/business-hours"
                                                                   {:tenant-id tenant-id})
                                                    :body (merge {:tenant-id tenant-id
                                                                  :name name
                                                                  :active active
                                                                  :timezone timezone}
                                                                 time-minutes)}
                                                  (not (nil? description)) (assoc-in [:body :description] description))]
           (api/api-request create-business-hour-request)))

(defn update-business-hour-request [business-hours-id name timezone time-minutes active description]
      (let [tenant-id (state/get-active-tenant-id)
            update-business-hour-request (cond-> {:method :put
                                                  :url (iu/api-url "tenants/:tenant-id/business-hours/:business-hours-id"
                                                            {:tenant-id tenant-id
                                                             :business-hours-id business-hours-id})
                                                  :body {:tenant-id tenant-id}}
                                                 (not (nil? active)) (assoc-in [:body :active] active)
                                                  (not (nil? timezone)) (assoc-in [:body :timezone] timezone)
                                                  (not (nil? name)) (assoc-in [:body :name] name)
                                                  (not (nil? description)) (assoc-in [:body :description] description)
                                                  (not (nil? time-minutes)) (update :body merge time-minutes))]
           (api/api-request update-business-hour-request)))

(defn create-exception-request [business-hour-id date is-all-day description start-time-minutes end-time-minutes]
      (let [tenant-id (state/get-active-tenant-id)
            create-exception-request (cond->  {:method :post
                                                   :url (iu/api-url "tenants/:tenant-id/business-hours/:business-hour-id/exceptions"
                                                                    {:tenant-id tenant-id
                                                                     :business-hour-id business-hour-id})
                                                   :body {:date date
                                                          :is-all-day is-all-day
                                                          :start-time-minutes start-time-minutes
                                                          :end-time-minutes end-time-minutes}}
                                              (not (nil? description)) (assoc-in [:body :description] description))]
           (api/api-request create-exception-request)))

(defn delete-exception-request [business-hour-id exception-id]
      (let [tenant-id (state/get-active-tenant-id)
            delete-exception-request {:method :delete
                                      :url (iu/api-url "tenants/:tenant-id/business-hours/:business-hour-id/exceptions/:exception-id"
                                                       {:tenant-id tenant-id
                                                        :business-hour-id business-hour-id
                                                        :exception-id exception-id})}]
           (api/api-request delete-exception-request)))

(defn create-api-key-request [name description role-id]
  (let [tenant-id (state/get-active-tenant-id)
        create-api-key-request (cond-> {:method :post
                                        :url (iu/api-url "tenants/:tenant-id/api-keys"
                                                         {:tenant-id tenant-id})}
                                 (not (nil? name))            (assoc-in [:body :name] name)
                                 (not (nil? description))     (assoc-in [:body :description] description)
                                 (not (nil? role-id))         (assoc-in [:body :role-id] role-id))]
    (api/api-request create-api-key-request)))

(defn update-api-key-request [api-key-id name description role-id active]
  (let [tenant-id (state/get-active-tenant-id)
        update-api-key-request (cond-> {:method :put
                                        :url (iu/api-url "tenants/:tenant-id/api-keys/:api-key-id"
                                                         {:tenant-id tenant-id :api-key-id api-key-id})}
                                 (not (nil? name))            (assoc-in [:body :name] name)
                                 (not (nil? description))     (assoc-in [:body :description] description)
                                 (not (nil? role-id))         (assoc-in [:body :role-id] role-id)
                                 (not (nil? active))         (assoc-in [:body :status] (if active "enabled" "disabled")))]
    (api/api-request update-api-key-request)))

(defn delete-api-key-request [api-key-id]
  (let [tenant-id (state/get-active-tenant-id)
        delete-api-key-request {:method :delete
                                :preserve-casing? true
                                :url (iu/api-url
                                      "tenants/:tenant-id/api-keys/:api-key-id"
                                      {:tenant-id tenant-id
                                       :api-key-id api-key-id})}]
    (api/api-request delete-api-key-request)))
