(ns cxengage-javascript-sdk.api.session
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [camel-snake-kebab.core :as camel]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.module-gateway :as mg]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.pubsub :refer [sdk-response sdk-error-response]]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.errors :as err]))

(s/def ::change-presence-state-params
  (s/keys :req-un [:specs/state]
          :opt-un [:specs/callback]))

(defn change-presence-state
  ([state params callback]
   (change-presence-state (merge (iu/extract-params params) {:callback callback :state state})))
  ([params]
   (let [params (iu/extract-params params)
         {:keys [extension callback]} params
         sub-topic "cxengage/session/state-changed"]
     (if-not (s/valid? ::change-presence-state-params params)
       (sdk-error-response sub-topic (err/invalid-params-err) callback)
       (let [{:keys [state direction]} params
             change-presence-state-msg (iu/base-module-request
                                        :SESSION/CHANGE_STATE
                                        {:tenant-id (state/get-active-tenant-id)
                                         :user-id (state/get-active-user-id)
                                         :sessionId (state/get-session-id)
                                         :state state
                                         :direction direction})]
         (go (let [change-presence-state-response (a/<! (mg/send-module-message change-presence-state-msg))
                   {:keys [result status]} change-presence-state-response]
               (if (not= status 200)
                 (let [error (err/sdk-request-error (str "Error from the server: " status))]
                   (do (sdk-error-response sub-topic error callback)
                       (sdk-error-response "cxengage/errors/error" error callback)))
                 (do (log :info (str "Sucessfully changed state to " state))
                     (state/set-user-session-state! result)
                     nil)))))))))

(def required-permissions ["CONTACTS_CREATE"
                           "CONTACTS_UPDATE"
                           "CONTACTS_READ"
                           "CONTACTS_ATTRIBUTES_READ"
                           "CONTACTS_LAYOUTS_READ"
                           "CONTACTS_ASSIGN_INTERACTION"
                           "CONTACTS_INTERACTION_HISTORY_READ"])

(s/def ::change-presence-state-ready-params
 (s/keys :req-un [:specs/extensionId]
         :opt-un [:specs/callback]))

(defn change-presence-state-ready
  ([state params callback]
   (change-presence-state-ready state (merge (iu/extract-params params) {:callback callback})))
  ([state params]
   (let [params (iu/extract-params params)
         {:keys [extensionId callback]} params
         pubsub-topic "cxengage/session/state-changed"]
    (if-not (s/valid? ::change-presence-state-ready-params params)
      (sdk-error-response pubsub-topic (err/invalid-params-err) callback)
      (let [config (state/get-session-details)
            {:keys [activeExtension]} config
            {:keys [value]} activeExtension
            active-extension-id (or value "")]
       (if (= active-extension-id extensionId)
        (change-presence-state {:state state :callback callback})
        (go (let [{:keys [extensions]} config
                  extension (first (filter #(= extensionId (:value %)) extensions))
                  set-active-extension-msg (iu/base-module-request
                                            :CRUD/SET_ACTIVE_EXTENSION
                                            {:tenant-id (state/get-active-tenant-id)
                                             :resource-id (state/get-active-user-id)
                                             :extension extension})
                  set-active-extension-response (a/<! (mg/send-module-message set-active-extension-msg))]
              (sdk-response "cxengage/voice/extension-set" set-active-extension-response callback)
              (change-presence-state {:state state :callback callback})))))))))

(s/def ::set-active-tenant-params
  (s/keys :req-un [:specs/tenantId]
          :opt-un [:specs/callback]))

(defn set-active-tenant
  ([params callback]
   (set-active-tenant (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         {:keys [callback]} params]
     (if-not (s/valid? ::set-active-tenant-params params)
       (sdk-error-response "cxengage/session/active-tenant-set" (err/invalid-params-err) callback)
       (let [{:keys [tenantId]} params
             get-config-msg (iu/base-module-request
                             :AUTH/GET_CONFIG
                             {:tenant-id tenantId
                              :user-id (state/get-active-user-id)})
             tenant-permissions (->> (state/get-user-tenants)
                                     (filter #(= tenantId (:tenantId %1)))
                                     (first)
                                     (:tenantPermissions))]
         (if-let [error (cond
                          (not (state/has-permissions? tenant-permissions required-permissions)) (err/insufficient-permissions-err)
                          :else false)]
           (sdk-error-response "cxengage/errors/error" error)
           (do
             (state/set-active-tenant! tenantId)
             (log :info "Successfully set active tenant")
             (sdk-response "cxengage/session/active-tenant-set" {:tenantId tenantId} callback)
             (go (let [get-config-response (a/<! (mg/send-module-message get-config-msg))
                       {:keys [result]} get-config-response
                       {:keys [activeExtension extensions]} result
                       start-session-msg (iu/base-module-request
                                          :SESSION/START_SESSION
                                          {:tenant-id (state/get-active-tenant-id)
                                           :user-id (state/get-active-user-id)})
                       pub-chan (mg/>get-publication-channel)]
                   (a/put! pub-chan {:type :MQTT/INIT :module-name :mqtt :config result})
                   (a/put! pub-chan {:type :TWILIO/INIT :module-name :twilio :config result})
                   (state/set-config! result)
                   (sdk-response "cxengage/voice/extensions-response" {:activeExtension activeExtension :extensions extensions})
                   (let [start-session-response (a/<! (mg/send-module-message start-session-msg))
                         session-topic "cxengage/session/started"]
                     (if-let [error (:error start-session-response)]
                       (do (sdk-error-response session-topic error callback)
                           (sdk-error-response "cxengage/errors/error" error callback))
                       (do (state/set-session-details! start-session-response)
                           (log :info "Successfully initiated presence session")
                           (sdk-response session-topic {:sessionId (:sessionId start-session-response)})
                           (change-presence-state {:state "notready"})
                           (a/put! pub-chan {:type :SQS/INIT :module-name :sqs :config (state/get-session-details)})
                           nil))))))))))))
