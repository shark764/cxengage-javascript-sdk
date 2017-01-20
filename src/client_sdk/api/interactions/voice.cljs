(ns client-sdk.api.interactions.voice
  (:require-macros [lumbajack.macros :refer [log]]
                   [cljs.core.async.macros :refer [go]])
  (:require [client-sdk.state :as state]
            [cljs.core.async :as a]
            [client-sdk.internal-utils :as iu]
            [cljs.spec :as s]
            [client-sdk.pubsub :refer [sdk-response sdk-error-response]]
            [client-sdk.domain.specs :as specs]
            [client-sdk.domain.errors :as err]
            [client-sdk.module-gateway :as mg]))

(s/def ::voice-features-params
    (s/keys :req-un [::specs/interactionId]
            :opt-un [::specs/callback]))

(defn auxiliary-features
  ([interrupt-type params callback]
   (auxiliary-features interrupt-type (merge (iu/extract-params params) {:callback callback})))
  ([interrupt-type params]
   (let [params (iu/extract-params params)
         pubsub-topic (str "cxengage/voice/" interrupt-type)
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
                                     :resourceId (state/get-active-user-id)
                                     :interactionId interactionId}))]
        (go (let [voice-tool-response (a/<! (mg/send-module-message voice-tool-body))]
              (sdk-response pubsub-topic voice-tool-response callback))))))))
