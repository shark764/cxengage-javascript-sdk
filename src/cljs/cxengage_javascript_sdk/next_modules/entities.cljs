(ns cxengage-javascript-sdk.next-modules.entities
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cxengage-javascript-sdk.macros :refer [def-sdk-fn]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.specs :as specs]))

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
  ::get-user-params
  (p/get-topic :get-user-response)
  [params]
  (let [{:keys [callback topic resource-id]} params
        tenant-id (st/get-active-tenant-id)
        url (str (st/get-base-api-url) "tenants/tenant-id/users/resource-id")
        get-user-request {:method :get
                          :url (iu/build-api-url-with-params
                                url
                                {:tenant-id tenant-id
                                 :resource-id resource-id})}
        {:keys [status api-response]} (a/<! (iu/api-request get-user-request))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getUsers();
;; -------------------------------------------------------------------------- ;;

(s/def ::get-users-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn get-users
  ::get-users-params
  (p/get-topic :get-users-response)
  [params]
  (let [{:keys [callback topic]} params
        tenant-id (st/get-active-tenant-id)
        url (str (st/get-base-api-url) "tenants/tenant-id/users")
        get-users-request {:method :get
                           :url (iu/build-api-url-with-params
                                 url
                                 {:tenant-id tenant-id})}
        {:keys [status api-response]} (a/<! (iu/api-request get-users-request))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
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
  ::get-queue-params
  (p/get-topic :get-queue-response)
  [params]
  (let [{:keys [callback topic queue-id]} params
        tenant-id (st/get-active-tenant-id)
        url (str (st/get-base-api-url) "tenants/tenant-id/queues/queue-id")
        get-queue-request {:method :get
                           :url (iu/build-api-url-with-params
                                 url
                                 {:tenant-id tenant-id
                                  :queue-id queue-id})}
        {:keys [status api-response]} (a/<! (iu/api-request get-queue-request))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getQueues();
;; -------------------------------------------------------------------------- ;;

(s/def ::get-queues-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn get-queues
  ::get-queues-params
  (p/get-topic :get-queues-response)
  [params]
  (let [{:keys [callback topic]} params
        tenant-id (st/get-active-tenant-id)
        url (str (st/get-base-api-url) "tenants/tenant-id/queues")
        get-queues-request {:method :get
                           :url (iu/build-api-url-with-params
                                 url
                                 {:tenant-id tenant-id})}
        {:keys [status api-response]} (a/<! (iu/api-request get-queues-request))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
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
  ::get-transfer-list-params
  (p/get-topic :get-transfer-list-response)
  [params]
  (let [{:keys [callback topic transfer-list-id]} params
        tenant-id (st/get-active-tenant-id)
        url (str (st/get-base-api-url) "tenants/tenant-id/transfer-lists/transfer-list-id")
        get-transfer-list-request {:method :get
                                   :url (iu/build-api-url-with-params
                                         url
                                         {:tenant-id tenant-id
                                          :transfer-list-id transfer-list-id})}
        {:keys [status api-response]} (a/<! (iu/api-request get-transfer-list-request))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.getTransferLists();
;; -------------------------------------------------------------------------- ;;

(s/def ::get-transfer-lists-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn get-transfer-lists
  ::get-transfer-lists-params
  (p/get-topic :get-transfer-lists-response)
  [params]
  (let [{:keys [callback topic]} params
        tenant-id (st/get-active-tenant-id)
        url (str (st/get-base-api-url) "tenants/tenant-id/transfer-lists")
        get-transfer-lists-request {:method :get
                                    :url (iu/build-api-url-with-params
                                          url
                                          {:tenant-id tenant-id})}
        {:keys [status api-response]} (a/<! (iu/api-request get-transfer-lists-request))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
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
  ::update-user-params
  (p/get-topic :update-user-response)
  [params]
  (let [{:keys [callback topic update-body resource-id]} params
        tenant-id (st/get-active-tenant-id)
        url (str (st/get-base-api-url) "tenants/tenant-id/users/resource-id")
        put-user-request {:method :put
                          :body update-body
                          :url (iu/build-api-url-with-params
                                url
                                {:tenant-id tenant-id
                                 :resource-id resource-id})}
        {:keys [status api-response]} (a/<! (iu/api-request put-user-request))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
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
                                       :update-user update-user}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
