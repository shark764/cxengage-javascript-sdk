(ns cxengage-javascript-sdk.modules.email
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [ajax.core :as ax :refer [POST]]))

#_(defn attachment-operation
    ([module operation] (e/wrong-number-of-args-error))
    ([module operation params & others]
     (if-not (fn? (js->clj (first others)))
       (e/wrong-number-of-args-error)
       (attachment-operation module operation (merge (iu/extract-params params) {:callback (first others)}))))
    ([module operation params]
     (let [params (iu/extract-params params)
           {:keys [interaction-id callback file]} params
           {:keys [name]} file
           interaction (state/get-interaction interaction-id)
           artifact-id (get-in interaction [:email-reply-details :artifact :artifact-id])
           reply-interaction-id (get-in interaction [:email-reply-details :reply-interaction-id])]
       (if (= operation :remove)
         (js/console.log "removing attachment")
         (let [tenant-id (state/get-active-tenant-id)
               upload-url (str (state/get-base-api-url) "tenants/tenant-id/interactions/interaction-id/artifacts/artifact-id")
               form-data (doto (js/FormData.) (.append "attachment" file "attachment"))
               upload-request {:method :post
                               :url (iu/build-api-url-with-params
                                     upload-url
                                     {:tenant-id tenant-id
                                      :interaction-id reply-interaction-id
                                      :artifact-id artifact-id})
                               :body form-data}]

           (go (let [upload-response (a/<! (iu/file-api-request upload-request))
                     _ (js/console.log "UPLOAD RESPONSE" upload-response)]))
           nil)))))

(s/def ::get-attachment-params
  (s/keys :req-un [::specs/interaction-id ::specs/artifact-id ::specs/artifact-file-id]
          :opt-un [::specs/callback]))

(defn get-attachment-url
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (get-attachment-url module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         {:keys [interaction-id artifact-file-id artifact-id callback]} params
         tenant-id (state/get-active-tenant-id)
         url (str (state/get-base-api-url) "tenants/tenant-id/interactions/interaction-id/artifacts/artifact-id")
         url (iu/build-api-url-with-params
              url
              {:tenant-id tenant-id
               :interaction-id interaction-id
               :artifact-id artifact-id})
         req {:url url
              :method :get}]
     (go (let [attachment-response (a/<! (iu/api-request req))
               {:keys [api-response status]} attachment-response
               attachment (first (filterv #(= (:artifact-file-id %) artifact-file-id) (:files api-response)))]
           (p/publish {:topics (p/get-topic :attachment-received)
                       :response attachment
                       :callback callback})))
     nil)))

(s/def ::add-attachment-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(defn add-attachment
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (add-attachment module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         {:keys [interaction-id file callback]} params
         attachment-id (id/uuid-string (id/make-random-uuid))]
     (if-not (s/valid? ::add-attachment-params params)
       (p/publish {:topics (p/get-topic :add-attachment)
                   :response (e/invalid-args-error "invalid args")
                   :callback callback})
       (do (state/add-attachment-to-reply {:interaction-id interaction-id
                                           :attachment-id attachment-id
                                           :file file})
           (p/publish {:topics (p/get-topic :add-attachment)
                       :response {:interaction-id interaction-id :attachment-id attachment-id}
                       :callback callback})
           nil)))))

(s/def ::remove-attachment-params
  (s/keys :req-un [::specs/attachment-id ::specs/interaction-id]
          :opt-un [::specs/callback]))

(defn remove-attachment
  ([module] (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (remove-attachment module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [params (iu/extract-params params)
         {:keys [interaction-id attachment-id callback]} params]
     (if-not (s/valid? ::remove-attachment-params params)
       (p/publish {:topics (p/get-topic :remove-attachment)
                   :response (e/invalid-args-error "invalid args")
                   :callback callback})
       (do (state/remove-attachment-from-reply {:interaction-id interaction-id
                                                :attachment-id attachment-id})
           (p/publish {:topics (p/get-topic :remove-attachment)
                       :response {:interaction-id interaction-id}
                       :callback callback})
           nil)))))

(def initial-state
  {:module-name :email})

(defrecord EmailModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)
          email-integration true]
      (if-not email-integration
        (a/put! core-messages< {:module-registration-status :failure
                                :module module-name})
        (do (register {:api {:interactions {:email {:add-attachment (partial add-attachment this)
                                                    :remove-attachment (partial remove-attachment this)
                                                    :get-attachment-url (partial get-attachment-url this)}}}
                       :module-name module-name})
            (js/console.info "<----- Started " (name module-name) " module! ----->")))))
  (stop [this]))
