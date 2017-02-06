(ns cxengage-javascript-sdk.api.contacts
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.module-gateway :as mg]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.pubsub :refer [sdk-response sdk-error-response]]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.errors :as err]))

(s/def ::assign-contact-params
  (s/keys :req-un [:specs/interactionId
                   :specs/contactId]
          :opt-un [:specs/callback]))

(defn contact-interaction-assignment
  ([assignment-type params callback]
   (contact-interaction-assignment assignment-type (merge (iu/extract-params params) {:callback callback})))
  ([assignment-type params]
   (let [params (iu/extract-params params)
         toggle (case assignment-type
                  :unassign ["interaction-contact-deselected" "contact-unassigned"]
                  :assign ["interaction-contact-selected" "contact-assigned"]
                  (do (log :error "Invalid contact assignment operation")
                      nil))
         pubsub-topic (str "cxengage/interactions/" (second toggle))
         {:keys [contactId interactionId callback]} params]
     (if-let [error (cond
                      (not (s/valid? ::assign-contact-params params)) (err/invalid-params-err)
                      (not (state/session-started?)) (err/invalid-sdk-state-err "Your session isn't started yet.")
                      (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Your active tenant isn't set yet."))]
       (sdk-error-response pubsub-topic error callback)
       (let [interaction-details (state/get-interaction interactionId)
             {:keys [subId actionId channelType resourceId tenantId resource direction]} interaction-details
             {:keys [extension roleId sessionId workOfferId]} resource
             contact-msg (iu/base-module-request
                          :INTERACTIONS/SEND_INTERRUPT
                          {:interruptType (first toggle)
                           :source "client"
                           :tenantId tenantId
                           :interactionId interactionId
                           :interrupt {:tenantId tenantId
                                       :contactId contactId
                                       :interactionId interactionId
                                       :subId subId
                                       :actionId actionId
                                       :workofferId workOfferId
                                       :sessionId sessionId
                                       :resourceId resourceId
                                       :direction direction
                                       :channelType channelType}})]
         (log :debug "AAAAAAAAAAAAAAAAAa" contact-msg)
         (go (let [assign-contact-response (a/<! (mg/send-module-message contact-msg))]
               (sdk-response pubsub-topic assign-contact-response callback)
               nil)))))))

(s/def ::get-contact-params
  (s/keys :req-un [:specs/contactId]
          :opt-un [:specs/callback]))

(defn get-contact
  ([params callback]
   (get-contact (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/contacts/get-response"
         {:keys [contactId callback]} params]
     (if-let [error (cond
                      (not (s/valid? ::get-contact-params params)) (err/invalid-params-err)
                      (not (state/session-started?)) (err/invalid-sdk-state-err "Your session isn't started yet.")
                      (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Your active tenant isn't set yet.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [contact-msg (iu/base-module-request
                          :CONTACTS/GET_CONTACT
                          {:tenant-id (state/get-active-tenant-id)
                           :contact-id contactId})]
         (go (let [get-contact-response (a/<! (mg/send-module-message contact-msg))]
               (sdk-response pubsub-topic get-contact-response callback))))))))

(s/def ::search-contacts-params
  (s/keys :req-un []
          :opt-un [:specs/query
                   :specs/callback]))

(defn search-contacts
  ([params callback]
   (search-contacts (merge (iu/extract-params params) {:callback callback})))
  ([]
   (search-contacts {}))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/contacts/search-response"
         {:keys [query callback]} params]
     (if-let [error (cond
                      (not (s/valid? ::search-contacts-params params)) (err/invalid-params-err)
                      (not (state/session-started?)) (err/invalid-sdk-state-err "Your session isn't started yet.")
                      (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Your active tenant isn't set yet.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [search-msg (iu/base-module-request
                         :CONTACTS/SEARCH_CONTACTS
                         (cond-> {:tenant-id (state/get-active-tenant-id)}
                           query (assoc :query query)))]
         (go (let [search-contact-response (a/<! (mg/send-module-message search-msg))]
               (sdk-response pubsub-topic search-contact-response callback))))))))

(s/def ::create-contact-params
  (s/keys :req-un [:specs/attributes]
          :opt-un [:specs/callback]))

(defn create-contact
  ([params callback]
   (create-contact (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/contacts/create-response"
         {:keys [attributes callback]} params]
     (if-let [error (cond
                      (not (s/valid? ::create-contact-params params)) (err/invalid-params-err)
                      (not (state/session-started?)) (err/invalid-sdk-state-err "Your session isn't started yet.")
                      (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Your active tenant isn't set yet.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [create-msg (iu/base-module-request
                         :CONTACTS/CREATE_CONTACT
                         {:tenant-id (state/get-active-tenant-id)
                          :attributes attributes})]
         (go (let [create-contact-response (a/<! (mg/send-module-message create-msg))]
               (sdk-response pubsub-topic create-contact-response callback))))))))

