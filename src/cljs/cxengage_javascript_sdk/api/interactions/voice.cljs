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
       (sdk-error-response pubsub-topic error callback)
       (let [voice-tool-body (iu/base-module-request
                              :INTERACTIONS/SEND_INTERRUPT
                              {:tenantId (state/get-active-tenant-id)
                               :interruptType interrupt-type
                               :source "client"
                               :interrupt {:resourceId (state/get-active-user-id)}
                               :interactionId interactionId})]
         (go (let [voice-tool-response (a/<! (mg/send-module-message voice-tool-body))]
               (sdk-response pubsub-topic voice-tool-response callback)
               nil)))))))

(s/def ::cancel-transfer-params
  (s/keys :req-un [::specs/interactionId]
          :opt-un [::specs/callback]))

(defn cancel-transfer
  ([params callback]
   (cancel-transfer (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         {:keys [interactionId callback]} params
         pubsub-topic (str "cxengage/voice/cancel-transfer-response")
         interrupt {:resourceId (state/get-active-user-id)}]
     (if-let [error (cond
                      (not (s/valid? ::cancel-transfer-params params)) (err/invalid-params-err)
                      (not (state/interaction-exists-in-state? :active interactionId)) (err/invalid-sdk-state-err "No such active Interaction.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [transfer-request (iu/base-module-request
                               :INTERACTIONS/SEND_INTERRUPT
                               {:tenantId (state/get-active-tenant-id)
                                :interruptType "transfer-cancel"
                                :source "client"
                                :interactionId interactionId
                                :interrupt interrupt})]
         (go (let [transfer-response (a/<! (mg/send-module-message transfer-request))]
               (sdk-response pubsub-topic transfer-response callback)
               nil)))))))

(s/def ::transferType #{"cold" "warm"})
(s/def ::interruptBody map?)
(s/def ::cancel? boolean?)
(s/def ::voice-transfer-params
  (s/keys :req-un [::transferType :specs/interactionId ::interruptBody]
          :opt-un [:specs/callback ::cancel?]))

(defn transfer-impl*
  ([params callback]
   (transfer-impl* (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         {:keys [transferType interruptBody interactionId callback]} params
         pubsub-topic (str "cxengage/voice/transfer-response")
         transfer-climate (if (= "warm" transferType)
                            "warm-transfer"
                            "cold-transfer")
         interrupt (merge interruptBody
                    {:resourceId (state/get-active-user-id)
                     :transferType transfer-climate})]
     (if-let [error (cond
                      (not (s/valid? ::voice-transfer-params params)) (err/invalid-params-err)
                      (not (state/interaction-exists-in-state? :active interactionId)) (err/invalid-sdk-state-err "No such active Interaction.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [transfer-request (iu/base-module-request
                               :INTERACTIONS/SEND_INTERRUPT
                               {:tenantId (state/get-active-tenant-id)
                                :interruptType "customer-transfer"
                                :source "client"
                                :interactionId interactionId
                                :interrupt interrupt})]
         (go (let [transfer-response (a/<! (mg/send-module-message transfer-request))]
               (sdk-response pubsub-topic transfer-response callback)
               nil)))))))

(s/def ::transfer-to-resource-params
  (s/keys :req-un [:specs/interactionId :specs/resourceId]
          :opt-un [:specs/callback :specs/callback]))

(defn transfer-to-resource
  ([params callback]
   (transfer-to-resource (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)]
     (if-let [error (cond
                      (not (s/valid? ::transfer-to-resource-params params)) (err/invalid-params-err)
                      :else false)]
       (sdk-error-response "cxengage/voice/transfer-response" error (:callback params))
       (transfer-impl* (-> params
                           (merge {:interruptBody {:transfer-resource-id (:resourceId params)}})
                           (dissoc :resourceId)))))))

(s/def ::transfer-to-queue-params
  (s/keys :req-un [:specs/interactionId :specs/queueId]
          :opt-un [:specs/callback]))

(defn transfer-to-queue
  ([params callback]
   (transfer-to-queue (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)]
     (if-let [error (cond
                      (not (s/valid? ::transfer-to-queue-params params)) (err/invalid-params-err)
                      :else false)]
       (sdk-error-response "cxengage/voice/transfer-response" error (:callback params))
       (transfer-impl* (-> params
                           (merge {:interruptBody {:transfer-queue-id (:queueId params)}})
                           (dissoc :queueId)))))))

(s/def ::value string?)
(s/def ::type string?)
(s/def ::transferExtension
  (s/keys :req-un [::type ::value]))
(s/def ::transfer-to-extension-params
  (s/keys :req-un [:specs/interactionId ::transferExtension]
          :opt-un [:specs/callback]))

(defn transfer-to-extension
  ([params callback]
   (transfer-to-extension (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)]
     (if-let [error (cond
                      (not (s/valid? ::transfer-to-extension-params params)) (err/invalid-params-err)
                      :else false)]
       (sdk-error-response "cxengage/voice/transfer-response" error (:callback params))
       (transfer-impl* (-> params
                           (merge {:interruptBody {:transfer-extension (:transferExtension params)}})
                           (dissoc :extensionId)))))))
