(ns cxengage-javascript-sdk.api.interactions.voice
  (:require-macros [lumbajack.macros :refer [log]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cxengage-javascript-sdk.state :as state]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.pubsub :refer [sdk-response sdk-error-response]]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.errors :as err]
            [cxengage-javascript-sdk.module-gateway :as mg]))

(s/def ::voice-features-params
    (s/keys :req-un [:specs/interactionId]
            :opt-un [:specs/callback]))

(defn auxiliary-features
  ([interrupt-type params callback]
   (auxiliary-features interrupt-type (merge (iu/extract-params params) {:callback callback})))
  ([interrupt-type params]
   (let [params (iu/extract-params params)
         pubsub-topic (str "cxengage/voice/phone-controls-response")
         {:keys [interactionId callback]} params]
    (if-let [error (cond
                     (not (s/valid? ::voice-features-params params)) (err/invalid-params-err "Invalid interaction ID")
                     (not (state/session-started?)) (err/invalid-sdk-state-err "Session not started.")
                     (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Active Tenant not set.")
                     (not (state/presence-state-matches? "ready")) (err/invalid-sdk-state-err "User is not set to 'Ready'.")
                     (not (state/interaction-exists-in-state? :active interactionId)) (err/invalid-sdk-state-err "No such active Interaction.")
                     :else false)]
      error
      (let [module-chan (state/get-module-chan :interactions)
            voice-tool-body (merge (iu/base-module-request
                                    :INTERACTIONS/SEND_INTERRUPT
                                    {:tenantId (state/get-active-tenant-id)
                                     :interruptType interrupt-type
                                     :source "client"
                                     :interrupt {:resourceId (state/get-active-user-id)}
                                     :interactionId interactionId}))]
        (go (let [voice-tool-response (a/<! (mg/send-module-message voice-tool-body))]
              (sdk-response pubsub-topic voice-tool-response callback))))))))

(s/def ::voice-transfer-params
    (s/keys :req-un [:specs/interactionId]
            :opt-un [:specs/callback]))

(defn transfer
  ([interrupt-type params callback]
   (transfer interrupt-type (merge (iu/extract-params params) {:callback callback})))
  ([interrupt-type params]
   (let [params (iu/extract-params params)
         pubsub-topic (str "cxengage/voice/transfer-response")
         {:keys [interactionId resourceId queueId callback]} params]
    (if-let [error (cond
                     (not (s/valid? ::voice-transfer-params params)) (err/invalid-params-err "Invalid interaction ID")
                     (not (state/session-started?)) (err/invalid-sdk-state-err "Session not started.")
                     (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Active Tenant not set.")
                     (not (state/presence-state-matches? "ready")) (err/invalid-sdk-state-err "User is not set to 'Ready'.")
                     (not (state/interaction-exists-in-state? :active interactionId)) (err/invalid-sdk-state-err "No such active Interaction.")
                     :else false)]
      error
      (let [module-chan (state/get-module-chan :interactions)
            transfer-body (merge (iu/base-module-request
                                    :INTERACTIONS/SEND_INTERRUPT
                                    (merge {:tenantId (state/get-active-tenant-id)
                                            :interruptType "customer-transfer"
                                            :source "client"
                                            :interactionId interactionId}
                                           (when (not (nil? resourceId))
                                              {:interrupt {:resource-id (state/get-active-user-id)
                                                           :transfer-resource-id resourceId
                                                           :transfer-type interrupt-type}})
                                           (when (not (nil? queueId))
                                              {:interrupt {:resource-id (state/get-active-user-id)
                                                           :transfer-queue-id queueId
                                                           :transfer-type interrupt-type}}))))]

        (go (let [transfer-response (a/<! (mg/send-module-message transfer-body))]
              (sdk-response pubsub-topic transfer-response callback))))))))