(s/def ::update-contact-params
  (s/keys :req-un [:specs/contactId
                   :specs/attributes]
          :opt-un [:specs/callback]))

(defn update-contact
  ([params callback]
   (update-contact (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/contacts/update-response"
         {:keys [attributes callback contactId]} params]
     (if-let [error (cond
                      (not (s/valid? ::update-contact-params params)) (err/invalid-params-err)
                      (not (state/session-started?)) (err/invalid-sdk-state-err "Your session isn't started yet.")
                      (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Your active tenant isn't set yet.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [update-msg (iu/base-module-request
                         :CONTACTS/UPDATE_CONTACT
                         {:tenant-id (state/get-active-tenant-id)
                          :contact-id contactId
                          :attributes attributes})]
         (go (let [update-contact-response (a/<! (mg/send-module-message update-msg))]
               (sdk-response pubsub-topic update-contact-response callback))))))))

(s/def ::delete-contact-params
  (s/keys :req-un [:specs/contactId]
          :opt-un [:specs/callback]))

(defn delete-contact
  ([params callback]
   (delete-contact (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/contacts/delete-response"
         {:keys [attributes callback contactId]} params]
     (if-let [error (cond
                      (not (s/valid? ::delete-contact-params params)) (err/invalid-params-err)
                      (not (state/session-started?)) (err/invalid-sdk-state-err "Your session isn't started yet.")
                      (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Your active tenant isn't set yet.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [delete-msg (iu/base-module-request
                         :CONTACTS/DELETE_CONTACT
                         {:tenant-id (state/get-active-tenant-id)
                          :contact-id contactId
                          :attributes attributes})]
         (go (let [delete-contact-response (a/<! (mg/send-module-message delete-msg))]
               (sdk-response pubsub-topic delete-contact-response callback))))))))

(s/def ::list-attributes-params
  (s/keys :req-un []
          :opt-un [:specs/callback]))

(defn list-attributes
  ([params callback]
   (list-attributes {:callback callback}))
  ([]
   (list-attributes {}))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/contacts/list-attributes-response"
         {:keys [callback]} params]
     (if-let [error (cond
                      (not (s/valid? ::list-attributes-params params)) (err/invalid-params-err)
                      (not (state/session-started?)) (err/invalid-sdk-state-err "Your session isn't started yet.")
                      (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Your active tenant isn't set yet.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [qp-msg (iu/base-module-request
                     :CONTACTS/LIST_ATTRIBUTES
                     {:tenant-id (state/get-active-tenant-id)})]
         (go (let [list-attributes-response (a/<! (mg/send-module-message qp-msg))
                   relevant-attributes  (->> list-attributes-response
                                             (filterv #(:active %))
                                             (mapv (fn [{:keys [type label mandatory objectName default]}]
                                                     (clj->js {:type type
                                                               :label label
                                                               :mandatory mandatory
                                                               :objectName objectName
                                                               :default default}))))]
               (sdk-response pubsub-topic relevant-attributes callback))))))))

(s/def ::get-layout-params
  (s/keys :req-un [:specs/layoutId]
          :opt-un [:specs/callback]))

(defn get-layout
  ([params callback]
   (get-layout (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/contacts/get-layout-response"
         {:keys [layoutId callback]} params]
     (if-let [error (cond
                      (not (s/valid? ::get-layout-params params)) (err/invalid-params-err)
                      (not (state/session-started?)) (err/invalid-sdk-state-err "Your session isn't started yet.")
                      (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Your active tenant isn't set yet.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [layout-msg (iu/base-module-request
                         :CONTACTS/GET_LAYOUT
                         {:tenant-id (state/get-active-tenant-id)
                          :layout-id layoutId})]
         (go (let [get-layout-response (a/<! (mg/send-module-message layout-msg))]
               (sdk-response pubsub-topic get-layout-response callback))))))))

(s/def ::list-layouts-params
  (s/keys :req-un []
          :opt-un [:specs/callback]))

(defn list-layouts
  ([params callback]
   (list-layouts {:callback callback}))
  ([]
   (list-layouts {}))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/contacts/list-layouts-response"
         {:keys [callback]} params]
     (if-let [error (cond
                      (not (s/valid? ::list-layouts-params params)) (err/invalid-params-err)
                      (not (state/session-started?)) (err/invalid-sdk-state-err "Your session isn't started yet.")
                      (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Your active tenant isn't set yet.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [layout-msg (iu/base-module-request
                         :CONTACTS/LIST_LAYOUTS
                         {:tenant-id (state/get-active-tenant-id)})]
         (go (let [list-layouts-response (a/<! (mg/send-module-message layout-msg))
                   relevant-layouts  (->> list-layouts-response
                                          (filterv #(:active %))
                                          (mapv (fn [{:keys [id layout]}]
                                                  (clj->js {:id id
                                                            :layout layout}))))]
               (sdk-response pubsub-topic relevant-layouts callback))))))))
