(ns cxengage-javascript-sdk.modules.entities
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn]])
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

(defn add-key-to-items [list-obj]
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getUser({
;;   resourceId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-user-params
  (s/keys :req-un [::specs/resource-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-user
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
                  :error (e/failed-to-get-user-list-err entity-response)
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getDashboards();
;; -------------------------------------------------------------------------- ;;

(s/def ::get-dashboards-params
  (s/keys :req-un []
          :opt-un [::specs/callback ::specs/exclude-inactive]))

(def-sdk-fn get-dashboards
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getGroups();
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn get-groups
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getSkills();
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn get-skills
  {:validation ::get-entities-params
   :topic-key :get-skills-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as skills-response} (a/<! (rest/get-skills-request))]
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

;;hygen-insert-before-get

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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.createOutboundIdentifierList({
;;   active: {{boolean}},
;;   name: {{string}},
;;   description: {{string}},
;;})
;; -------------------------------------------------------------------------- ;;

(s/def ::create-outbound-identifier-list-params
  (s/keys :req-un [::specs/active ::specs/name ::specs/description]
          :opt-un [::specs/callback]))

(def-sdk-fn create-outbound-identifier-list
  {:validation ::create-outbound-identifier-list-params
   :topic-key :create-outbound-identifier-list-response}
  [params]
  (let [{:keys [active name description callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/create-outbound-identifier-list-request active name description ))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-outbound-identifier-list-err entity-response)
                  :callback callback}))))

;;hygen-insert-before-create

;;--------------------------------------------------------------------------- ;;
;; PUT Entity Functions
;; -------------------------------------------------------------------------- ;;

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.updateUser({
;;   resourceId: {{uuid}},
;;   updateBody: {{object}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::update-user-params
  (s/keys :req-un [::specs/update-body ::specs/resource-id]
          :opt-un [::specs/callback]))

(def-sdk-fn update-user
  {:validation ::update-user-params
   :topic-key :update-user-response}
  [params]
  (let [{:keys [callback topic update-body resource-id]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/crud-entity-request :put "user" resource-id update-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-user-err update-body resource-id entity-response)
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

;;hygen-insert-before-update

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
                                       :get-groups get-groups
                                       :get-email-types get-email-types
                                       :get-email-templates get-email-templates
                                       :get-artifacts get-artifacts
                                       :get-artifact get-artifact
                                      ;;hygen-insert-above-get
                                       :create-list create-list
                                       :create-list-item create-list-item
                                       :create-email-template create-email-template
                                       :create-outbound-identifier create-outbound-identifier
                                       :create-outbound-identifier-list create-outbound-identifier-list
                                      ;;hygen-insert-above-create
                                       :update-user update-user
                                       :update-list update-list
                                       :update-list-item update-list-item
                                       :upload-list upload-list
                                       :update-email-template update-email-template
                                       :update-outbound-identifier update-outbound-identifier
                                       :update-outbound-identifier-list update-outbound-identifier-list
                                      ;;hygen-insert-above-update
                                       :delete-list-item delete-list-item
                                       :delete-email-template delete-email-template
                                      ;;hygen-insert-above-delete
                                       :download-list download-list
                                       }}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
