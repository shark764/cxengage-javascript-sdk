(ns cxengage-javascript-sdk.modules.entities
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]])
  (:require [cljs.spec.alpha :as s]
            [cljs.core.async :as a]
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
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entity-request :get "user" resource-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-user-err resource-id entity-response)
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getQueue({
;;   queueId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-queue-params
  (s/keys :req-un [::specs/queue-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-queue
  ""
  {:validation ::get-queue-params
   :topic-key :get-queue-response}
  [params]
  (let [{:keys [callback topic queue-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entity-request :get "queue" queue-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-queue-err queue-id entity-response)
                  :callback callback}))))

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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getFlows();
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn get-flows
  ""
  {:validation ::get-entities-params
   :topic-key :get-flows-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "flow"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-flows-err entity-response)
                  :callback callback}))))

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
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "custom-metric"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-custom-metrics-err entity-response)
                  :callback callback}))))

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
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entity-request :get "custom-metric" custom-metric-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-custom-metric-err entity-response)
                  :callback callback}))))

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
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entities-request :get "data-access-report"))]
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
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-data-access-report-request data-access-report-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-data-access-report-err entity-response)
                  :callback callback}))))

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

;;hygen-insert-before-get

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
          :opt-un [::specs/callback ::specs/description ::specs/realtime-report-type ::specs/realtime-report-name ::specs/historical-catalog-name]))

(def-sdk-fn create-data-access-report
  "``` javascript
  CxEngage.entities.createDataAccessReport({
    name: {{string}},
    description: {{string}}, (optional)
    active: {{boolean}},
    reportType: {{string}},
    realtimeReportType: {{string}}, (optional)
    realtimeReportName: {{string}}, (optional)
    historicalCatalogName: {{string}}, (optional)
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
  (let [{:keys [name description active report-type realtime-report-type realtime-report-name historical-catalog-name callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-data-access-report-request name description active report-type realtime-report-type realtime-report-name historical-catalog-name))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
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

(s/def ::create-user-params
  (s/keys :req-un [::specs/email ::specs/role-id ::specs/platform-role-id]
          :opt-un [::specs/callback ::specs/status ::specs/default-identity-provider ::specs/no-password ::specs/work-station-id ::specs/external-id ::specs/extensions ::specs/first-name ::specs/last-name ::specs/capacity-rule-id]))

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
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-user-request email, role-id, default-identity-provider, no-password, status, work-station-id, external-id, extensions, first-name, last-name, capacity-rule-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-user-err entity-response)
                  :callback callback}))))

(s/def ::create-role-params
  (s/keys :req-un [::specs/name ::specs/description]
          :opt-un [::specs/callback]))

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


;;--------------------------------------------------------------------------- ;;
;; PUT Entity Functions
;; -------------------------------------------------------------------------- ;;

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
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entity-request :put "user" user-id user-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-user-err entity-response)
                  :callback callback}))))

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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.updateOutboundIdentifier({
;;   id: {{uuid}}
;;   name: {{string}} (optional)
;;   active: {{boolean}} (optional)
;;   value: {{string}} (optional)
;;   flowId: {{string}} (optional)
;;   channelType: {{string}} (optional)
;;   description: {{string}} (optional)
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::update-outbound-identifier-params
  (s/keys :req-un [::specs/outbound-identifier-id]
          :opt-un [::specs/callback ::specs/name ::specs/active ::specs/value ::specs/flow-id ::specs/channel-type ::specs/description]))

(def-sdk-fn update-outbound-identifier
  ""
  {:validation ::update-outbound-identifier-params
   :topic-key :update-outbound-identifier-response}
  [params]
  (let [{:keys [callback outbound-identifier-id topic name active value flowId channel-type description]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-outbound-identifier-request outbound-identifier-id name active value flowId channel-type description))]
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
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-list-item-request list-id list-item-key item-value))]
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
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-custom-metric-request description custom-metrics-type custom-metric-id active sla-abandon-type sla-threshold name sla-abandon-threshold))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-custom-metric-err entity-response)
                  :callback callback}))))

(s/def ::update-data-access-report-params
    (s/keys :req-un [::specs/data-access-report-id]
            :opt-un [::specs/callback ::specs/name ::specs/description ::specs/active ::specs/report-type ::specs/realtime-report-type ::specs/realtime-report-name ::specs/historical-catalog-name ::specs/member-ids]))

(def-sdk-fn update-data-access-report
  "``` javascript
  CxEngage.entities.updateDataAccessReport({
    dataAccessReportId: {{uuid}},
    name: {{string}}, (optional)
    description: {{string}}, (optional)
    active: {{boolean}}, (optional)
    reportType: {{string}}, (optional)
    realtimeReportType: {{string}}, (optional)
    realtimeReportName: {{string}}, (optional)
    historicalCatalogName: {{string}}, (optional)
    memberIds: {{array}}, (optional)
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
  (let [{:keys [data-access-report-id name description active report-type realtime-report-type realtime-report-name historical-catalog-name member-ids callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-data-access-report-request data-access-report-id name description active report-type realtime-report-type realtime-report-name historical-catalog-name member-ids))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
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

(def-sdk-fn update-skill-member
  "``` javascript
  CxEngage.entities.updateSkillMember({
    skillId: {{uuid}},
    userId: {{uuid}},
    proficiency: {{integer}}
  });
  ```
  Updates a single user member from skill by calling rest/update-skill-member-request
  with skillId and userId as the unique keys and the proficiency.

  Topic: cxengage/entities/update-skill-member-response

  Possible Errors:

  - [Entities: 11068](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-skill-member-err)"
  {:validation ::update-skill-member-params
   :topic-key :update-skill-member-response}
  [params]
  (let [{:keys [callback topic skill-id user-id proficiency]} params
        {:keys [api-response status] :as entity-response} (a/<! (rest/update-skill-member-request skill-id user-id proficiency))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-skill-member-err entity-response)
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
        {:keys [api-response status] :as list-items-response} (a/<! (rest/delete-list-item-request list-id list-item-key))]
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
                                       :get-platform-roles get-platform-roles
                                       :get-integrations get-integrations
                                       :get-capacity-rules get-capacity-rules
                                       :get-reasons get-reasons
                                       :get-reason-lists get-reason-lists
                                       :get-permissions get-permissions
                                       :get-historical-report-folders get-historical-report-folders
                                       :get-data-access-reports get-data-access-reports
                                       :get-data-access-report get-data-access-report
                                       :get-data-access-member get-data-access-member
                                       :get-entity get-entity
                                       :get-message-templates get-message-templates
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
                                       :add-role-list-member add-role-list-member
                                       :remove-role-list-member remove-role-list-member
                                       :update-custom-metric update-custom-metric
                                       :update-role update-role
                                       :update-data-access-report update-data-access-report
                                       :update-skill update-skill
                                       :add-skill-member add-skill-member
                                       :remove-skill-member remove-skill-member
                                       :update-skill-member update-skill-member
                                       :update-group update-group
                                       :add-group-member add-group-member
                                       :remove-group-member remove-group-member
                                       :update-user update-user
                                       :associate associate
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
