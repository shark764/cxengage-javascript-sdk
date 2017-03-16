(ns cxengage-javascript-sdk.modules.email
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
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

(s/def ::get-artifact-file-params
  (s/keys :req-un [::specs/interaction-id ::specs/artifact-id ::specs/artifact-file-id]
          :opt-un [::specs/callback]))

(defn get-artifact-file
  ([module]
   (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (get-artifact-file module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [{:keys [interaction-id artifact-id artifact-file-id callback] :as params} (iu/extract-params params)
         api-url (get-in module [:config :api-url])
         module-state @(:state module)
         base-url (str api-url (get-in module-state [:urls :artifact-file]))
         request-url (iu/build-api-url-with-params
                      base-url
                      {:tenant-id (state/get-active-tenant-id)
                       :interaction-id interaction-id
                       :artifact-id artifact-id})
         topic (p/get-topic :artifact-received)]
     (if-not (s/valid? ::get-artifact-file-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::get-artifact-file-params params))
                   :callback callback})
       (let [request-map {:url request-url
                          :method :get}]
         (go (let [response (a/<! (iu/api-request request-map))
                   {:keys [status api-response]} response
                   {:keys [files]} api-response]
               (if (not= status 200)
                 (p/publish {:topics topic
                             :error (e/api-error api-response)
                             :callback callback})
                 (if-let [artifact-file (->> files
                                             (filterv #(= (:artifact-file-id %1) artifact-file-id))
                                             (peek))]
                   (p/publish {:topics topic
                               :response {:interaction-id interaction-id
                                          :artifact-file artifact-file}
                               :callback callback})
                   (p/publish {:topics topic
                               :error (e/invalid-artifact-file)
                               :callback callback})))))
         nil)))))

(def initial-state
  {:module-name :email})

(defrecord EmailModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)
          email-integration (state/get-integration-by-type "email")
          email-integration {:email "latrosbc@gmail.com"}]
      (if-not email-integration
        (a/put! core-messages< {:module-registration-status :failure
                                :module module-name})
        (do (register {:api {:interactions {:email {;;:add-attachment (partial attachment-operation this :add)
                                                    ;;:remove-attachment (partial attachment-operation this :remove)
                                                    :getAttachment (partial get-artifact-file this)}}}
                       :module-name module-name})
            (js/console.info "<----- Started " (name module-name) " module! ----->")))))
  (stop [this]))
