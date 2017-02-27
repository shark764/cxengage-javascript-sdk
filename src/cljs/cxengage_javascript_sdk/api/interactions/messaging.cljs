(ns cxengage-javascript-sdk.api.interactions.messaging
  (:require-macros [lumbajack.macros :refer [log]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.state :as state]
            [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.pubsub :refer [sdk-response sdk-error-response]]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.errors :as err]
            [cxengage-javascript-sdk.module-gateway :as mg]
            [cxengage-javascript-sdk.internal-utils :as iu]))



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
