(ns cxengage-javascript-sdk.modules.entities
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]]
                   [cljs-sdk-utils.macros :refer [def-sdk-fn]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cljs-sdk-utils.protocols :as pr]
            [cljs-sdk-utils.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cljs-sdk-utils.specs :as specs]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs-sdk-utils.interop-helpers :as ih]
            [cljs-sdk-utils.specs :as specs]))

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
        {:keys [status api-response]} (a/<! (rest/crud-entity-request :get "user" resource-id))]
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
  {:validation ::get-users-params
   :topic-key :get-users-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response]} (a/<! (rest/crud-entities-request :get "user"))]
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
  {:validation ::get-queue-params
   :topic-key :get-queue-response}
  [params]
  (let [{:keys [callback topic queue-id]} params
        {:keys [status api-response]} (a/<! (rest/crud-entity-request :get "queue" queue-id))]
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
  {:validation ::get-queues-params
   :topic-key :get-queues-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response]} (a/<! (rest/crud-entities-request :get "queue"))]
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
  {:validation ::get-transfer-list-params
   :topic-key :get-transfer-list-response}
  [params]
  (let [{:keys [callback topic transfer-list-id]} params
        {:keys [status api-response]} (a/<! (rest/crud-entity-request :get "transfer-list" transfer-list-id))]
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
  {:validation ::get-transfer-lists-params
   :topic-key :get-transfer-lists-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response]} (a/<! (rest/crud-entities-request :get "transfer-list"))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
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
        {:keys [status api-response]} (a/<! (rest/get-branding-request))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response (:result api-response)
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
  {:validation ::update-user-params
   :topic-key :update-user-response}
  [params]
  (let [{:keys [callback topic update-body resource-id]} params
        {:keys [status api-response]} (a/<! (rest/crud-entity-request :put "user" resource-id update-body))]
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
                                       :update-user update-user
                                       :get-branding get-branding}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
