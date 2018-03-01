(ns cxengage-javascript-sdk.modules.entities
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn]])
  (:require [cljs.spec.alpha :as s]
            [cljs.core.async :as a]
            [camel-snake-kebab.core :as camel]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]))

;; -------------------------------------------------------------------------- ;;
;; Entity Utility Functions
;; -------------------------------------------------------------------------- ;;

(defn add-key-to-items [list-obj]
  (let [
    list-item-key (camel/->kebab-case (get (first (get-in list-obj [:list-type :fields])) :name))
    items (get list-obj :items)
    updated-items (mapv #(assoc % :key (get % (keyword list-item-key))) items)]
      (assoc list-obj :items updated-items)))

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

(s/def ::get-queues-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn get-queues
  {:validation ::get-queues-params
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

(s/def ::get-transfer-lists-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn get-transfer-lists
  {:validation ::get-transfer-lists-params
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
;; CxEngage.entities.getBranding();
;; -------------------------------------------------------------------------- ;;

(s/def ::get-branding-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn get-branding
  {:validation ::get-branding-params
   :topic-key :get-branding-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/get-branding-request))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (:result api-response)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-tenant-branding-err entity-response)
                  :callback callback}))))
;
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
                  :response (add-key-to-items (get api-response :result))
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
;; POST Entity Functions
;; -------------------------------------------------------------------------- ;;

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.createList({
;;   listTypeId: {{uuid}},
;;   name: {{string}},
;;   active: {{boolean}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::create-list-params
  (s/keys :req-un [::specs/list-type-id ::specs/name ::specs/active]
          :opt-un [::specs/callback]))

(def-sdk-fn create-list
  {:validation ::create-list-params
   :topic-key :create-list-response}
  [params]
  (let [{:keys [list-type-id name  active callback topic]} params
        {:keys [api-response status] :as entity-response} (a/<! (rest/create-list-request list-type-id name [] active))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (add-key-to-items (get api-response :result))
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
        created-list-item (:result api-response)]
    (if (= status 200)
      (p/publish {:topics topic
                  :response created-list-item
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-list-item-err entity-response)
                  :callback callback}))))

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
                  :response (e/failed-to-update-user-err update-body resource-id entity-response)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.updateList({
;;   listId: {{uuid}},
;;   name: {{string}} (optional)
;;   active: {{boolean}} (optional)
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::update-list-params
  (s/keys :req-un [::specs/list-id]
          :opt-un [::specs/callback ::name ::active]))

(def-sdk-fn update-list
  {:validation ::update-list-params
   :topic-key :update-list-response}
  [params]
  (let [{:keys [callback list-id topic name active]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/update-list-request list-id name active))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (add-key-to-items (get api-response :result))
                  :callback callback})
      (p/publish {:topics topic
                  :response (e/failed-to-update-list-err entity-response)
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
                  :response entity-response
                  :callback callback})
      (p/publish {:topics topic
                  :response (e/failed-to-update-list-item-err entity-response)
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
  {:validation ::delete-list-item-params
   :topic-key :delete-list-item-response}
  [params]
  (let [{:keys [list-id list-item-key callback topic]} params
        {:keys [api-response status] :as list-items-response} (a/<! (rest/delete-list-item-request list-id list-item-key))
        deleted-list-item? (:result api-response)]
    (if (= status 200)
      (p/publish {:topics topic
                  :response deleted-list-item?
                  :callback callback
                  :preserve-casing? true})
      (p/publish {:topics topic
                  :error (e/failed-to-delete-list-item-err list-id list-item-key list-items-response)
                  :callback callback}))))

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
                                       :get-transfer-list get-transfer-list
                                       :get-dashboards get-dashboards
                                       :get-branding get-branding
                                       :get-list get-list
                                       :get-list-item get-list-item
                                       :get-lists get-lists
                                       :create-list create-list
                                       :create-list-item create-list-item
                                       :update-user update-user
                                       :update-list update-list
                                       :update-list-item update-list-item
                                       :delete-list-item delete-list-item}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
