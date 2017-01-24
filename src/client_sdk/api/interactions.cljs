(ns client-sdk.api.interactions
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [client-sdk.internal-utils :as iu]
            [client-sdk.module-gateway :as mg]
            [client-sdk.state :as state]
            [client-sdk.pubsub :refer [sdk-response sdk-error-response]]
            [client-sdk.domain.specs :as specs]
            [client-sdk.domain.errors :as err]))

(s/def ::accept-interaction-params
  (s/keys :req-un [::specs/interactionId]
          :opt-un [::specs/callback]))

(defn accept-interaction
  ([params callback]
   (accept-interaction (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/interactions/accept-response"
         {:keys [interactionId callback]} params
         interaction (state/get-pending-interaction interactionId)
         {:keys [channelType]} interaction]
     (if-let [error (cond
                      (not (s/valid? ::accept-interaction-params params)) (err/invalid-params-err)
                      (not (state/session-started?)) (err/invalid-sdk-state-err "Your session isn't started yet.")
                      (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Your active tenant isn't set yet.")
                      (not (state/presence-state-matches? "ready")) (err/invalid-sdk-state-err "You must be in a 'ready' state.")
                      (not (state/interaction-exists-in-state? :pending interactionId)) (err/invalid-sdk-state-err "No interaction of that ID found.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [send-interrupt-msg (iu/base-module-request
                                 :INTERACTIONS/SEND_INTERRUPT
                                 {:tenantId (state/get-active-tenant-id)
                                  :interruptType "offer-accept"
                                  :source "client"
                                  :resourceId (state/get-active-user-id)
                                  :interactionId interactionId})]
         (go (let [accept-interaction-response (a/<! (mg/send-module-message send-interrupt-msg))]
               (sdk-response pubsub-topic accept-interaction-response callback)
               (when (= channelType "voice")
                (let [connection (state/get-twilio-connection)]
                  (.accept connection)))
               nil)))))))

(s/def ::end-interaction-params
  (s/keys :req-un [::specs/interactionId]
          :opt-un [::specs/callback]))

(defn end-interaction
  ([params callback]
   (end-interaction (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/interactions/end-response"
         {:keys [interactionId callback]} params
         interaction (state/get-active-interaction interactionId)
         {:keys [channelType]} interaction]
     (if-let [error (cond
                      (not (s/valid? ::end-interaction-params params)) (err/invalid-params-err)
                      (not (state/session-started?)) (err/invalid-sdk-state-err "Your session isn't started yet.")
                      (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Your active tenant isn't set yet.")
                      (not (state/presence-state-matches? "ready")) (err/invalid-sdk-state-err "You must be in a 'ready' state.")
                      (not (state/interaction-exists-in-state? :active interactionId)) (err/invalid-sdk-state-err "No interaction of that ID found.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [send-interrupt-msg (iu/base-module-request
                                 :INTERACTIONS/SEND_INTERRUPT
                                 {:tenantId (state/get-active-tenant-id)
                                  :interruptType "resource-disconnect"
                                  :source "client"
                                  :resourceId (state/get-active-user-id)
                                  :interactionId interactionId})]
         (go (let [end-interaction-response (a/<! (mg/send-module-message send-interrupt-msg))]
               (sdk-response pubsub-topic end-interaction-response callback)
               (when (= channelType "voice")
                (let [device (state/get-twilio-device)]
                  (.disconnectAll device)))
               nil)))))))
