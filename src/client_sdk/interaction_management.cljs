(ns client-sdk.interaction-management
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [client-sdk.state :as state]
            [client-sdk.api.interactions :as int]
            [client-sdk.module-gateway :as mg]
            [client-sdk.internal-utils :as iu]
            [client-sdk.pubsub :as pubsub :refer [sdk-response sdk-error-response]]))

(defn handle-work-offer [message]
  (state/add-interaction! :pending message)
  (let [{:keys [channelType interactionId]} message]
    (when (or (= channelType "messaging") (= channelType "sms"))
      (let [history-result-chan (a/promise-chan)
            history-req (-> message
                            (select-keys [:tenantId :interactionId])
                            (merge {:token (state/get-token)
                                    :resp-chan history-result-chan
                                    :type :MESSAGING/GET_HISTORY}))]
        (a/put! (state/get-module-chan :messaging) history-req)
        (go (let [history (a/<! history-result-chan)]
              (sdk-response "cxengage/messaging/history" history)
              (state/add-messages-to-history! interactionId history))))))
  (sdk-response "cxengage/interactions/work-offer" message))

(defn handle-new-messaging-message [payload]
  (let [payload (-> (.-payloadString payload)
                    (js/JSON.parse)
                    (js->clj :keywordize-keys true))
        interactionId (:to payload)
        channelId (:id payload)]
    (sdk-response "cxengage/messaging/new-message-received" payload)
    (state/add-messages-to-history! interactionId [{:payload payload}])))

(defn handle-resource-state-change [message]
  ;; TODO: update our internal state
  (sdk-response "cxengage/session/state-changed" (select-keys message [:state :availableStates :direction])))

(defn handle-work-initiated [message]
  nil)

(defn handle-work-rejected [message]
  (let [{:keys [interactionId]} message]
    (state/transition-interaction! :pending :past interactionId)
    (sdk-response "cxengage/interactions/work-rejected" {:interactionId interactionId})))

(defn handle-custom-fields [message]
  (let [{:keys [interactionId]} message
        custom-field-details (:customFields message)]
    (state/add-interaction-custom-field-details! custom-field-details interactionId)))

(defn handle-disposition-codes [message]
  (let [{:keys [interactionId]} message
        disposition-code-details (:dispositionCodes message)]
    (state/add-interaction-disposition-code-details! disposition-code-details interactionId)))

(defn handle-session-start [message]
  nil)

(defn handle-login [message]
  nil)

(defn handle-interaction-heartbeat [message]
  nil)

(defn handle-work-accepted [message]
  (let [{:keys [interactionId tenantId]} message
        interaction (state/get-pending-interaction interactionId)
        channel-type (get interaction :channelType)]
    (state/transition-interaction! :pending :active interactionId)
    (when (or (= channel-type "sms")
              (= channel-type "messaging"))
      (a/put! (state/get-module-chan :mqtt) {:type :MQTT/SUBSCRIBE_TO_INTERACTION
                                             :tenantId tenantId
                                             :interactionId interactionId}))))
    (sdk-response "cxengage/interactions/work-accepted" {:interactionId interactionId})

(defn handle-work-ended [message]
  (let [{:keys [interactionId]} message]
    (state/transition-interaction! :active :past interactionId)
    (sdk-response "cxengage/interactions/work-ended" {:interactionId interactionId})))

(defn handle-wrapup [message]
  (let [wrapup-details (select-keys message [:wrapupTime :wrapupEnabled :wrapupUpdateAllowed :targetWrapupTime])
        {:keys [interactionId]} message]
    (state/add-interaction-wrapup-details! wrapup-details interactionId)))

(defn handle-generic [message]
  nil)

(defn handle-screen-pop [message]
  nil)

