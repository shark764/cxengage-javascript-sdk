(ns client-sdk.api.contacts
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [client-sdk.internal-utils :as iu]
            [client-sdk.module-gateway :as mg]
            [client-sdk.state :as state]
            [client-sdk.pubsub :refer [sdk-response sdk-error-response]]
            [client-sdk.domain.specs :as specs]
            [client-sdk.domain.errors :as err]))

(s/def ::get-contact-params
  (s/keys :req-un [:specs/contactId]
          :opt-run [:specs/callback]))

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
                         :CONTACTS/LIST_CONTACT
                         (cond-> {:tenant-id (state/get-active-tenant-id)}
                                 query (assoc :query query)))]
         (go (let [search-contact-response (a/<! (mg/send-module-message search-msg))]
               (sdk-response pubsub-topic search-contact-response callback))))))))

(s/def ::create-contact-params
  (s/keys :req-un [:specs/attributes]
          :opt-un [:specs/callback]))

(defn create-contact
  ([params callback]
   (create-contact (merge iu/extract-params params) {:callback callback}))
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
   (update-contact (merge iu/extract-params params) {:callback callback}))
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
   (delete-contact (merge iu/extract-params params) {:callback callback}))
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
