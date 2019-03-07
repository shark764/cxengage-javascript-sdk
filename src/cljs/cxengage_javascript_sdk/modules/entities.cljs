(ns cxengage-javascript-sdk.modules.entities
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]])
  (:require [cljs.spec.alpha :as s]
            [cljs.core.async :as a]
            [clojure.set :refer [rename-keys]]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]))

;; -------------------------------------------------------------------------- ;;
;; Entity Utility Functions
;; -------------------------------------------------------------------------- ;;

(defn- add-key-to-items [list-obj]
  (let [list-item-key (get (first (get-in list-obj [:list-type :fields])) :name)
        items (js->clj (ih/camelify (get list-obj :items)) :keywordize-keys true)
        updated-items (mapv #(assoc % :key (get % (keyword list-item-key))) items)]
    (assoc list-obj :items updated-items)))

(s/def ::get-entities-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

;; -------------------------------------------------------------------------- ;;
;; GET Entity Functions
;; -------------------------------------------------------------------------- ;;

(s/def ::get-user-params
  (s/keys :req-un [::specs/resource-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-user
  "``` javascript
  CxEngage.entities.getUser({
    resourceId: {{uuid}}
  });
  ```
  Retrieves single User given parameter resourceId
  as a unique key

  Topic: cxengage/entities/get-user-response

  Possible Errors:

  - [Entities: 11000](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-user-err)"
  {:validation ::get-user-params
   :topic-key :get-user-response}
  [params]
  (let [{:keys [callback topic resource-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entity-request :get "user" resource-id))
        response {:result (merge (get-in entity-response [:api-response :result]) {:skills-with-proficiency (get-in entity-response [:api-response :result :skills])})}
        error (if-not (= 200 (:status entity-response)) (e/failed-to-get-user-err resource-id entity-response))]
    (p/publish {:topics topic
                :response response
                :error error
                :callback callback})))

(s/def ::get-platform-user-email-params
  (s/keys :req-un [::specs/email]
          :opt-un [::specs/callback]))

(def-sdk-fn get-platform-user-email
  "``` javascript
  CxEngage.entities.getPlatformUserEmail({
    email: {{email}}
  });
  ```
  Retrieves single User given email parameter

  Topic: cxengage/entities/get-platform-user-email-response

  Possible Errors:

  - [Entities: 11076](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-platform-user-email-err)"
  {:validation ::get-platform-user-email-params
   :topic-key :get-platform-user-email-response}
  [params]
  (let [{:keys [callback topic email]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-platform-user-email-request email))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-platform-user-email-err entity-response)
                  :callback callback}))))


;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getUsers();
;; -------------------------------------------------------------------------- ;;

(s/def ::get-users-params
  (s/keys :req-un []
          :opt-un [::specs/callback ::specs/exclude-offline]))

(def-sdk-fn get-users
  "``` javascript
  CxEngage.entities.getUsers();
  ```
  Retrieves available users for current logged in tenant

  Topic: cxengage/entities/get-users-response

  Possible Errors:

  - [Entities: 11001](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-users-err)"
  {:validation ::get-users-params
   :topic-key :get-users-response}
  [params]
  (let [{:keys [callback topic exclude-offline]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-users-request :get "user" exclude-offline))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-users-err entity-response)
                  :callback callback}))))

(s/def ::get-queue-params
  (s/keys :req-un [::specs/queue-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-queue
  "``` javascript
  CxEngage.entities.getQueue({
    queueId: {{uuid}}
  });
  ```
  Retrieves single queue given parameter queueId
  as a unique key

  Topic: cxengage/entities/get-queue-response

  Possible Errors:

  - [Entities: 11002](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-queue-err)"
  {:validation ::get-queue-params
   :topic-key :get-queue-response}
  [params]
  (let [{:keys [callback topic queue-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-crud-entity-request ["queues" queue-id]))
        {:keys [status api-response] :as entity-response2} (a/<! (rest/get-crud-entity-request ["queues" queue-id "versions"]))
        versions {:versions (get-in entity-response2 [:api-response :result])}
        response {:result (merge (get-in entity-response [:api-response :result]) versions)}
        error (if-not (and (= 200 (:status entity-response)) (= 200 (:status entity-response2))) (e/failed-to-get-queue-err queue-id entity-response))]
      (p/publish {:topics topic
                  :response response
                  :error error
                  :callback callback})))


;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getQueues();
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn get-queues
  ""
  {:validation ::get-entities-params
   :topic-key :get-queues-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "queue"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-queue-list-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getTransferList({
;;   transferListId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-transfer-list-params
  (s/keys :req-un [::specs/transfer-list-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-transfer-list
  ""
  {:validation ::get-transfer-list-params
   :topic-key :get-transfer-list-response}
  [params]
  (let [{:keys [callback topic transfer-list-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entity-request :get "transfer-list" transfer-list-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-transfer-list-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getTransferLists();
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn get-transfer-lists
  ""
  {:validation ::get-entities-params
   :topic-key :get-transfer-lists-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "transfer-list"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-transfer-lists-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getOutboundIdentifiers();
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn get-outbound-identifiers
  ""
  {:validation ::get-entities-params
   :topic-key :get-outbound-identifiers-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "outbound-identifier"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-outbound-identifiers-err entity-response)
                  :callback callback}))))

(s/def ::get-flows-params
  (s/keys :req-un []
          :opt-un [::specs/callback ::specs/include-notations]))

(def-sdk-fn get-flows
  "``` javascript
  CxEngage.entities.getFlows({
    includeNotations: {{boolean}} (optional)
  });
  ```
  Retrieves available Flows configured for current logged in tenant.
  Receives include-notations param to add flows that don't belong to current tenant.
  Flows included are notations used in flow designer iframe.

  Topic: cxengage/entities/get-flows-response

  Possible Errors:

  - [Entities: 11043](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-flows-err)"
  {:validation ::get-flows-params
   :topic-key :get-flows-response}
  [params]
  (let [{:keys [callback topic include-notations]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "flow"))
        ; Filtering flows that belong to active tenant, at this point we don't need
        ; flows for notations used in flow designer iframe
        response (if (true? include-notations) api-response {:result (into [] (filter #(= (:tenant-id %) (state/get-active-tenant-id)) (:result api-response)))})
        error (if-not (= status 200) (e/failed-to-get-flows-err entity-response))]
    (p/publish {:topics topic
                :response response
                :error error
                :callback callback})))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getBranding();
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn get-branding
  ""
  {:validation ::get-entities-params
   :topic-key :get-branding-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-branding-request))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-tenant-branding-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getProtectedBranding();
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn get-protected-branding
  ""
  {:validation ::get-entities-params
   :topic-key :get-protected-branding-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-protected-branding-request))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-tenant-protected-branding-err entity-response)
                  :callback callback}))))

(s/def ::get-dashboards-params
  (s/keys :req-un []
          :opt-un [::specs/callback ::specs/exclude-inactive]))

(def-sdk-fn get-dashboards
  "``` javascript
  CxEngage.entities.getDashboards();
  ```
  Retrieves available Custom Dashboards created for current logged in tenant

  Topic: cxengage/entities/get-dashboards-response

  Possible Errors:

  - [Entities: 11008](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-dashboards-err)"
  {:validation ::get-dashboards-params
   :topic-key :get-dashboards-response}
  [params]
  (let [{:keys [callback topic exclude-inactive]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-dashboards-request :get "dashboard" exclude-inactive))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-dashboards-list-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getList({
;;   listId: {{uuid}},
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-list-params
  (s/keys :req-un [::specs/list-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-list
  ""
  {:validation ::get-list-params
   :topic-key :get-list-response}
  [params]
  (let [{:keys [callback topic list-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entity-request :get "list" list-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (add-key-to-items api-response)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-list-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getListItem({
;;   listId: {{uuid}},
;;   listItemKey: {{string}},
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-list-item-params
  (s/keys :req-un [::specs/list-id ::specs/list-item-key]
          :opt-un [::specs/callback]))

(def-sdk-fn get-list-item
  ""
  {:validation ::get-list-item-params
   :topic-key :get-list-item-response}
  [params]
  (let [{:keys [callback topic list-id list-item-key]} params
        list-item-key (js/encodeURIComponent list-item-key)
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-list-item-request list-id list-item-key))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-list-item-err entity-response)
                  :callback callback}))))

;;--------------------------------------------------------------------------- ;;
;; CxEngage.entities.getLists({
;;   listTypeId: {{uuid}} (optional),
;;   name: {{string}} (optional)
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-lists-params
  (s/keys :req-un []
          :opt-un [::specs/callback ::specs/list-type-id ::specs/name]))

(def-sdk-fn get-lists
  ""
  {:validation ::get-lists-params
   :topic-key :get-lists-response}
  [params]
  (let [{:keys [callback topic list-type-id name]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-lists-request list-type-id name))]
    (if (= status 200)
      (let [lists (get-in api-response [:result])
            updated-lists (mapv #(add-key-to-items %) lists)
            api-response (assoc api-response :result updated-lists)]
        (p/publish {:topics topic
                    :response api-response
                    :callback callback}))
      (p/publish {:topics topic
                  :error (e/failed-to-get-lists-err entity-response)
                  :callback callback}))))

;;--------------------------------------------------------------------------- ;;
;; CxEngage.entities.getListTypes();
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn get-list-types
  ""
  {:validation ::get-entities-params
   :topic-key :get-list-types-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-list-types-request))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-list-types-err entity-response)
                  :callback callback}))))

(def-sdk-fn get-groups
  "``` javascript
  CxEngage.entities.getGroups();
  ```
  Retrieves available Groups configured for current logged in tenant

  Topic: cxengage/entities/get-groups-response

  Possible Errors:

  - [Entities: 11025](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-groups-err)"
  {:validation ::get-entities-params
   :topic-key :get-groups-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as groups-response} (a/<! (rest/get-groups-request))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-groups-err groups-response)
                  :callback callback}))))

(def-sdk-fn get-skills
  "``` javascript
  CxEngage.entities.getSkills();
  ```
  Retrieves available Skills configured for current logged in tenant

  Topic: cxengage/entities/get-skills-response

  Possible Errors:

  - [Entities: 11026](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-skills-err)"
  {:validation ::get-entities-params
   :topic-key :get-skills-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as skills-response} (a/<! (rest/get-crud-entity-request ["skills"]))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-skills-err skills-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.downloadList({
;;   listId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::download-list-params
  (s/keys :req-un [::specs/list-id]
          :opt-un [::specs/callback]))

(def-sdk-fn download-list
  ""
  {:validation ::download-list-params
   :topic-key :download-list-response}
  [params]
  (let [{:keys [list-id callback topic]} params
        {:keys [api-response status] :as list-items-response} (a/<! (rest/download-list-request list-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-download-list-err list-items-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.uploadList({
;;   listId: {{uuid}},
;;   file: {{file}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::upload-list-params
  (s/keys :req-un [::specs/list-id ::specs/file]
          :opt-un [::specs/callback]))

(def-sdk-fn upload-list
  ""
  {:validation ::upload-list-params
   :topic-key :upload-list-response}
  [params]
  (let [{:keys [list-id file callback topic]} params
        tenant-id (state/get-active-tenant-id)
        url (iu/api-url
              (str "tenants/:tenant-id/lists/:list-id/upload")
              {:tenant-id tenant-id
               :list-id list-id})
        form-data (doto (js/FormData.) (.append "file" file (.-name file)))
        {:keys [api-response status] :as list-upload-response} (a/<! (rest/file-api-request
                                                                           {:method :post
                                                                            :url url
                                                                            :body form-data}))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-upload-list-err list-upload-response)
                  :callback callback}))))

;;--------------------------------------------------------------------------- ;;
;; CxEngage.entities.getEmailTypes({
;;   emailTypeId: {{uuid}} (optional)
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-email-types-params
  (s/keys :req-un []
          :opt-un [::specs/callback ::specs/email-type-id]))

(def-sdk-fn get-email-types
  ""
  {:validation ::get-email-types-params
   :topic-key :get-email-types-response}
  [params]
  (let [{:keys [callback topic email-type-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-email-types-request email-type-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-email-types-err entity-response)
                  :callback callback}))))

;;--------------------------------------------------------------------------- ;;
;; CxEngage.entities.getEmailTemplates({
;;   emailTypeId: {{uuid}} (optional)
;;   fallback: {{boolean}} (optional)
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-email-templates-params
  (s/keys :req-un []
          :opt-un [::specs/callback ::specs/email-type-id ::specs/fallback]))

(def-sdk-fn get-email-templates
  ""
  {:validation ::get-email-templates-params
   :topic-key :get-email-templates-response}
  [params]
  (let [{:keys [callback topic email-type-id fallback]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-email-templates-request email-type-id fallback))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-email-templates-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getArtifacts({
;;   interactionId: "{{uuid}}",
;;   tenantId: "{{uuid}}" (optional)
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-artifacts-params
  (s/keys :req-un [::specs/interaction-id]
    :opt-un [::specs/tenant-id ::specs/callback]))

(def-sdk-fn get-artifacts
  ""
  {:validation ::get-artifacts-params
   :topic-key :get-artifacts-response}
  [params]
  (let [{:keys [interaction-id tenant-id topic callback]} params
        {:keys [api-response status] :as entity-response} (a/<! (rest/get-interaction-artifacts-request interaction-id tenant-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-artifacts-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getArtifact({
;;   interactionId: "{{uuid}}",
;;   artifactId: "{{uuid}}",
;;   tenantId: "{{uuid}}" (optional)
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-artifact-params
  (s/keys :req-un [::specs/interaction-id ::specs/artifact-id]
    :opt-un [::specs/tenant-id ::specs/callback]))

(def-sdk-fn get-artifact
  ""
  {:validation ::get-artifact-params
   :topic-key :get-artifact-response}
  [params]
  (let [{:keys [interaction-id artifact-id tenant-id topic callback]} params
        {:keys [api-response status] :as entity-response} (a/<! (rest/get-artifact-by-id-request artifact-id interaction-id tenant-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-artifact-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getOutboundIdentifierList({
;;   outboundIdentifierListId: {{uuid}},
;;})
;; -------------------------------------------------------------------------- ;;

(s/def ::get-outbound-identifier-list-params
  (s/keys :req-un [::specs/outbound-identifier-list-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-outbound-identifier-list
  ""
  {:validation ::get-outbound-identifier-list-params
   :topic-key :get-outbound-identifier-list-response}
  [params]
  (let [{:keys [callback topic outbound-identifier-list-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entity-request :get "outbound-identifier-list" outbound-identifier-list-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-outbound-identifier-list-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getOutboundIdentifierLists();
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn get-outbound-identifier-lists
  ""
  {:validation ::get-entities-params
   :topic-key :get-outbound-identifier-lists-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "outbound-identifier-list"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-outbound-identifier-lists-err entity-response)
                  :callback callback}))))

(s/def ::get-user-outbound-identifier-lists-params
  (s/keys :req-un []
          :opt-un [::specs/user-id]))

(def-sdk-fn get-user-outbound-identifier-lists
  "``` javascript
  CxEngage.entities.getUserOutboundIdentifierLists(
    userId: {{uuid}}, (Optional, defaults to current user)
  );
  ```
  Retrieves the outbound identifier lists configured for currently logged in user

  Possible Errors:

  - [Entities: 11068](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-user-outbound-identifier-lists-err)"
  {:validation ::get-user-outbound-identifier-lists-params
   :topic-key :get-user-outbound-identifier-lists-response}
  [params]
  (let [{:keys [user-id callback topic]} params
         user-id (if user-id user-id (state/get-active-user-id))
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-crud-entity-request ["users" user-id "outbound-identifier-lists"]))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-user-outbound-identifier-lists-err entity-response)
                  :callback callback}))))

(def-sdk-fn get-custom-metrics
  "``` javascript
  CxEngage.entities.getCustomMetrics();
  ```
  Retrieves available Custom Metrics configured for current logged in tenant

  Possible Errors:

  - [Entities: 11040](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-custom-metrics-err)"
  {:validation ::get-entities-params
   :topic-key :get-custom-metrics-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-crud-entity-request ["custom-metrics"]))
       ; We set value for updated and updated-by if entity hasn't been updated yet
       ; since those values are not set by default like in other entities.
       ; JIRA Reference: https://liveops.atlassian.net/browse/CXV1-15814
        response {:result (map #(cond-> %
                                   (nil? (:updated %))    (assoc :updated (:created %))
                                   (nil? (:updated-by %)) (assoc :updated-by (:created-by %)))
                             (:result api-response))}
        error (if-not (= (:status entity-response) 200) (e/failed-to-get-custom-metrics-err entity-response))]
    (p/publish {:topics topic
                :response response
                :error error
                :callback callback})))

(s/def ::get-custom-metric-params
  (s/keys :req-un [::specs/custom-metric-id]
          :opt-un []))

(def-sdk-fn get-custom-metric
  "``` javascript
  CxEngage.entities.getCustomMetric({
    customMetricId: {{uuid}},
  });
  ```
  Retrieves single Custom Metrics given parameter customMetricId
  as a unique key

  Possible Errors:

  - [Entities: 11041](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-custom-metric-err)"
  {:validation ::get-custom-metric-params
   :topic-key :get-custom-metric-response}
  [params]
  (let [{:keys [callback topic custom-metric-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-crud-entity-request  ["custom-metrics" custom-metric-id]))
       ; We set value for updated and updated-by if entity hasn't been updated yet
       ; since those values are not set by default like in other entities.
       ; JIRA Reference: https://liveops.atlassian.net/browse/CXV1-15814
        response {:result (#(cond-> %
                               (nil? (:updated %))    (assoc :updated (:created %))
                               (nil? (:updated-by %)) (assoc :updated-by (:created-by %)))
                            (:result api-response))}
        error (if-not (= (:status entity-response) 200) (e/failed-to-get-custom-metric-err entity-response))]
    (p/publish {:topics topic
                :response response
                :error error
                :callback callback})))

(def-sdk-fn get-historical-report-folders
  "``` javascript
  CxEngage.entities.getHistoricalReportFolders();
  ```
  Retrieves available Report Types configured for current logged in tenant

  Topic: cxengage/entities/get-historical-report-folders-response

  Possible Errors:

  - [Entities: 11052](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-historical-report-folders-err)"
  {:validation ::get-entities-params
   :topic-key :get-historical-report-folders-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "historical-report-folder"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-historical-report-folders-err entity-response)
                  :callback callback}))))

(def-sdk-fn get-data-access-reports
  "``` javascript
  CxEngage.entities.getDataAccessReports();
  ```
  Retrieves available Data Access Reports configured for current logged in tenant

  Topic: cxengage/entities/get-data-access-reports-response

  Possible Errors:

  - [Entities: 11053](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-data-access-reports-err)"
  {:validation ::get-entities-params
   :topic-key :get-data-access-reports-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-crud-entity-request ["data-access-reports"]))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-data-access-reports-err entity-response)
                  :callback callback}))))

(s/def ::get-data-access-report-params
  (s/keys :req-un [::specs/data-access-report-id]
          :opt-un []))

(def-sdk-fn get-data-access-report
  "``` javascript
  CxEngage.entities.getDataAccessReport({
    dataAccessReportId: {{uuid}}
  });
  ```
  Retrieves single Data Access Report given parameter dataAccessReportId
  as a unique key

  Topic: cxengage/entities/get-data-access-report-response

  Possible Errors:

  - [Entities: 11054](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-data-access-report-err)"
  {:validation ::get-data-access-report-params
   :topic-key :get-data-access-report-response}
  [params]
  (let [{:keys [callback topic data-access-report-id]} params
        data-access-reports-response (a/<! (rest/get-crud-entity-request ["data-access-reports" data-access-report-id]))
        status-is-200 (= (:status data-access-reports-response) 200)
        response (:api-response (update-in data-access-reports-response [:api-response :result] rename-keys {:members :users}))]
    (p/publish {:topics topic
                :response response
                :error (if (false? status-is-200) (e/failed-to-get-data-access-report-err response))
                :callback callback})))

(s/def ::get-data-access-member-params
  (s/keys :req-un [::specs/data-access-member-id]
          :opt-un []))

(def-sdk-fn get-data-access-member
  "``` javascript
  CxEngage.entities.getDataAccessMember({
    dataAccessMemberId: {{uuid}}
  });
  ```
  Retrieves single Data Access Member given parameter dataAccessMemberId
  as a unique key

  Topic: cxengage/entities/get-data-access-member-response

  Possible Errors:

  - [Entities: 11067](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-data-access-member-err)"
  {:validation ::get-data-access-member-params
   :topic-key :get-data-access-member-response}
  [params]
  (let [{:keys [callback topic data-access-member-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-crud-entity-request ["data-access-member" data-access-member-id]))]
      (p/publish {:topics topic
                  :response api-response
                  :error (if-not (= status 200)
                           (e/failed-to-get-data-access-member-err entity-response))
                  :callback callback})))

(s/def ::get-skill-params
  (s/keys :req-un [::specs/skill-id]
          :opt-un []))

(def-sdk-fn get-skill
  "``` javascript
  CxEngage.entities.getSkill({
    skillId: {{uuid}}
  });
  ```
  Retrieves single Skill given parameter skillId
  as a unique key

  Topic: cxengage/entities/get-skill-response

  Possible Errors:

  - [Entities: 11057](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-skill-err)"
  {:validation ::get-skill-params
   :topic-key :get-skill-response}
  [params]
  (let [{:keys [callback topic skill-id]} params
        skills-response (a/<! (rest/get-crud-entity-request ["skills" skill-id]))
        skills-users-response (a/<! (rest/get-crud-entity-request ["skills" skill-id "users"]))
        status-is-200 (and (= (:status skills-response) 200)(= (:status skills-users-response) 200))
        users-ids (mapv #(:user-id %) (into [] (get-in skills-users-response [:api-response :result])))
        response (assoc-in (:api-response skills-response) [:result :users] users-ids)]
    (p/publish
      {:topics topic
       :response response
       :error (if (false? status-is-200) (e/failed-to-get-skill-err response))
       :callback callback})))

(s/def ::get-group-params
  (s/keys :req-un [::specs/group-id]
          :opt-un []))

(def-sdk-fn get-group
  "``` javascript
  CxEngage.entities.getGroup({
    groupId: {{uuid}}
  });
  ```
  Retrieves single Group given parameter groupId
  as a unique key

  Topic: cxengage/entities/get-group-response

  Possible Errors:

  - [Entities: 11060](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-group-err)"
  {:validation ::get-group-params
   :topic-key :get-group-response}
  [params]
  (let [{:keys [callback topic group-id]} params
        groups-response (a/<! (rest/get-crud-entity-request ["groups" group-id]))
        groups-users-response (a/<! (rest/get-crud-entity-request ["groups" group-id "users"]))
        status-is-200 (and (= (:status groups-response) 200)(= (:status groups-users-response) 200))
        users-ids (mapv #(:member-id %) (into [] (get-in groups-users-response [:api-response :result])))
        response (assoc-in (:api-response groups-response) [:result :users] users-ids)]
    (p/publish
      {:topics topic
       :response response
       :error (if (false? status-is-200) (e/failed-to-get-group-err response))
       :callback callback})))

(s/def ::get-role-params
  (s/keys :req-un [::specs/role-id]
          :opt-un []))

(def-sdk-fn get-role
  "``` javascript
  CxEngage.entities.getRole({
    roleId: {{uuid}}
  });
  ```
  Retrieves single Role given parameter roleId
  as a unique key

  Topic: cxengage/entities/get-role-response

  Possible Errors:

  - [Entities: 11079](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-role-err)"
  {:validation ::get-role-params
   :topic-key :get-role-response}
  [params]
  (let [{:keys [callback topic role-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-crud-entity-request ["roles" role-id]))]
      (p/publish {:topics topic
                  :response api-response
                  :error (if-not (= status 200)
                           (e/failed-to-get-role-err entity-response))
                  :callback callback})))

(s/def ::get-reason-params
  (s/keys :req-un [::specs/reason-id]
          :opt-un []))

(def-sdk-fn get-reason
  "``` javascript
  CxEngage.entities.getReason({
    reasonId: {{uuid}}
  });
  ```
  Retrieves single Reason given parameter reasonId
  as a unique key

  Topic: cxengage/entities/get-reason-response

  Possible Errors:

  - [Entities: 11082](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-reason-err)"
  {:validation ::get-reason-params
    :topic-key :get-reason-response}
  [params]
  (let [{:keys [callback topic reason-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-crud-entity-request ["reasons" reason-id]))]
      (p/publish {:topics topic
                  :response api-response
                  :error (if-not (= status 200)
                            (e/failed-to-get-reason-err entity-response))
                  :callback callback})))


(s/def ::get-reason-list-params
  (s/keys :req-un [::specs/reason-list-id]
          :opt-un []))

(def-sdk-fn get-reason-list
  "``` javascript
  CxEngage.entities.getReasonList({
    reasonListId: {{uuid}}
  });
  ```
  Retrieves single Reason List given parameter reasonListId
  as a unique key

  Topic: cxengage/entities/get-reason-list-response

  Possible Errors:

  - [Entities: 11083](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-reason-list-err)"
  {:validation ::get-reason-list-params
    :topic-key :get-reason-list-response}
  [params]
  (let [{:keys [callback topic reason-list-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-crud-entity-request ["reason-lists" reason-list-id]))]
      (p/publish {:topics topic
                  :response api-response
                  :error (if-not (= status 200)
                            (e/failed-to-get-reason-list-err entity-response))
                  :callback callback})))

(s/def ::get-flow-params
  (s/keys :req-un [::specs/flow-id]
          :opt-un []))

(def-sdk-fn get-flow
  "``` javascript
  CxEngage.entities.getFlow({
    flowId: {{uuid}}
  });
  ```
  Retrieves single Flow given parameter flowId
  as a unique key

  Topic: cxengage/entities/get-flow-response

  Possible Errors:

  - [Entities: 11084](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-flow-err)"
  {:validation ::get-flow-params
   :topic-key :get-flow-response}
  [params]
  (let [{:keys [callback topic flow-id]} params
        entity-response (a/<! (rest/get-crud-entity-request ["flows" flow-id]))
        error (if-not (= (:status entity-response) 200) (e/failed-to-get-flow-err entity-response))
        flow-versions-response (if (nil? error) (a/<! (rest/get-crud-entity-request ["flows" flow-id "versions"])))
        error (if-not (= (:status flow-versions-response) 200) (e/failed-to-get-flow-err flow-versions-response) error)
        flow-drafts-response (if (nil? error) (a/<! (rest/get-crud-entity-request ["flows" flow-id "drafts"])))
        error (if-not (= (:status flow-drafts-response) 200) (e/failed-to-get-flow-err flow-drafts-response) error)
        response (-> (:api-response entity-response)
                   (assoc-in [:result :versions] (get-in flow-versions-response [:api-response :result]))
                   (assoc-in [:result :drafts] (get-in flow-drafts-response [:api-response :result])))]
    (p/publish {:topics topic
                :response response
                :error error
                :callback callback})))

;;hygen-insert-before-get

(s/def ::get-dispatch-mapping-params
  (s/keys :req-un [ ::specs/dispatch-mapping-id]
          :opt-un [ ::specs/callback]))

(def-sdk-fn get-dispatch-mapping
  "``` javascript
  CxEngage.entities.getDispatchMapping({
    dispatchMappingId: {{uuid}} (required),
  });
  ```
  Calls rest/get-dispatch-mapping-request
  with the provided data for current tenant.

  Topic: cxengage/entities/get-dispatch-mapping-response

  Possible Errors:

  - [Entities: 11094](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-dispatch-mapping-err)"
  {:validation ::get-dispatch-mapping-params
   :topic-key :get-dispatch-mapping-response}
  [params]
  (let [{:keys [callback topic dispatch-mapping-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-crud-entity-request ["dispatch-mappings" dispatch-mapping-id]))
        error (if-not (= (:status entity-response) 200) (e/failed-to-get-dispatch-mappings-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))


(def-sdk-fn get-dispatch-mappings
  "``` javascript
  CxEngage.entities.getDispatchMappings();
  ```
  Calls rest/get-dispatch-mappings-request
  with the provided data for current tenant.

  Topic: cxengage/entities/get-dispatch-mappings-response

  Possible Errors:

  - [Entities: 11093](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-dispatch-mappings-err)"
  {:validation ::get-entities-params
   :topic-key :get-dispatch-mappings-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as dispatch-mappings-response} (a/<! (rest/get-crud-entity-request ["dispatch-mappings"]))
        error (if-not (= (:status dispatch-mappings-response) 200) (e/failed-to-get-dispatch-mappings-err dispatch-mappings-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))


(s/def ::get-disposition-params
  (s/keys :req-un [ ::specs/disposition-id]
          :opt-un []))

(def-sdk-fn get-disposition
  "``` javascript
  CxEngage.entities.getDisposition({
       disposition-id: {{uuid}} (required),
  });
  ```
  Calls rest/get-disposition-request
  with the provided data for current tenant.

  Topic: cxengage/entities/get-disposition-response

  Possible Errors:

  - [Entities: 11090](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-disposition-err)"
  {:validation ::get-disposition-params
   :topic-key :get-disposition-response}
  [params]
  (let [{:keys [callback topic disposition-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-crud-entity-request  ["dispositions" disposition-id]))
        error (if-not (= (:status entity-response) 200) (e/failed-to-get-disposition-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))


(def-sdk-fn get-dispositions
  "``` javascript
  CxEngage.entities.getDispositions();
  ```
  Retrieves available Dispositions configured for current logged in tenant

  Topic: cxengage/entities/get-dispositions-response

  Possible Errors:

  - [Entities: 11089](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-dispositions-err)"
  {:validation ::get-entities-params
   :topic-key :get-dispositions-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as dispositions-response} (a/<! (rest/get-crud-entity-request ["dispositions"]))
        error (if-not (= (:status dispositions-response) 200) (e/failed-to-get-dispositions-err dispositions-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))


(def-sdk-fn get-permissions
  "``` javascript
  CxEngage.entities.getPermissions();
  ```
  Retrieves Permissions for current logged in tenant

  Possible Errors:

  - [Entities: 11049](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-permissions-err)"
  {:validation ::get-entities-params
   :topic-key :get-permissions-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "permission"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-permissions-err entity-response)
                  :callback callback}))))


(def-sdk-fn get-reason-lists
  "``` javascript
  CxEngage.entities.getReasonLists();
  ```
  Retrieves available ReasonLists for current logged in tenant

  Possible Errors:

  - [Entities: 11048](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-reason-lists-err)"
  {:validation ::get-entities-params
   :topic-key :get-reason-lists-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "reason-list"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-reason-lists-err entity-response)
                  :callback callback}))))


(def-sdk-fn get-reasons
  "``` javascript
  CxEngage.entities.getReasons();
  ```
  Retrieves available Presence Reasons for current logged in tenant

  Possible Errors:

  - [Entities: 11047](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-reasons-err)"
  {:validation ::get-entities-params
   :topic-key :get-reasons-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "reason"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-reasons-err entity-response)
                  :callback callback}))))


(def-sdk-fn get-capacity-rules
  "``` javascript
  CxEngage.entities.getCapacityRules();
  ```
  Retrieves available CapacityRules for current logged in tenant

  Possible Errors:

  - [Entities: 11046](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-capacity-rules-err)"
  {:validation ::get-entities-params
   :topic-key :get-capacity-rules-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "capacity-rule"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-capacity-rules-err entity-response)
                  :callback callback}))))


(def-sdk-fn get-integrations
  "``` javascript
  CxEngage.entities.getIntegrations();
  ```
  Retrieves available Integrations for current logged in tenant

  Possible Errors:

  - [Entities: 11045](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-integrations-err)"
  {:validation ::get-entities-params
   :topic-key :get-integrations-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "integration"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-integrations-err entity-response)
                  :callback callback}))))

(def-sdk-fn get-roles
  "``` javascript
  CxEngage.entities.getRoles();
  ```
  Retrieves available roles for current logged in tenant

  Possible Errors:

  - [Entities: 11044](/cxengage-javascript-sdk.domain.errors.html#failed-to-get-roles-err)"
  {:validation ::get-entities-params
   :topic-key :get-roles-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "role"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-roles-err entity-response)
                  :callback callback}))))

(def-sdk-fn get-message-templates
  "``` javascript
  CxEngage.entities.getMessageTemplates();
  ```
  Retrieves available message templates for current logged in tenant

  Possible Errors:

  - [Entities: 11070](/cxengage-javascript-sdk.domain.errors.html#failed-to-get-message-templates-err)"
  {:validation ::get-entities-params
   :topic-key :get-message-templates-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "message-template"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-message-templates-err entity-response)
                  :callback callback}))))

(def-sdk-fn get-platform-roles
  "``` javascript
  CxEngage.entities.getPlatformRoles();
  ```
  Retrieves available platform roles predefined for current logged

  Possible Errors:

  - [Entities: 11063](/cxengage-javascript-sdk.domain.errors.html#failed-to-get-platform-roles-err)"
  {:validation ::get-entities-params
   :topic-key :get-platform-roles-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-platform-roles-request))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-platform-roles-err entity-response)
                  :callback callback}))))

(s/def ::get-entity-params
  (s/or
    :entity-name (s/keys :req-un [::specs/entity-name]
                         :opt-un [::specs/callback ::specs/entity-id ::specs/sub-entity-name])
    :path (s/keys :req-un [::specs/path]
                   :opt-un [::specs/callback])))

(def-sdk-fn get-entity
  "A generic method of retrieving an entity. The first way to call the function allows you to explicitly
  pass an entity name,  or its name and it's id along with an optional sub entity name.
  ``` javascript
  CxEngage.entities.getEntity({
    entityName: {{string}} (required)
    entityId: {{uuid}} (optional)
    subEntityName: {{string}} (optional)
  });
  ```
  Advanced users/consumers of the sdk/api can pass in an object with path as the key and an ordered array of the variables
  required to construct the api endpoint
  ``` javascript
  CxEngage.entities.getEntity({
    path: {{array}} (required) Example Array ['groups', '0000-0000-0000-0000', 'outboundIdentifierLists']
  });
  ```
  Retrieves an entity from the api matching the required parameters

  Topic: cxengage/entities/get-entity-response

  - [Api Documentation](https://api-docs.cxengage.net/Rest/Default.htm#Introduction/Intro.htm)

  Possible Errors:

  - [Entities: 11068](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-get-entity-err)"
  {:validation ::get-entity-params
   :topic-key :get-entity-response}
  [params]
  (let [{:keys [callback topic entity-name entity-id sub-entity-name path]} params]
     (let [{:keys [status api-response]} (a/<! (rest/get-crud-entity-request (if path
                                                                              (into [] path)
                                                                              [entity-name entity-id sub-entity-name])))
           status-is-200 (= status 200)]
          (p/publish
            {:topics topic
             :response api-response
             :error (if (false? status-is-200) (e/failed-to-get-entity-err api-response))
             :callback callback}))))

(s/def ::get-platform-user-params
  (s/keys :req-un [::specs/user-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-platform-user
  "``` javascript
  CxEngage.entities.getPlatformUser(
    userId: {{uuid}} (required)
  );
  ```
  Retrieves specified platform user details

  Possible Errors:

  - [Entities: 11072](/cxengage-javascript-sdk.domain.errors.html#failed-to-get-platform-user-err)"
  {:validation ::get-platform-user-params
   :topic-key :get-platform-user-response}
  [params]
  (let [{:keys [callback topic user-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-platform-user-request user-id))
        error (if-not (= status 200) (e/failed-to-get-platform-user-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

(def-sdk-fn get-identity-providers
  "``` javascript
  CxEngage.entities.getIdentityProviders();
  ```
  Retrieves tenants configured single sign on identity providers

  Possible Errors:

  - [Entities: 11073](/cxengage-javascript-sdk.domain.errors.html#failed-to-get-identity-providers-err)"
  {:validation ::get-entities-params
   :topic-key :get-identity-providers-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-crud-entity-request ["identity-providers"]))
        error (if-not (= status 200) (e/failed-to-get-identity-providers-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

;;--------------------------------------------------------------------------- ;;
;; POST Entity Functions
;; -------------------------------------------------------------------------- ;;

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.createList({
;;   listTypeId: {{uuid}},
;;   name: {{string}},
;;   shared: {{boolean}},
;;   active: {{boolean}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::create-list-params
  (s/keys :req-un [::specs/list-type-id ::specs/name ::specs/shared ::specs/active]
          :opt-un [::specs/callback]))

(def-sdk-fn create-list
  ""
  {:validation ::create-list-params
   :topic-key :create-list-response}
  [params]
  (let [{:keys [list-type-id name shared active callback topic]} params
        {:keys [api-response status] :as entity-response} (a/<! (rest/create-list-request list-type-id name shared [] active))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (assoc api-response :result (add-key-to-items (get api-response :result)))
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-list-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.createListItem({
;;   listId: {{uuid}},
;;   itemValue: {{object}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::create-list-item-params
  (s/keys :req-un [::specs/list-id ::specs/item-value]
          :opt-un [::specs/callback]))

(def-sdk-fn create-list-item
  ""
  {:validation ::create-list-item-params
   :topic-key :create-list-item-response}
  [params]
  (let [{:keys [list-id item-value callback topic]} params
        {:keys [api-response status] :as entity-response} (a/<! (rest/create-list-item-request list-id item-value))
        created-list-item api-response]
    (if (= status 200)
      (p/publish {:topics topic
                  :response created-list-item
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-list-item-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.createEmailTemplate({
;;   emailTypeId: {{uuid}},
;;   active: {{boolean}},
;;   shared: {{boolean}},
;;   subject: {{string}},
;;   body: {{string}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::create-email-template-params
  (s/keys :req-un [::specs/email-type-id ::specs/active ::specs/shared ::specs/body ::specs/subject]
          :opt-un [::specs/callback]))

(def-sdk-fn create-email-template
  ""
  {:validation ::create-email-template-params
   :topic-key :create-email-template-response}
  [params]
  (let [{:keys [email-type-id active shared body subject callback topic]} params
        {:keys [api-response status] :as entity-response} (a/<! (rest/create-email-template-request email-type-id active shared body subject))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-email-template-err entity-response)
                  :callback callback}))))

(s/def ::create-outbound-identifier-list-params
  (s/keys :req-un [::specs/active ::specs/name]
          :opt-un [::specs/description ::specs/callback]))

(def-sdk-fn create-outbound-identifier-list
  "``` javascript
  CxEngage.entities.createOutboundIdentifierList({
    active: {{boolean}},
    name: {{string}},
    description: {{string}} (optional)
  });
  ```
  Create new list of outbound identifiers for current tenant

  Possible Errors:

  - [Entities: 11034](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-outbound-identifier-list-err)"
  {:validation ::create-outbound-identifier-list-params
   :topic-key :create-outbound-identifier-list-response}
  [params]
  (let [{:keys [active name description callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-outbound-identifier-list-request active name description))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-outbound-identifier-list-err entity-response)
                  :callback callback}))))

(s/def ::create-data-access-report-params
  (s/keys :req-un [::specs/name ::specs/active ::specs/report-type]
          :opt-un [::specs/callback ::specs/description ::specs/realtime-report-id ::specs/realtime-report-type ::specs/realtime-report-name ::specs/historical-catalog-name ::specs/users]))

(def-sdk-fn create-data-access-report
  "``` javascript
  CxEngage.entities.createDataAccessReport({
    name: {{string}},
    description: {{string}}, (optional)
    active: {{boolean}},
    realtimeReportId: {{uuid}}, (optional)
    reportType: {{string}},
    realtimeReportType: {{string}}, (optional)
    realtimeReportName: {{string}}, (optional)
    historicalCatalogName: {{string}}, (optional)
    users: {{array}} (optional)
  });
  ```
  Creates new single Data Access Report by calling rest/create-data-access-report-request
  with the provided data for current tenant.

  Topic: cxengage/entities/create-data-access-report-response

  Possible Errors:

  - [Entities: 11055](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-data-access-report-err)"
  {:validation ::create-data-access-report-params
   :topic-key :create-data-access-report-response}
  [params]
  (let [{:keys [name description active realtime-report-id report-type realtime-report-type realtime-report-name historical-catalog-name users callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-data-access-report-request name description active realtime-report-id report-type realtime-report-type realtime-report-name historical-catalog-name users))
        response (update-in api-response [:result] rename-keys {:members :users})]
    (if (= status 200)
      (p/publish {:topics topic
                  :response response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-data-access-report-err entity-response)
                  :callback callback}))))

(s/def ::create-skill-params
  (s/keys :req-un [::specs/name ::specs/active]
          :opt-un [::specs/callback ::specs/description ::specs/has-proficiency]))

(def-sdk-fn create-skill
  "``` javascript
  CxEngage.entities.createSkill({
    name: {{string}},
    description: {{string}}, (optional)
    active: {{boolean}},
    hasProficiency: {{boolean}} (optional)
  });
  ```
  Creates new single Skill by calling rest/create-skill-request
  with the provided data for current tenant.

  Topic: cxengage/entities/create-skill-response

  Possible Errors:

  - [Entities: 11058](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-skill-err)"
  {:validation ::create-skill-params
   :topic-key :create-skill-response}
  [params]
  (let [{:keys [name description active has-proficiency callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-skill-request name description active has-proficiency))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-skill-err entity-response)
                  :callback callback}))))

(s/def ::create-group-params
  (s/keys :req-un [::specs/name ::specs/active]
          :opt-un [::specs/callback ::specs/description]))

(def-sdk-fn create-group
  "``` javascript
  CxEngage.entities.createGroup({
    name: {{string}},
    description: {{string}}, (optional)
    active: {{boolean}}
  });
  ```
  Creates new single Group by calling rest/create-group-request
  with the provided data for current tenant.

  Topic: cxengage/entities/create-group-response

  Possible Errors:

  - [Entities: 11061](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-group-err)"
  {:validation ::create-group-params
   :topic-key :create-group-response}
  [params]
  (let [{:keys [name description active callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-group-request name description active))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-group-err entity-response)
                  :callback callback}))))

;;hygen-insert-before-create

(s/def ::create-dispatch-mapping-params
  (s/keys :req-un [ ::specs/name ::specs/value ::specs/flow-id ::specs/channel-type ::specs/interaction-field ::specs/active]
          :opt-un [ ::specs/callback ::specs/description ::specs/version]))

(def-sdk-fn create-dispatch-mapping
  "``` javascript
  CxEngage.entities.createDispatchMapping({
    name: {{string}} (required),
    description: {{string}} (optional),
    value: {{string}} (required),
    flowId: {{uuid}} (required),
    version: {{uuid}} (optional),
    channelType: {{string}} (required),
    interactionField: {{string}} (required),
    active: {{boolean}} (required),
  });
  ```
  Calls rest/create-dispatch-mapping-request
  with the provided data for current tenant.

  Topic: cxengage/entities/create-dispatch-mapping-response

  Possible Errors:

  - [Entities: 11095](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-dispatch-mapping-err)"
  {:validation ::create-dispatch-mapping-params
   :topic-key :create-dispatch-mapping-response}
  [params]
  (let [{:keys [callback topic name description value flow-id version channel-type interaction-field active]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-dispatch-mapping-request name description value flow-id version channel-type interaction-field active))
        error (if-not (= (:status entity-response) 200) (e/failed-to-create-dispatch-mapping-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))


(s/def ::create-disposition-params
  (s/keys :req-un [ ::specs/name ::specs/active ::specs/shared]
          :opt-un [ ::specs/callback ::specs/description ::specs/external-id]))

(def-sdk-fn create-disposition
  "``` javascript
  CxEngage.entities.createDisposition({
     name: {{string}} (required),
     description: {{string}} (optional),
     externalId: {{string}} (optional),
     active: {{boolean}} (required),
     shared: {{boolean}} (required),
  });
  ```
  Calls rest/create-disposition-request
  with the provided data for current tenant.

  Topic: cxengage/entities/create-disposition-response

  Possible Errors:

  - [Entities: 11091](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-disposition-err)"
  {:validation ::create-disposition-params
   :topic-key :create-disposition-response}
  [params]
  (let [{:keys [name description external-id active shared callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-disposition-request name description external-id active shared))
        error (if-not (= (:status entity-response) 200) (e/failed-to-create-disposition-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))


(s/def ::create-reason-list-params
  (s/keys :req-un [ ::specs/name ::specs/active ::specs/shared]
          :opt-un [ ::specs/callback ::specs/description ::specs/external-id ::specs/reasons ::specs/is-default]))

(def-sdk-fn create-reason-list
  "``` javascript
  CxEngage.entities.createReasonList({
    name: {{string}} (required),
    description: {{string}} (optional),
    externalId: {{string}} (optional),
    active: {{boolean}} (required),
    shared: {{boolean}} (required),
    reasons: {{object}} (optional),
    isDefault: {{boolean}} (optional),
  });
  ```
  Calls rest/create-reason-list-request
  with the provided data for current tenant.

  Topic: cxengage/entities/create-reason-list-response

  Possible Errors:

  - [Entities: 11080](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-reason-list-err)"
  {:validation ::create-reason-list-params
   :topic-key :create-reason-list-response}
  [params]
  (let [{:keys [name description external-id active shared reasons is-default callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-reason-list-request name description external-id active shared reasons is-default))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-reason-list-err entity-response)
                  :callback callback}))))


(s/def ::create-reason-params
  (s/keys :req-un [ ::specs/name ::specs/active ::specs/shared]
          :opt-un [ ::specs/callback ::specs/description ::specs/external-id]))

(def-sdk-fn create-reason
  "``` javascript
  CxEngage.entities.createReason({
       name: {{string}} (required),
       description: {{string}} (optional),
       externalId: {{string}} (optional),
       active: {{boolean}} (required),
       shared: {{boolean}} (required),
  });
  ```
  Calls rest/create-reason-request
  with the provided data for current tenant.

  Topic: cxengage/entities/create-reason-response

  Possible Errors:

  - [Entities: 11078](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-reason-err)"
  {:validation ::create-reason-params
   :topic-key :create-reason-response}
  [params]
  (let [{:keys [name description external-id active shared callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-reason-request name description external-id active shared))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-reason-err entity-response)
                  :callback callback}))))


(s/def ::create-user-params
  (s/keys :req-un [::specs/email ::specs/role-id ::specs/platform-role-id]
          :opt-un [::specs/first-name ::specs/last-name ::specs/callback ::specs/status ::specs/default-identity-provider ::specs/no-password ::specs/work-station-id ::specs/external-id]))

(def-sdk-fn create-user
  "``` javascript
  CxEngage.entities.createUser({
    email: {{string}} (required),
    roleId: {{uuid}} (required),
    platformRoleId: {{uuid}} (required),
    defaultIdentityProvider: {{uuid}} (optional),
    noPassword: {{boolean}} (optional),
    status: {{string}} (required),
    workStationId: {{string}} (optional),
    externalId: {{string}} (optional),
    extensions: {{object}} (optional),
    firstName: {{string}} (optional),
    lastName: {{string}} (optional),
    capacityRuleId: {{uuid}} (optional)
  });
  ```
  Creates new single User by calling rest/create-user-request
  with the provided data for current tenant.

  Topic: cxengage/entities/create-user-response

  Possible Errors:

  - [Entities: 11065](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-user-err)"
  {:validation ::create-user-params
   :topic-key :create-user-response}
  [params]
  (let [{:keys [email, role-id, default-identity-provider, no-password, status, work-station-id, external-id, extensions, first-name, last-name, capacity-rule-id callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-user-request email, role-id, default-identity-provider, no-password, status, work-station-id, external-id, extensions, first-name, last-name, capacity-rule-id))
        response (update-in api-response [:result] rename-keys {:user-id :id})
        error (if-not (= status 200) (e/failed-to-create-user-err entity-response))]
    (p/publish {:topics topic
                :response response
                :error error
                :callback callback})))


(s/def ::create-role-params
  (s/keys :req-un [::specs/name]
          :opt-un [::specs/description ::specs/permissions ::specs/callback]))

(def-sdk-fn create-role
  "``` javascript
  CxEngage.entities.createRole({
    name: {{string}},
    description: {{string}},
    permissions: {{array}}
  });
  ```
  Create Role for current logged in tenant

  Possible Errors:

  - [Entities: 11050](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-role-err)"
  {:validation ::create-role-params
   :topic-key :create-role-response}
  [params]
  (let [{:keys [active name description permissions callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-role-request name description permissions))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-role-err entity-response)
                  :callback callback}))))

(s/def ::create-flow-params
  (s/keys :req-un [::specs/flow-type ::specs/name]
          :opt-un [::specs/callback ::specs/flow ::specs/active ::specs/metadata ::specs/description]))

(def-sdk-fn create-flow
  "``` javascript
  CxEngage.entities.createFlow({
    flowType: {{string}}, (required) (for new/copy of flow)
    name: {{string}}, (required) (for new/copy of flow)
    active: {{boolean}}, (optional) (for new/copy of flow)
    description: {{string}} (optional) (for new/copy of flow)
    flow: {{string}}, (optional) (for new initial draft)
    metadata: {{string}}, (optional) (for new initial draft)
  });
  ```
  Creates new single/copy Flow and its Initial Draft by calling rest/create-flow-request
  with the provided data for current tenant.

  Topic: cxengage/entities/create-flow-response

  Possible Errors:

  - [Entities: 11085](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-flow-err)"
  {:validation ::create-flow-params
   :topic-key :create-flow-response}
  [params]
  (let [{:keys [flow-type name flow metadata active description callback topic]} params
        flow-response (a/<! (rest/create-flow-request flow-type name active description))
        error (if-not (= (:status flow-response) 200) (e/failed-to-create-flow-err flow-response))
        flow-id (get-in flow-response [:api-response :result :id])
        draft-response (if (nil? error) (a/<! (rest/create-flow-draft-request flow-id "Initial Draft" flow metadata nil)))
        error (if-not (= (:status draft-response) 200) (e/failed-to-create-flow-err draft-response) error)
        response (assoc-in (:api-response flow-response) [:result :drafts] (conj [] (get-in draft-response [:api-response :result])))]
      (p/publish {:topics topic
                  :response response
                  :error error
                  :callback callback})))

(s/def ::create-flow-draft-params
  (s/keys :req-un [::specs/flow-id ::specs/name]
          :opt-un [::specs/callback ::specs/flow ::specs/metadata ::specs/description]))

(def-sdk-fn create-flow-draft
  "``` javascript
  CxEngage.entities.createFlowDraft({
    flowId: {{uuid}}, (required)
    name: {{string}}, (required)
    flow: {{string}}, (required)
    metadata: {{string}}, (optional)
    description: {{string}} (optional)
  });
  ```
  Creates new single Flow Draft by calling rest/create-flow-draft-request
  with the provided data for current tenant.

  Topic: cxengage/entities/create-flow-draft-response

  Possible Errors:

  - [Entities: 11086](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-flow-draft-err)"
  {:validation ::create-flow-draft-params
   :topic-key :create-flow-draft-response}
  [params]
  (let [{:keys [flow-id name flow metadata description callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-flow-draft-request flow-id name flow metadata description))
        error (if-not (= status 200) (e/failed-to-create-flow-draft-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

(s/def ::create-custom-metric-params
  (s/keys :req-un [::specs/sla-threshold ::specs/sla-abandon-type ::specs/active ::specs/name]
          :opt-un [::specs/callback ::specs/sla-abandon-threshold ::specs/custom-metrics-type ::specs/description]))

(def-sdk-fn create-custom-metric
  "``` javascript
  CxEngage.entities.createCustomMetric({
    slaThreshold: {{integer}}, (required)
    slaAbandonType: {{string}}, (required)
    active: {{boolean}}, (required)
    name: {{string}}, (required)
    slaAbandonThreshold: {{integer}}, (optional)
    customMetricsType: {{string}}, (optional)
    description: {{string}}, (optional)
  });
  ```
  Creates a single Custom Metric by calling rest/create-custom-metric-request
  with the provided data for current tenant.

  Possible Errors:

  - [Entities: 11097](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-custom-metric-err)"
  {:validation ::create-custom-metric-params
   :topic-key :create-custom-metric-response}
  [params]
  (let [{:keys [name description custom-metrics-type active sla-abandon-type sla-threshold sla-abandon-threshold callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-custom-metric-request name description custom-metrics-type active sla-abandon-type sla-threshold sla-abandon-threshold))
        error (if-not (= (:status entity-response) 200) (e/failed-to-create-custom-metric-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

;;--------------------------------------------------------------------------- ;;
;; PUT Entity Functions
;; -------------------------------------------------------------------------- ;;

(s/def ::update-platform-user-params
  (s/keys :req-un [::specs/user-id ::specs/update-body]
          :opt-un [::specs/callback]))

(def-sdk-fn update-platform-user
  "``` javascript
  CxEngage.entities.updatePlatformUser({
    userId: {{uuid}} (required),
    updateBody: {{object}} (required)
  });
  ```

  Example updateBody
  updateBody: {
    'externalId': {{string}} (optional)
    'firstName': {{string}} (optional)
    'lastName': {{string}} (optional)
    'personalTelephone': {{string}} (optional)
	}

  Updates a platform user accounts details from the provided updateBody

  Topic: cxengage/entities/update-user-response

  Possible Errors:

  - [Entities: 11071](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-platform-user-err)"
  {:validation ::update-platform-user-params
   :topic-key :update-platform-user-response}
  [params]
  (let [{:keys [callback topic update-body user-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/platform-crud-entity-request :put "user" user-id update-body))
        status-is-200 (= status 200)]
    (p/publish {:topics topic
                :response api-response
                :error (if-not status-is-200 (e/failed-to-update-platform-user-err entity-response))
                :callback callback})))

(s/def ::update-user-params
  (s/keys :req-un [::specs/user-id]
          :opt-un [::specs/callback ::specs/role-id ::specs/platform-role-id ::specs/status ::specs/default-identity-provider ::specs/no-password ::specs/work-station-id ::specs/external-id ::specs/extensions ::specs/first-name ::specs/last-name ::specs/capacity-rule-id]))

(def-sdk-fn update-user
  "``` javascript
  CxEngage.entities.createUser({
    userId: {{uuid}} (required),
    email: {{string}} (required),
    roleId: {{uuid}} (required),
    platformRoleId: {{uuid}} (required),
    defaultIdentityProvider: {{uuid}} (optional),
    noPassword: {{boolean}} (optional),
    status: {{string}} (required),
    workStationId: {{string}} (optional),
    externalId: {{string}} (optional),
    extensions: {{object}} (optional),
    firstName: {{string}} (optional),
    lastName: {{string}} (optional),
    capacityRuleId: {{uuid}} (optional),
    updateBody: {{object}} (optional)
  });
  ```
  Updates new single User by calling rest/update-user-response
  with the provided data for current tenant.

  Topic: cxengage/entities/update-user-response

  Possible Errors:

  - [Entities: 11066](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-user-err)"
  {:validation ::update-user-params
   :topic-key :update-user-response}
  [params]
  (let [{:keys [callback topic update-body user-id]} params
        user-body (or update-body (dissoc params :callback :topic :update-body))
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entity-request :put "user" user-id user-body))
        response (update-in api-response [:result] rename-keys {:user-id :id})
        error (if-not (= status 200) (e/failed-to-update-user-err api-response))]
    (p/publish {:topics topic
                :response response
                :error error
                :callback callback})))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.updateList({
;;   listId: {{uuid}},
;;   name: {{string}} (optional),
;;   shared: {{boolean}} (optional),
;;   active: {{boolean}} (optional)
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::update-list-params
  (s/keys :req-un [::specs/list-id]
          :opt-un [::specs/callback ::specs/name ::specs/shared ::specs/active]))

(def-sdk-fn update-list
  ""
  {:validation ::update-list-params
   :topic-key :update-list-response}
  [params]
  (let [{:keys [callback list-id topic name shared active]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-list-request list-id name shared active))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (assoc api-response :result (add-key-to-items (get api-response :result)))
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-list-err entity-response)
                  :callback callback}))))

(s/def ::update-outbound-identifier-params
  (s/keys :req-un [::specs/outbound-identifier-id]
          :opt-un [::specs/callback ::specs/name ::specs/active ::specs/value ::specs/flow-id ::specs/channel-type ::specs/description]))

(def-sdk-fn update-outbound-identifier
  "``` javascript
  CxEngage.entities.updateOutboundIdentifier({
    id: {{uuid}}
    name: {{string}} (optional)
    active: {{boolean}} (optional)
    value: {{string}} (optional)
    flowId: {{string}} (optional)
    channelType: {{string}} (optional)
    description: {{string}} (optional)
  });
  ```
  Updates single Outbound Identifier by calling rest/update-outbound-identifier-request
  with the provided data for current tenant.

  Topic: cxengage/entities/update-outbound-identifier-response

  Possible Errors:

  - [Entities: 11032](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-outbound-identifier-err)"
  {:validation ::update-outbound-identifier-params
   :topic-key :update-outbound-identifier-response}
  [params]
  (let [{:keys [callback outbound-identifier-id topic name active value flow-id channel-type description]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-outbound-identifier-request outbound-identifier-id name active value flow-id channel-type description))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (assoc api-response :result (add-key-to-items (get api-response :result)))
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-outbound-identifier-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.createOutboundIdentifier({
;;   name: {{string}}
;;   active: {{boolean}}
;;   value: {{string}}
;;   flowId: {{string}}
;;   channelType: {{string}}
;;   description: {{string}} (optional)
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::create-outbound-identifier-params
  (s/keys :req-un [::specs/name ::specs/active ::specs/value ::specs/flow-id ::specs/channel-type]
          :opt-un [::specs/description]))

(def-sdk-fn create-outbound-identifier
  ""
  {:validation ::create-outbound-identifier-params
   :topic-key :create-outbound-identifier-response}
  [params]
  (let [{:keys [callback topic name active value flow-id channel-type description]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-outbound-identifier-request name active value flow-id channel-type description))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (assoc api-response :result (add-key-to-items (get api-response :result)))
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-outbound-identifier-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.updateListItem({
;;   listId: {{uuid}},
;;   listItemKey: {{string}}
;;   itemValue: {{object}}
;; });
;;
;; NOTE: Within the individual item object assigned to the itemValue property,
;; if you make a change to the item property that is serving as the key for
;; the item (ie: the first field in that list), then you will get an error.
;; Due to messiness of executing 2 API calls in a single SDK method,
;; the temporary workaround () for this is to:
;; 1) Delete the item using CxEngage.entities.deleteItem()
;; 2) In the callback for the deleteItem() method, create a new item using
;; the item object you want to create
;; -------------------------------------------------------------------------- ;;

(s/def ::update-list-item-params
  (s/keys :req-un [::specs/list-id ::specs/list-item-key ::specs/item-value]
          :opt-un [::specs/callback]))

(def-sdk-fn update-list-item
  ""
  {:validation ::update-list-item-params
   :topic-key :update-list-item-response}
  [params]
  (let [{:keys [callback list-id topic list-item-key item-value]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-list-item-request list-id (js/encodeURIComponent list-item-key) item-value))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-list-item-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.updateEmailTemplate({
;;   emailTypeId: {{uuid}},
;;   active: {{boolean}}, (optional)
;;   shared: {{boolean}}, (optional)
;;   subject: {{string}}, (optional)
;;   body: {{string}} (optional)
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::update-email-template-params
  (s/keys :req-un [::specs/email-type-id]
          :opt-un [::specs/callback ::specs/active ::specs/shared ::specs/body ::specs/subject]))

(def-sdk-fn update-email-template
  ""
  {:validation ::update-email-template-params
   :topic-key :update-email-template-response}
  [params]
  (let [{:keys [email-type-id active shared body subject callback topic]} params
        {:keys [api-response status] :as entity-response} (a/<! (rest/update-email-template-request email-type-id active shared body subject))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-email-template-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.updateOutboundIdentifierList({
;;   id: {{uuid}},
;;   active: {{boolean}}, (optional)
;;   name: {{string}}, (optional)
;;   description: {{string}}, (optional)
;;})
;; -------------------------------------------------------------------------- ;;

(s/def ::update-outbound-identifier-list-params
  (s/keys :req-un [::specs/outbound-identifier-list-id]
          :opt-un [::specs/callback ::specs/active ::specs/name ::specs/description]))

(def-sdk-fn update-outbound-identifier-list
  ""
  {:validation ::update-outbound-identifier-list-params
   :topic-key :update-outbound-identifier-list-response}
  [params]
  (let [{:keys [outbound-identifier-list-id active name description callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-outbound-identifier-list-request outbound-identifier-list-id active name description))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-outbound-identifier-list-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.deleteOutboundIdentifier({
;;   outboundIdentifierId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::delete-outbound-identifier-params
  (s/keys :req-un [::specs/outbound-identifier-id]
          :opt-un [::specs/callback]))

(def-sdk-fn delete-outbound-identifier
  ""
  {:validation ::delete-outbound-identifier-params
   :topic-key :delete-outbound-identifier-response}
  [params]
  (let [{:keys [callback outbound-identifier-id topic]} params
        {:keys [api-response status] :as entity-response} (a/<! (rest/delete-outbound-identifier-request outbound-identifier-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-delete-outbound-identifier-err entity-response)
                  :callback callback}))))

(s/def ::dissociate-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn dissociate
  "``` javascript
  CxEngage.entities.dissociate({
    originEntity: {
      name: {{string}}, (required)
      id: {{string}}, (required)
    },
    destinationEntity: {
      name: {{string}}, (required)
      id: {{string}}, (required)
    }
  });
  ```
  Dissociates item from the entity requested. For example  , remove a user from a group or skill.

  Topic: cxengage/entities/dissociate-response

  Possible Errors:

  - [Entities: 11064](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-dissociate-err)"
  {:validation ::dissociate-params
   :topic-key :dissociate-response}
  [params]
  (let [{:keys [callback topic origin-entity destination-entity]} params
        {:keys [api-response status] :as response} (a/<! (rest/dissociate-request origin-entity destination-entity))]
   (p/publish {:topics topic
               :response api-response
               :error (if (false? (= status 200)) (e/failed-to-dissociate-err response))
               :callback callback})))

(s/def ::associate-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn associate
  "``` javascript
  CxEngage.entities.associate({
    originEntity: {
      name: {{string}}, (required)
      id: {{string}}, (required)
    },
    destinationEntity: {
      name: {{string}}, (required)
      id: {{string}}, (required)
    }
  });
  ```
  Associates item from the entity requested. For example  , add a user to a group or skill.

  Topic: cxengage/entities/associate-response

  Possible Errors:

  - [Entities: 11066](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-associate-err)"
  {:validation ::associate-params
   :topic-key :associate-response}
  [params]
  (let [{:keys [callback topic origin-entity destination-entity]} params
        {:keys [api-response status] :as response} (a/<! (rest/associate-request origin-entity destination-entity))
        {:keys [member-id member-type]} (:result api-response)
        response-body (if (and (string? member-type) (string? member-id) (= member-type "user"))
                          (assoc-in api-response [:result :user-id] member-id)
                          api-response)]
   (p/publish {:topics topic
               :response response-body
               :error (if (false? (= status 200)) (e/failed-to-associate-err response))
               :callback callback})))

(s/def ::update-users-capacity-rule-params
  (s/keys :req-un [::specs/user-id ::specs/capacity-rule-id]
          :opt-un [::specs/callback]))

(def-sdk-fn update-users-capacity-rule
  "``` javascript
  CxEngage.entities.updateUsersCapacityRule({
    userId: {{string}}, (required)
    capacityRuleId: {{string or null}}, (required)
  });
  ```
  Updates the users effective capacity rule.

  Topic: cxengage/entities/update-users-capacity-rule-response

  Possible Errors:

  - [Entities: 11074](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-users-capacity-rule-err)"
  {:validation ::update-users-capacity-rule-params
   :topic-key :update-users-capacity-rule-response}
  [params]
  (let [{:keys [callback topic user-id capacity-rule-id]} params
        effective-capacity-rule (get-in (a/<! (rest/crud-entity-request :get "user" user-id)) [:api-response :result :effective-capacity-rule :id])
        deleted (and effective-capacity-rule (a/<! (rest/delete-users-capacity-request user-id effective-capacity-rule)))
        deletedResponse (:api-response deleted)
        deletedStatus (:status deleted)
        updated (and (not= capacity-rule-id nil) (a/<! (rest/update-users-capacity-request user-id capacity-rule-id)))
        updatedResponse (:api-response updated)
        updatedStatus (:status updated)
        response (if (not= capacity-rule-id nil) updatedResponse deletedResponse)
        error (if (not= capacity-rule-id nil) (if (not= updatedStatus 200) (e/failed-to-update-users-capacity-rule-err response)) (if (not= deletedStatus 200) (e/failed-to-update-users-capacity-rule-err response)))]
   (p/publish {:topics topic
               :response response
               :error error
               :callback callback})))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.addOutboundIdentifierListMember({
;;   outboundIdentifierListId: {{uuid}}
;;   outboundIdentifierId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::add-outbound-identifier-list-member-params
  (s/keys :req-un [::specs/outbound-identifier-list-id ::specs/outbound-identifier-id]
          :opt-un [::specs/callback]))

(def-sdk-fn add-outbound-identifier-list-member
  ""
  {:validation ::add-outbound-identifier-list-member-params
   :topic-key :add-outbound-identifier-list-member-response}
  [params]
  (let [{:keys [callback topic outbound-identifier-list-id outbound-identifier-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/add-outbound-identifier-list-member-request outbound-identifier-list-id outbound-identifier-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (assoc api-response :result (add-key-to-items (get api-response :result)))
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-add-outbound-identifier-list-member-err entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.removeOutboundIdentifierListMember({
;;   outboundIdentifierListId: {{uuid}}
;;   outboundIdentifierId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::remove-outbound-identifier-list-member-params
  (s/keys :req-un [::specs/outbound-identifier-list-id ::specs/outbound-identifier-id]
          :opt-un [::specs/callback]))

(def-sdk-fn remove-outbound-identifier-list-member
  ""
  {:validation ::remove-outbound-identifier-list-member-params
   :topic-key :remove-outbound-identifier-list-member-response}
  [params]
  (let [{:keys [callback topic outbound-identifier-list-id outbound-identifier-id]} params
        {:keys [api-response status] :as entity-response} (a/<! (rest/remove-outbound-identifier-list-member-request outbound-identifier-list-id outbound-identifier-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-remove-outbound-identifier-list-member-err entity-response)
                  :callback callback}))))

(s/def ::update-custom-metric-params
    (s/keys :req-un [::specs/custom-metric-id]
            :opt-un [::specs/callback ::specs/sla-threshold ::specs/sla-abandon-type ::specs/active ::specs/name ::specs/sla-abandon-threshold ::specs/custom-metrics-type ::specs/description]))

(def-sdk-fn update-custom-metric
  "``` javascript
  CxEngage.entities.updateCustomMetric({
    customMetricId: {{uuid}},
    slaThreshold: {{integer}}, (optional)
    slaAbandonType: {{string}}, (optional)
    active: {{boolean}}, (optional)
    name: {{string}}, (optional)
    slaAbandonThreshold: {{integer}}, (optional)
    customMetricsType: {{string}}, (optional)
    description: {{string}}, (optional)
  });
  ```
  Updates a single Custom Metric by calling rest/update-custom-metric-request
  with the new data and customMetricId as the unique key.

  Possible Errors:

  - [Entities: 11042](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-custom-metric-err)"
  {:validation ::update-custom-metric-params
   :topic-key :update-custom-metric-response}
  [params]
  (let [{:keys [custom-metric-id sla-abandon-type active name custom-metrics-type sla-threshold sla-abandon-threshold description callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-custom-metric-request description custom-metrics-type custom-metric-id active sla-abandon-type sla-threshold name sla-abandon-threshold))
        error (if-not (= (:status entity-response) 200) (e/failed-to-update-custom-metric-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

(s/def ::update-data-access-report-params
    (s/keys :req-un [::specs/data-access-report-id]
            :opt-un [::specs/callback ::specs/name ::specs/realtime-report-id ::specs/description ::specs/active ::specs/report-type ::specs/realtime-report-type ::specs/realtime-report-name ::specs/historical-catalog-name ::specs/users]))

(def-sdk-fn update-data-access-report
  "``` javascript
  CxEngage.entities.updateDataAccessReport({
    dataAccessReportId: {{uuid}},
    name: {{string}}, (optional)
    description: {{string}}, (optional)
    active: {{boolean}}, (optional)
    realtimeReportId: {{uuid}}, (optional)
    reportType: {{string}}, (optional)
    realtimeReportType: {{string}}, (optional)
    realtimeReportName: {{string}}, (optional)
    historicalCatalogName: {{string}}, (optional)
    users: {{array}} (optional)
  });
  ```
  Updates a single Data Access Report by calling rest/update-data-access-report-request
  with the new data and dataAccessReportId as the unique key.

  Topic: cxengage/entities/update-data-access-report-response

  Possible Errors:

  - [Entities: 11056](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-data-access-report-err)"
  {:validation ::update-data-access-report-params
   :topic-key :update-data-access-report-response}
  [params]
  (let [{:keys [data-access-report-id name realtime-report-id description active report-type realtime-report-type realtime-report-name historical-catalog-name users callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-data-access-report-request data-access-report-id name description active realtime-report-id report-type realtime-report-type realtime-report-name historical-catalog-name users))
        response (update-in api-response [:result] rename-keys {:members :users})]
    (if (= status 200)
      (p/publish {:topics topic
                  :response response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-data-access-report-err entity-response)
                  :callback callback}))))

(s/def ::update-skill-params
  (s/keys :req-un [::specs/skill-id]
          :opt-un [::specs/callback ::specs/name ::specs/active ::specs/description ::specs/has-proficiency]))

(def-sdk-fn update-skill
  "``` javascript
  CxEngage.entities.updateSkill({
    skillId: {{uuid}},
    name: {{string}}, (optional)
    description: {{string}}, (optional)
    active: {{boolean}}, (optional)
    hasProficiency: {{boolean}} (optional)
  });
  ```
  Updates a single Skill by calling rest/update-skill-request
  with the new data and skillId as the unique key.

  Topic: cxengage/entities/update-skill-response

  Possible Errors:

  - [Entities: 11059](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-skill-err)"
  {:validation ::update-skill-params
   :topic-key :update-skill-response}
  [params]
  (let [{:keys [skill-id name description active has-proficiency callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-skill-request skill-id name description active has-proficiency))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-skill-err entity-response)
                  :callback callback}))))

(s/def ::update-group-params
  (s/keys :req-un [::specs/group-id]
          :opt-un [::specs/callback ::specs/name ::specs/active ::specs/description]))

(def-sdk-fn update-group
  "``` javascript
  CxEngage.entities.updateGroup({
    groupId: {{uuid}},
    name: {{string}}, (optional)
    description: {{string}}, (optional)
    active: {{boolean}} (optional)
  });
  ```
  Updates a single Group by calling rest/update-group-request
  with the new data and groupId as the unique key.

  Topic: cxengage/entities/update-group-response

  Possible Errors:

  - [Entities: 11062](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-group-err)"
  {:validation ::update-group-params
   :topic-key :update-group-response}
  [params]
  (let [{:keys [group-id name description active callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-group-request group-id name description active))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-group-err entity-response)
                  :callback callback}))))

;;hygen-insert-before-update

(s/def ::update-dispatch-mapping-params
  (s/keys :req-un [ ::specs/dispatch-mapping-id]
          :opt-un [ ::specs/callback ::specs/name ::specs/description ::specs/value ::specs/flow-id ::specs/version ::specs/interaction-field ::specs/channel-type ::specs/active]))

(def-sdk-fn update-dispatch-mapping
  "``` javascript
  CxEngage.entities.updateDispatchMapping({
    dispatchMappingId: {{uuid}} (required),
    name: {{string}} (optional),
    description: {{string}} (optional),
    value: {{string}} (optional),
    flowId: {{uuid}} (optional),
    version: {{uuid}} (optional),
    interactionField: {{string}} (optional),
    channelType: {{string}} (optional),
    active: {{boolean}} (optional),
  });
  ```
  Calls rest/update-dispatch-mapping-request
  with the provided data for current tenant.

  Topic: cxengage/entities/update-dispatch-mapping-response

  Possible Errors:

  - [Entities: 11096](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-dispatch-mapping-err)"
  {:validation ::update-dispatch-mapping-params
    :topic-key :update-dispatch-mapping-response}
  [params]
  (let [{:keys [dispatch-mapping-id name description value flow-id version interaction-field channel-type active callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-dispatch-mapping-request dispatch-mapping-id name description value flow-id version interaction-field channel-type active))
        error (if-not (= (:status entity-response) 200) (e/failed-to-update-dispatch-mapping-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

(s/def ::update-disposition-params
  (s/keys :req-un [ ::specs/disposition-id ::specs/name ::specs/active ::specs/shared]
          :opt-un [ ::specs/callback ::specs/description ::specs/external-id]))

(def-sdk-fn update-disposition
  "``` javascript
  CxEngage.entities.updateDisposition({
    dispositionId: {{uuid}} (required),
    name: {{string}} (required),
    description: {{string}} (optional),
    externalId: {{string}} (optional),
    active: {{boolean}} (required),
    shared: {{boolean}} (required),
  });
  ```
  Calls rest/update-disposition-request
  with the provided data for current tenant.

  Topic: cxengage/entities/update-disposition-response

  Possible Errors:

  - [Entities: 11092](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-disposition-err)"
  {:validation ::update-disposition-params
   :topic-key :update-disposition-response}
  [params]
  (let [{:keys [disposition-id name description external-id active shared callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-disposition-request disposition-id name description external-id active shared))
        error (if-not (= (:status entity-response) 200) (e/failed-to-create-disposition-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

(s/def ::update-reason-params
  (s/keys :req-un [::specs/reason-id]
          :opt-un [::specs/name ::specs/active ::specs/shared ::specs/callback ::specs/description ::specs/external-id]))

(def-sdk-fn update-reason
  "``` javascript
  CxEngage.entities.updateReason({
       reasonId: {{uuid}} (required),
       name: {{string}} (optional),
       description: {{string}} (optional),
       externalId: {{string}} (optional),
       active: {{boolean}} (optional),
       shared: {{boolean}} (optional),
      });
  ```
  Calls rest/update-reason-request
  with the provided data for current tenant.

  Topic: cxengage/entities/update-reason-response

  Possible Errors:

  - [Entities: 11077](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-reason-err)"
  {:validation ::update-reason-params
   :topic-key :update-reason-response}
  [params]
  (let [{:keys [reason-id name description external-id active shared callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-reason-request reason-id name description external-id active shared))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-reason-err entity-response)
                  :callback callback}))))

(s/def ::update-reason-list-params
  (s/keys :req-un [::specs/reason-list-id]
          :opt-un [::specs/name ::specs/active ::specs/shared ::specs/callback ::specs/description ::specs/external-id ::specs/reasons ::specs/is-default]))

(def-sdk-fn update-reason-list
  "``` javascript
  CxEngage.entities.updateReasonList({
        reasonListId: {{uuid}} (required),
        name: {{string}} (optional),
        description: {{string}} (optional),
        externalId: {{string}} (optional),
        active: {{boolean}} (optional),
        shared: {{boolean}} (optional),
        reasons: {{object}} (optional),
        isDefault: {{boolean}} (optional),
      });
  ```
  Calls rest/update-reason-list-request
  with the provided data for current tenant.

  Topic: cxengage/entities/update-reason-list-response

  Possible Errors:

  - [Entities: 11081](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-reason-list-err)"
  {:validation ::update-reason-list-params
    :topic-key :update-reason-list-response}
  [params]
  (let [{:keys [reason-list-id name description external-id active shared reasons is-default callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-reason-list-request reason-list-id name description external-id active shared reasons is-default))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-reason-list-err entity-response)
                  :callback callback}))))


(s/def ::update-role-params
  (s/keys :req-un [::specs/role-id]
          :opt-un [::specs/callback ::specs/name ::specs/description ::specs/permissions]))

(def-sdk-fn update-role
  "``` javascript
  CxEngage.entities.updateRole({
    roleId: {{uuid}}
    name: {{string}},
    description: {{string}},
    permissions: {{array}}
  });
  ```
  Updates specified Role

  Possible Errors:

  - [Entities: 11051](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-role-err)"
  {:validation ::update-role-params
   :topic-key :update-role-response}
  [params]
  (let [{:keys [role-id name description permissions callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-role-request role-id name description permissions))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-role-err entity-response)
                  :callback callback}))))

(s/def ::update-user-skill-member-params
  (s/keys :req-un [::specs/user-id ::specs/skill-id ::specs/proficiency]
          :opt-un [::specs/callback]))

(def-sdk-fn update-user-skill-member
  "``` javascript
  CxEngage.entities.updateUserSkillMember({
    userId: {{uuid}},
    skillId: {{uuid}},
    proficiency: {{integer}}
  });
  ```
  Updates proficiency for a skill of an especific user, by calling rest/update-user-skill-member-request
  with the new data and skillId as the unique key.

  Topic: cxengage/entities/update-user-skill-member-response

  Possible Errors:

  - [Entities: 11075](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-user-skill-member-err)"
  {:validation ::update-user-skill-member-params
   :topic-key :update-user-skill-member-response}
  [params]
  (let [{:keys [user-id skill-id proficiency callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-user-skill-member-request user-id skill-id proficiency))
        error (if-not (= status 200) (e/failed-to-update-user-skill-member-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

(s/def ::update-flow-params
  (s/keys :req-un [::specs/flow-id]
          :opt-un [::specs/callback ::specs/flow-type ::specs/name ::specs/active-version ::specs/active ::specs/description]))

(def-sdk-fn update-flow
  "``` javascript
  CxEngage.entities.updateFlow({
    flowId: {{uuid}}, (required)
    flowType: {{string}}, (optional)
    name: {{string}}, (optional)
    activeVersion: {{uuid}}, (optional)
    active: {{boolean}}, (optional)
    description: {{string}} (optional)
  });
  ```
  Updates new single Flow by calling rest/update-flow-request
  with the provided data for current tenant.

  Topic: cxengage/entities/update-flow-response

  Possible Errors:

  - [Entities: 11087](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-flow-err)"
  {:validation ::update-flow-params
   :topic-key :update-flow-response}
  [params]
  (let [{:keys [flow-id flow-type name active-version active description callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-flow-request flow-id flow-type name active-version active description))
        error (if-not (= status 200) (e/failed-to-update-flow-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

;;--------------------------------------------------------------------------- ;;
;; DELETE Entity Functions
;; -------------------------------------------------------------------------- ;;

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.deleteListItem({
;;   listId: {{uuid}},
;;   listItemKey: {{string}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::delete-list-item-params
  (s/keys :req-un [::specs/list-id ::specs/list-item-key]
          :opt-un [::specs/callback]))

(def-sdk-fn delete-list-item
  ""
  {:validation ::delete-list-item-params
   :topic-key :delete-list-item-response}
  [params]
  (let [{:keys [list-id list-item-key callback topic]} params
        {:keys [api-response status] :as list-items-response} (a/<! (rest/delete-list-item-request list-id (js/encodeURIComponent list-item-key)))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-delete-list-item-err list-items-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.deleteEmailTemplate({
;;   emailTypeId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::delete-email-template-params
  (s/keys :req-un [::specs/email-type-id]
          :opt-un [::specs/callback]))

(def-sdk-fn delete-email-template
  ""
  {:validation ::delete-email-template-params
   :topic-key :delete-email-template-response}
  [params]
  (let [{:keys [email-type-id callback topic]} params
        {:keys [api-response status] :as list-items-response} (a/<! (rest/delete-email-template-request email-type-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-delete-email-template-err list-items-response)
                  :callback callback}))))

(s/def ::remove-flow-draft-params
  (s/keys :req-un [::specs/flow-id ::specs/draft-id]
          :opt-un [::specs/callback]))

(def-sdk-fn remove-flow-draft
  "``` javascript
  CxEngage.entities.removeFlowDraft({
    flowId: {{uuid}}, (required)
    draftId: {{uuid}} (required)
  });
  ```
  Removes single Flow Draft by calling rest/remove-flow-draft-request
  with the provided data for current tenant.

  Topic: cxengage/entities/remove-flow-draft-response

  Possible Errors:

  - [Entities: 11088](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-remove-flow-draft-err)"
  {:validation ::remove-flow-draft-params
   :topic-key :remove-flow-draft-response}
  [params]
  (let [{:keys [callback topic flow-id draft-id]} params
        {:keys [api-response status] :as entity-response} (a/<! (rest/remove-flow-draft-request flow-id draft-id))
        error (if-not (= status 200) (e/failed-to-remove-flow-draft-err entity-response))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

;;hygen-insert-before-delete

;; -------------------------------------------------------------------------- ;;
;; SDK Entities Module
;; -------------------------------------------------------------------------- ;;

(defrecord EntitiesModule []
  pr/SDKModule
  (start [this]
    (let [module-name :entities]
      (ih/register {:api {module-name {:get-users get-users
                                       :get-user get-user
                                       :get-queues get-queues
                                       :get-queue get-queue
                                       :get-transfer-lists get-transfer-lists
                                       :get-outbound-identifiers get-outbound-identifiers
                                       :get-outbound-identifier-list get-outbound-identifier-list
                                       :get-outbound-identifier-lists get-outbound-identifier-lists
                                       :get-user-outbound-identifier-lists get-user-outbound-identifier-lists
                                       :get-flows get-flows
                                       :get-flow get-flow
                                       :get-transfer-list get-transfer-list
                                       :get-dashboards get-dashboards
                                       :get-branding get-branding
                                       :get-protected-branding get-protected-branding
                                       :get-list get-list
                                       :get-list-item get-list-item
                                       :get-lists get-lists
                                       :get-list-types get-list-types
                                       :get-skills get-skills
                                       :get-skill get-skill
                                       :get-groups get-groups
                                       :get-group get-group
                                       :get-email-types get-email-types
                                       :get-email-templates get-email-templates
                                       :get-artifacts get-artifacts
                                       :get-artifact get-artifact
                                       :get-custom-metrics get-custom-metrics
                                       :get-custom-metric get-custom-metric
                                       :get-roles get-roles
                                       :get-role get-role
                                       :get-platform-roles get-platform-roles
                                       :get-integrations get-integrations
                                       :get-capacity-rules get-capacity-rules
                                       :get-reasons get-reasons
                                       :get-reason get-reason
                                       :get-reason-lists get-reason-lists
                                       :get-reason-list get-reason-list
                                       :get-permissions get-permissions
                                       :get-historical-report-folders get-historical-report-folders
                                       :get-data-access-reports get-data-access-reports
                                       :get-data-access-report get-data-access-report
                                       :get-data-access-member get-data-access-member
                                       :get-entity get-entity
                                       :get-message-templates get-message-templates
                                       :get-platform-user get-platform-user
                                       :get-platform-user-email get-platform-user-email
                                       :get-identity-providers get-identity-providers
                                       :get-dispositions get-dispositions
                                       :get-disposition get-disposition
                                       :get-dispatch-mappings get-dispatch-mappings
                                       :get-dispatch-mapping get-dispatch-mapping
                                      ;;hygen-insert-above-get
                                       :create-list create-list
                                       :create-list-item create-list-item
                                       :create-email-template create-email-template
                                       :create-outbound-identifier create-outbound-identifier
                                       :create-outbound-identifier-list create-outbound-identifier-list
                                       :create-role create-role
                                       :create-data-access-report create-data-access-report
                                       :create-skill create-skill
                                       :create-group create-group
                                       :create-user create-user
                                       :create-reason create-reason
                                       :create-reason-list create-reason-list
                                       :create-flow create-flow
                                       :update-flow update-flow
                                       :create-flow-draft create-flow-draft
                                       :remove-flow-draft remove-flow-draft
                                       :create-disposition create-disposition
                                       :create-dispatch-mapping create-dispatch-mapping
                                       :create-custom-metric create-custom-metric
                                      ;;hygen-insert-above-create
                                       :update-list update-list
                                       :update-list-item update-list-item
                                       :upload-list upload-list
                                       :update-email-template update-email-template
                                       :update-outbound-identifier update-outbound-identifier
                                       :delete-outbound-identifier delete-outbound-identifier
                                       :update-outbound-identifier-list update-outbound-identifier-list
                                       :add-outbound-identifier-list-member add-outbound-identifier-list-member
                                       :remove-outbound-identifier-list-member remove-outbound-identifier-list-member
                                       :update-custom-metric update-custom-metric
                                       :update-role update-role
                                       :update-data-access-report update-data-access-report
                                       :update-skill update-skill
                                       :update-group update-group
                                       :update-user update-user
                                       :update-platform-user update-platform-user
                                       :update-user-skill-member update-user-skill-member
                                       :associate associate
                                       :update-users-capacity-rule update-users-capacity-rule
                                       :update-reason update-reason
                                       :update-reason-list update-reason-list
                                       :update-disposition update-disposition
                                       :update-dispatch-mapping update-dispatch-mapping
                                      ;;hygen-insert-above-update
                                       :delete-list-item delete-list-item
                                       :delete-email-template delete-email-template
                                       :dissociate dissociate
                                      ;;hygen-insert-above-delete
                                       :download-list download-list}}

                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