(defn msg-router [message]
  (let [handling-fn (case (:msg-type message)
                      :INTERACTIONS/WORK_ACCEPTED_RECEIVED handle-work-accepted
                      :INTERACTIONS/WORK_OFFER_RECEIVED handle-work-offer
                      :INTERACTIONS/WORK_REJECTED_RECEIVED handle-work-rejected
                      :INTERACTIONS/WORK_INITIATED_RECEIVED handle-work-initiated
                      :INTERACTIONS/WORK_ENDED_RECEIVED handle-work-ended
                      :INTERACTIONS/CUSTOM_FIELDS_RECEIVED handle-custom-fields
                      :INTERACTIONS/DISPOSITION_CODES_RECEIVED handle-disposition-codes
                      :INTERACTIONS/INTERACTION_TIMEOUT_RECEIVED handle-interaction-heartbeat
                      :INTERACTIONS/WRAP_UP_RECEIVED handle-wrapup
                      :INTERACTIONS/SCREEN_POP_RECEIVED handle-screen-pop
                      :INTERACTIONS/GENERIC_AGENT_NOTIFICATION handle-generic
                      :SESSION/CHANGE_STATE_RESPONSE handle-resource-state-change
                      :SESSION/START_SESSION_RESPONSE handle-session-start
                      :AUTH/LOGIN_RESPONSE handle-login
                      nil)]
    (when (and (get message :actionId)
               (not= (get message :interactionId) "00000000-0000-0000-0000-000000000000"))
      (let [ack-msg (select-keys message [:actionId :subId :resourceId :tenantId :interactionId])]
        (log :debug (str "Acknowledging receipt of flow action: " (or (:notificationType message) (:type message))))
        (when (or (:notificationType message) (:type message))
          (mg/send-module-message (iu/base-module-request
                                   :INTERACTIONS/ACKNOWLEDGE_FLOW_ACTION
                                   ack-msg)))))
    (if handling-fn
      (handling-fn message)
      (log :debug (str "Temporarily ignoring msg 'cuz handler isnt written yet: " (:msg-type message)) message))
    nil))

(defn infer-notification-type [message]
  (let [{:keys [notificationType]} message]
    (let [inferred-notification-type (case notificationType
                                       "work-rejected" :INTERACTIONS/WORK_REJECTED_RECEIVED
                                       "work-initiated" :INTERACTIONS/WORK_INITIATED_RECEIVED
                                       "work-ended" :INTERACTIONS/WORK_ENDED_RECEIVED
                                       "work-accepted" :INTERACTIONS/WORK_ACCEPTED_RECEIVED
                                       "disposition-codes" :INTERACTIONS/DISPOSITION_CODES_RECEIVED
                                       "custom-fields" :INTERACTIONS/CUSTOM_FIELDS_RECEIVED
                                       "wrapup" :INTERACTIONS/WRAP_UP_RECEIVED
                                       "interaction-timeout" :INTERACTIONS/INTERACTION_TIMEOUT_RECEIVED
                                       "screen-pop" :INTERACTIONS/SCREEN_POP_RECEIVED
                                       :INTERACTIONS/GENERIC_AGENT_NOTIFICATION)]
      (merge {:msg-type inferred-notification-type} message))))

(defn sqs-msg-router [message]
  (let [cljsd-msg (js->clj (js/JSON.parse message) :keywordize-keys true)
        sessionId (or (get cljsd-msg :sessionId)
                      (get-in cljsd-msg [:resource :sessionId]))
        inferred-msg (case (:type cljsd-msg)
                       "resource-state-change" (merge {:msg-type :SESSION/CHANGE_STATE_RESPONSE} cljsd-msg)
                       "work-offer" (merge {:msg-type :INTERACTIONS/WORK_OFFER_RECEIVED} cljsd-msg)
                       "agent-notification" (infer-notification-type cljsd-msg)
                       nil)]
    (if (not= (state/get-session-id) sessionId)
      (do (log :warn (str "Received a message from a different session than the current one. Current session ID: "
                          (state/get-session-id) " - Session ID on message received: " sessionId " - Message:") cljsd-msg)
          nil)
      (if inferred-msg
        (msg-router inferred-msg)
        (do (log :warn "Unable to infer message type from sqs")
            nil)))))

(defn mqtt-msg-router [message]
  (handle-new-messaging-message message))

(defn twilio-msg-router [message type]
  nil
  #_(log :warn "message in twilio msg router" message))
