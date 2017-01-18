(ns client-sdk.api.interactions.messaging
  (:require-macros [lumbajack.macros :refer [log]]
                   [cljs.core.async.macros :refer [go]])
  (:require [client-sdk.state :as state]
            [cljs.core.async :as a]
            [cljs.spec :as s]
            [client-sdk.pubsub :refer [sdk-response sdk-error-response]]
            [client-sdk.domain.specs :as specs]
            [client-sdk.domain.errors :as err]
            [client-sdk.module-gateway :as mg]
            [client-sdk.internal-utils :as iu]))

(s/def ::message string?)
(s/def ::send-message-params
  (s/keys :req-un [::specs/interactionId
                   ::message]
          :opt-un [::specs/callback]))

(defn send-message
  ([params callback]
   (send-message (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/messaging/send-message-response"
         {:keys [callback]} params]
     (if-let [error (cond
                      (not (s/valid? ::send-message-params params)) (err/invalid-params-err)
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [{:keys [interactionId message]} params
             send-message-body (iu/base-module-request
                                :MQTT/SEND_MESSAGE
                                {:tenantId (state/get-active-tenant-id)
                                 :userId (state/get-active-user-id)
                                 :interactionId interactionId
                                 :message message})]
         (go (let [send-message-response (a/<! (mg/send-module-message send-message-body))]
               (sdk-response pubsub-topic send-message-response callback)
               nil)))))))
