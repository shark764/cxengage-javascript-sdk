(ns cxengage-javascript-sdk.api.interactions
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.module-gateway :as mg]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.pubsub :refer [sdk-response sdk-error-response]]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.errors :as err]))

(s/def ::accept-interaction-params
  (s/keys :req-un [:specs/interactionId]
          :opt-un [:specs/callback]))

(defn accept-interaction
  ([params callback]
   (accept-interaction (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (let [params (iu/extract-params params)
         pubsub-topic "cxengage/interactions/accept-response"
         {:keys [interactionId callback]} params
         interaction (state/get-pending-interaction interactionId)
         {:keys [channelType]} interaction
         interaction-response {:interactionId interactionId}]
     (if-let [error (cond
                      (not (s/valid? ::accept-interaction-params params)) (err/invalid-params-err)
                      (not (state/session-started?)) (err/invalid-sdk-state-err "Your session isn't started yet.")
                      (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Your active tenant isn't set yet.")
                      (not (state/interaction-exists-in-state? :pending interactionId)) (err/invalid-sdk-state-err "No interaction of that ID found.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [send-interrupt-msg (iu/base-module-request
                                 :INTERACTIONS/SEND_INTERRUPT
                                 {:tenantId (state/get-active-tenant-id)
                                  :interruptType "offer-accept"
                                  :source "client"
                                  :interrupt {:resourceId (state/get-active-user-id)}
                                  :interactionId interactionId})]
         (go (let [accept-interaction (a/<! (mg/send-module-message send-interrupt-msg))
                   accept-interaction-response (merge accept-interaction interaction-response)]
               (sdk-response pubsub-topic accept-interaction-response callback)
               (when (= channelType "voice")
                 (let [connection (state/get-twilio-connection)]
                   (.accept connection)))
               (when (or (= channelType "sms")
                         (= channelType "messaging"))
                 (let [history-result-chan (a/promise-chan)
                       history-req (iu/base-module-request
                                    :MESSAGING/GET_HISTORY
                                    {:tenantId (state/get-active-tenant-id)
                                     :interactionId interactionId})]
                   (a/put! (mg/>get-publication-channel) history-req)
                   (go (let [history (a/<! history-result-chan)]
                         (sdk-response "cxengage/messaging/history" (merge history interaction-response))
                         (state/add-messages-to-history! interactionId history)))))
               nil)))))))

(s/def ::end-interaction-params
  (s/keys :req-un [:specs/interactionId]
          :opt-un [:specs/callback]))

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
                      (not (state/interaction-exists-in-state? :active interactionId)) (err/invalid-sdk-state-err "No interaction of that ID found.")
                      :else false)]
       (sdk-error-response pubsub-topic error callback)
       (let [interaction-response {:interactionId interactionId}]
         (when (= channelType "voice")
           (let [device (state/get-twilio-device)]
             (.disconnectAll device))
           (sdk-response pubsub-topic interaction-response callback))
         (when (or (= channelType "sms")
                   (= channelType "messaging"))
           (let [send-interrupt-msg (iu/base-module-request
                                     :INTERACTIONS/SEND_INTERRUPT
                                     {:tenantId (state/get-active-tenant-id)
                                      :interruptType "resource-disconnect"
                                      :source "client"
                                      :interrupt {:resourceId (state/get-active-user-id)}
                                      :interactionId interactionId})]
             (go (let [{:keys [status] :as end-interaction-response} (a/<! (mg/send-module-message send-interrupt-msg))]
                   (if (not= status 200)
                     (let [error (err/sdk-request-error "Send interrupt failed")]
                       (do (sdk-error-response "cxengage/errors/error" error callback)
                           (sdk-error-response pubsub-topic error callback)))
                     (do (sdk-response pubsub-topic interaction-response callback)
                         nil)))))))))))

(s/def ::wrapup-params
  (s/keys ::req-un [:specs/interactionId
                    :specs/wrapup]
          ::opt-un [:specs/callback]))

(defn wrapup-impl*
  [params]
  (let [params (iu/extract-params params)
        {:keys [interactionId callback wrapup]} params
        pubsub-topic (str "cxengage/interactions/" wrapup)]
    (if-let [error (cond
                     (not (s/valid? ::wrapup-params params)) (err/invalid-params-err)
                     (not (state/session-started?)) (err/invalid-sdk-state-err "Your session isn't started yet.")
                     (not (state/active-tenant-set?)) (err/invalid-sdk-state-err "Your active tenant isn't set yet.")
                     (not (state/presence-state-matches? "ready")) (err/invalid-sdk-state-err "You must be in a 'ready' state.")
                     (not (state/interaction-exists-in-state? :active interactionId)) (err/invalid-sdk-state-err "No interaction of that ID found.")
                     :else false)]
      (sdk-error-response pubsub-topic error callback)
      (let [send-interrupt-msg (iu/base-module-request
                                :INTERACTIONS/SEND_INTERRUPT
                                {:tenantId (state/get-active-tenant-id)
                                 :interruptType wrapup
                                 :source "client"
                                 :interrupt {:resourceId (state/get-active-user-id)}
                                 :interactionId interactionId})]
        (go (let [{:keys [result status] :as wrapup-response} (a/<! (mg/send-module-message send-interrupt-msg))]
              (if (not= status 200)
                (let [error (err/sdk-request-error (str "Failed to " wrapup))]
                  (do (sdk-error-response "cxengage/errors/error" error callback)
                      (sdk-error-response pubsub-topic error callback)))
                (do (let [wu (if (= wrapup "wrapup-on") true false)
                          wrapup-state {:wrapupEnabled wu}
                          pub-response {:interactionId interactionId}]
                      (state/add-interaction-wrapup-details! wrapup-state interactionId)
                      (sdk-response pubsub-topic pub-response callback))
                    nil))))))))

(defn enable-wrapup
  ([params callback]
   (enable-wrapup (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (wrapup-impl* (merge (iu/extract-params params) {:wrapup "wrapup-on"}))))

(defn disable-wrapup
  ([params callback]
   (disable-wrapup (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (wrapup-impl* (merge (iu/extract-params params) {:wrapup "wrapup-off"}))))

(defn end-wrapup
  ([params callback]
   (end-wrapup (merge (iu/extract-params params) {:callback callback})))
  ([params]
   (wrapup-impl* (merge (iu/extract-params params) {:wrapup "wrapup-end"}))))
