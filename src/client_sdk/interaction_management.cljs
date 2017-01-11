(ns client-sdk.interaction-management
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [client-sdk.state :as state]
            [client-sdk.api.interactions :as int]
            [client-sdk.pubsub :as pubsub]) )

(defn handle-work-offer [message]
  (state/add-interaction! :pending message)
  (let [{:keys [channelType interactionId]} message]

    (when (= channelType "sms")
      (let [history-result-chan (a/promise-chan)
            history-req (-> message
                            (select-keys [:tenantId :interactionId])
                            (merge {:token (state/get-token)
                                    :resp-chan history-result-chan
                                    :type :MESSAGING/GET_HISTORY}))]
        (a/put! (state/get-module-chan :messaging) history-req)
        (go (let [history (a/<! history-result-chan)]
              (state/add-messages-to-history! interactionId history)))))))

(defn handle-resource-state-change [message])

(defn handle-work-initiated [message]
  (log :error "work initiated message:" message)
  (pubsub/publish! "cxengage/interactions/work-offer-received" message))

(defn handle-work-rejected [message])

(defn handle-custom-fields [message]
  (let [{:keys [interactionId]} message
        custom-field-details (:customFields message)]
    (state/add-interaction-custom-field-details! custom-field-details interactionId)))

(defn handle-disposition-codes [message]
  (let [{:keys [interactionId]} message
        disposition-code-details (:dispositionCodes message)]
    (state/add-interaction-disposition-code-details! disposition-code-details interactionId)))

(defn handle-session-start [message])

(defn handle-login [message])

(defn handle-interaction-heartbeat [message])

(defn handle-work-accepted [message]
  (let [{:keys [interactionId tenantId]} message]
    (state/transition-interaction! :pending :active interactionId)
    (a/put! (state/get-module-chan :mqtt) {:type :MQTT/SUBSCRIBE_TO_INTERACTION
                                           :tenantId tenantId
                                           :interactionId interactionId})))

(defn handle-wrapup [message]
  (let [wrapup-details (select-keys message [:wrapupTime :wrapupEnabled :wrapupUpdateAllowed :targetWrapupTime])
        {:keys [interactionId]} message]
    (state/add-interaction-wrapup-details! wrapup-details interactionId)))

(defn msg-router [message]
  #_(log :debug "MESSAGE ROUTER GOT:" message)
  (let [handling-fn (case (:msg-type message)
                      :INTERACTIONS/WORK_ACCEPTED_RECEIVED handle-work-accepted
                      :INTERACTIONS/WORK_OFFER_RECEIVED handle-work-offer
                      :INTERACTIONS/WORK_REJECTED_RECEIVED handle-work-rejected
                      :INTERACTIONS/WORK_INITIATED_RECEIVED handle-work-initiated
                      :INTERACTIONS/CUSTOM_FIELDS_RECEIVED handle-custom-fields
                      :INTERACTIONS/DISPOSITION_CODES_RECEIVED handle-disposition-codes
                      :INTERACTIONS/INTERACTION_TIMEOUT_RECEIVED handle-interaction-heartbeat
                      :INTERACTIONS/WRAP_UP_RECEIVED handle-wrapup
                      :SESSION/CHANGE_STATE_RESPONSE handle-resource-state-change
                      :SESSION/START_SESSION_RESPONSE handle-session-start
                      :AUTH/LOGIN_RESPONSE handle-login
                      nil)]
    (when (and (get message :actionId)
               (not= (get message :interactionId) "00000000-0000-0000-0000-000000000000"))
      (let [ack-msg (select-keys message [:actionId :subId :resourceId :tenantId :interactionId])]
        (log :info (str "Acknowledging receipt of flow action: " (or (:notificationType message) (:type message))))
        (when (or (:notificationType message) (:type message))
          (int/acknowledge-flow-action ack-msg))))
    (if handling-fn
      (handling-fn message)
      (log :debug (str "Temporarily ignoring msg 'cuz handler isnt written yet: " (:msg-type message)) message))))

(defn infer-notification-type [message]
  (let [{:keys [notificationType]} message]
    (if-let [inferred-notification-type (case notificationType
                                          "work-rejected" (merge {:msg-type :INTERACTIONS/WORK_REJECTED_RECEIVED} message)
                                          "work-initiated" (merge {:msg-type :INTERACTIONS/WORK_INITIATED_RECEIVED} message)
                                          "work-accepted" (merge {:msg-type :INTERACTIONS/WORK_ACCEPTED_RECEIVED} message)
                                          "disposition-codes" (merge {:msg-type :INTERACTIONS/DISPOSITION_CODES_RECEIVED} message)
                                          "custom-fields" (merge {:msg-type :INTERACTIONS/CUSTOM_FIELDS_RECEIVED} message)
                                          "wrapup" (merge {:msg-type :INTERACTIONS/WRAP_UP_RECEIVED} message)
                                          "interaction-timeout" (merge {:msg-type :INTERACTIONS/INTERACTION_TIMEOUT_RECEIVED} message)
                                          nil)]
      inferred-notification-type
      (log :warn "Unable to infer agent notification msg type - no matching clause" message))))

(defn sqs-msg-router [message]
  (let [cljsd-msg (js->clj (js/JSON.parse message) :keywordize-keys true)
        sessionId (or (get cljsd-msg :sessionId)
                      (get-in cljsd-msg [:resource :sessionId]))
        inferred-msg (case (:type cljsd-msg)
                       "resource-state-change" (merge {:msg-type :SESSION/CHANGE_STATE_RESPONSE} cljsd-msg)
                       "work-offer" (merge {:msg-type :INTERACTIONS/WORK_OFFER_RECEIVED} cljsd-msg)
                       "agent-notification" (infer-notification-type cljsd-msg)
                       nil)]
    (when (nil? (state/get-session-id))
      (log :error "Local Session ID so our session hasn't started yet :("))
    (if (not= (state/get-session-id) sessionId)
      (log :warn (str "Received a message from a different session than the current one. Current session ID: " (state/get-session-id) " - Session ID on message received: " sessionId " - Message:") cljsd-msg)
      (if inferred-msg
        (msg-router inferred-msg)
        (log :error "Unable to infer msg type from sqs")))))

(defn mqtt-msg-router [message]
  (log :warn "message in mqtt msg router" message))