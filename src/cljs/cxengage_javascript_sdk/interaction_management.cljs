(ns cxengage-javascript-sdk.interaction-management
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.errors :as err]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.modules.messaging :as messaging]))

;; TODO: make these better? in a module

(defn get-messaging-history [tenant-id interaction-id]
  (let [history-request {:method :get
                         :url (str (state/get-base-api-url) "messaging/tenants/" tenant-id "/channels/" interaction-id "/history")}]
    (go (let [history-response (a/<! (iu/api-request history-request))
              {:keys [api-response status]} history-response
              {:keys [result]} api-response]
          (if (not= status 200)
            (e/api-error result)
            (do (state/add-messages-to-history! interaction-id result)
                (p/publish {:topics (p/get-topic :messaging-history-received)
                            :response (state/get-interaction-messaging-history interaction-id)})))))))

(defn get-messaging-metadata [tenant-id interaction-id]
  (let [metadata-request {:method :get
                          :url (str (state/get-base-api-url) "messaging/tenants/" tenant-id "/channels/" interaction-id)}]
    (go (let [metadata-response (a/<! (iu/api-request metadata-request))
              {:keys [api-response status]} metadata-response
              {:keys [result]} api-response]
          (if (not= status 200)
            (e/api-error result)
            (do (state/add-messaging-interaction-metadata! result)
                (get-messaging-history tenant-id interaction-id)))))))

(defn handle-work-offer [message]
  (state/add-interaction! :pending message)
  (let [{:keys [channel-type interaction-id]} message]
    (when (or (= channel-type "sms")
              (= channel-type "messaging"))
      (let [{:keys [tenant-id interaction-id]} message]
        (get-messaging-metadata tenant-id interaction-id))))
  (p/publish {:topics (p/get-topic :work-offer-received)
              :response message})
  nil)

(defn handle-new-messaging-message [payload]
  (let [payload (-> (.-payloadString payload)
                    (js/JSON.parse)
                    (iu/kebabify))
        interaction-id (:to payload)
        channel-id (:id payload)
        from (:from payload)]
    (p/publish {:topics (p/get-topic :new-message-received)
                :response (:payload (state/augment-messaging-payload {:payload payload}))})
    (state/add-messages-to-history! interaction-id [{:payload payload}])))

(defn handle-resource-state-change [message]
  (state/set-user-session-state! message)
  (if (not= (:state message) "offline")
    (p/publish {:topics (p/get-topic :presence-state-changed)
                :response (select-keys message [:state :available-states :direction])})
    (p/publish {:topics (p/get-topic :session-ended)
                :response true})))

(defn handle-work-initiated [message]
  (p/publish {:topics (p/get-topic :work-initiated-received)
              :response message}))

(defn handle-work-rejected [message]
  (let [{:keys [interaction-id]} message]
    (state/transition-interaction! :pending :past interaction-id)
    (p/publish {:topics (p/get-topic :work-rejected-received)
                :response {:interaction-id interaction-id}})))

(defn handle-custom-fields [message]
  (let [{:keys [interaction-id]} message
        custom-field-details (:custom-fields message)]
    (state/add-interaction-custom-field-details! custom-field-details interaction-id)
    (p/publish {:topics (p/get-topic :custom-fields-received)
                :response custom-field-details})))

(defn handle-disposition-codes [message]
  (let [{:keys [interaction-id]} message
        disposition-code-details (:disposition-codes message)]
    (state/add-interaction-disposition-code-details! disposition-code-details interaction-id)
    (p/publish {:topics (p/get-topic :disposition-codes-received)
                :response disposition-code-details})))

(defn handle-session-start [message]
  nil)

(defn handle-interaction-heartbeat [message]
  nil)

(defn handle-work-accepted [message]
  (let [{:keys [interaction-id tenant-id]} message
        interaction (state/get-pending-interaction interaction-id)
        channel-type (get interaction :channel-type)]
    (state/transition-interaction! :pending :active interaction-id)
    (when (or (= channel-type "sms")
              (= channel-type "messaging"))
      (messaging/subscribe-to-messaging-interaction
       {:tenant-id tenant-id
        :interaction-id interaction-id
        :env (state/get-env)}))
    (p/publish {:topics (p/get-topic :work-accepted-received)
                :response {:interaction-id interaction-id}}) ))

(defn handle-work-ended [message]
  (let [{:keys [interaction-id]} message
        interaction (state/get-interaction interaction-id)
        channel-type (get interaction :channel-type)]
    (when (= channel-type "voice")
      (let [connection (state/get-twilio-device)]
        (.disconnectAll connection)))
    (state/transition-interaction! :active :past interaction-id)
    (p/publish {:topics (p/get-topic :work-ended-received)
                :response {:interaction-id interaction-id}})))

(defn handle-customer-hold [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (p/get-topic :customer-hold)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-customer-resume [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (p/get-topic :customer-resume)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-resource-mute [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (p/get-topic :resource-muted)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-resource-unmute [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (p/get-topic :resource-unmuted)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-recording-start [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (p/get-topic :recording-started)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-recording-stop [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (p/get-topic :recording-ended)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-transfer-connected [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (p/get-topic :transfer-connected)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-script-received [message]
  (let [{:keys [interaction-id resource-id script]} message]
    (state/add-script-to-interaction! interaction-id script)
    (p/publish {:topics (p/get-topic :script-received)
                :response {:interaction-id interaction-id
                           :resource-id resource-id
                           :script script}})))
(defn handle-generic [message]
  nil)

(defn handle-screen-pop [message]
  (let [{:keys [pop-uri pop-type interaction-id]} message]
    (when (and pop-uri (or (= pop-type "external-url") (= pop-type "url")))
      (p/publish {:topics (p/get-topic :url-pop-received)
                  :response {:interaction-id interaction-id
                             :pop-uri pop-uri}}))))

(defn handle-wrapup [message]
  (let [wrapup-details (select-keys message [:wrapup-time :wrapup-enabled :wrapup-update-allowed :target-wrapup-time])
        {:keys [interaction-id]} message]
    (do (state/add-interaction-wrapup-details! wrapup-details interaction-id)
        (p/publish {:topics (p/get-topic :wrapup-details-received)
                    :response (assoc wrapup-details :interaction-id interaction-id)}))))

(defn handle-wrapup-started
  [message]
  (let [{:keys [interaction-id]} message
        wrapup-details (state/get-interaction-wrapup-details interaction-id)]
    (p/publish {:topics (p/get-topic :wrapup-started)
                :response {:interaction-id interaction-id}})))

(defn handle-wrapup-ended
  [message]
  (let [{:keys [interaction-id]} message
        wrapup-details (state/get-interaction-wrapup-details interaction-id)]
    (p/publish {:topics (p/get-topic :wrapup-ended)
                :response {:interaction-id interaction-id}})))

(defn msg-router [message]
  (let [handling-fn (case (:sdk-msg-type message)
                      :INTERACTIONS/WORK_ACCEPTED_RECEIVED handle-work-accepted
                      :INTERACTIONS/WORK_OFFER_RECEIVED handle-work-offer
                      :INTERACTIONS/WORK_REJECTED_RECEIVED handle-work-rejected
                      :INTERACTIONS/WORK_INITIATED_RECEIVED handle-work-initiated
                      :INTERACTIONS/WORK_ENDED_RECEIVED handle-work-ended
                      :INTERACTIONS/CUSTOM_FIELDS_RECEIVED handle-custom-fields
                      :INTERACTIONS/DISPOSITION_CODES_RECEIVED handle-disposition-codes
                      :INTERACTIONS/INTERACTION_TIMEOUT_RECEIVED handle-interaction-heartbeat
                      :INTERACTIONS/WRAP_UP_RECEIVED handle-wrapup
                      :INTERACTIONS/WRAP_UP_STARTED handle-wrapup-started
                      :INTERACTIONS/SCREEN_POP_RECEIVED handle-screen-pop
                      :INTERACTIONS/GENERIC_AGENT_NOTIFICATION handle-generic
                      :INTERACTIONS/CUSTOMER_HOLD_RECEIVED handle-customer-hold
                      :INTERACTIONS/CUSTOMER_RESUME_RECEIVED handle-customer-resume
                      :INTERACTIONS/RESOURCE_MUTE_RECEIVED handle-resource-mute
                      :INTERACTIONS/RESOURCE_UNMUTE_RECEIVED handle-resource-unmute
                      :INTERACTIONS/RECORDING_START_RECEIVED handle-recording-start
                      :INTERACTIONS/RECORDING_STOP_RECEIVED handle-recording-stop
                      :INTERACTIONS/TRANSFER_CONNECTED_RECEIVED handle-transfer-connected
                      :INTERACTIONS/SCRIPT_RECEIVED handle-script-received
                      :SESSION/CHANGE_STATE_RESPONSE handle-resource-state-change
                      :SESSION/START_SESSION_RESPONSE handle-session-start
                      nil)]
    (when (and (get message :action-id)
               (not= (get message :interaction-id) "00000000-0000-0000-0000-000000000000"))
      (log :debug (str "Acknowledging receipt of flow action: "
                           (or (:notification-type message) (:type message))))
      (when (or (:notification-type message) (:type message))
        (let [{:keys [action-id sub-id resource-id tenant-id interaction-id]} message
              ack-request {:method :post
                           :body {:source "client"
                                  :sub-id sub-id
                                  :update {:resource-id resource-id}}
                           :url (str (state/get-base-api-url) "tenants/" tenant-id
                                     "/interactions/" interaction-id "/actions/" action-id)}]
          (go (let [ack-response (a/<! (iu/api-request ack-request))
                    {:keys [api-response status]} ack-response]
                (when (not= status 200)
                  (log :error "Failed to acknowledge flow action")))))))
    (if handling-fn
      (handling-fn message)
      (log :warn (str "Ignoring flow message:" (:sdk-msg-type message)) message))
    nil))

(defn infer-notification-type [message]
  (let [{:keys [notification-type]} message]
    (let [inferred-notification-type (case notification-type
                                       "work-rejected" :INTERACTIONS/WORK_REJECTED_RECEIVED
                                       "work-initiated" :INTERACTIONS/WORK_INITIATED_RECEIVED
                                       "work-ended" :INTERACTIONS/WORK_ENDED_RECEIVED
                                       "work-accepted" :INTERACTIONS/WORK_ACCEPTED_RECEIVED
                                       "disposition-codes" :INTERACTIONS/DISPOSITION_CODES_RECEIVED
                                       "custom-fields" :INTERACTIONS/CUSTOM_FIELDS_RECEIVED
                                       "wrapup" :INTERACTIONS/WRAP_UP_RECEIVED
                                       "wrapup-start" :INTERACTIONS/WRAP_UP_STARTED
                                       "interaction-timeout" :INTERACTIONS/INTERACTION_TIMEOUT_RECEIVED
                                       "screen-pop" :INTERACTIONS/SCREEN_POP_RECEIVED
                                       "customer-hold" :INTERACTIONS/CUSTOMER_HOLD_RECEIVED
                                       "customer-resume" :INTERACTIONS/CUSTOMER_RESUME_RECEIVED
                                       "resource-mute" :INTERACTIONS/RESOURCE_MUTE_RECEIVED
                                       "resource-unmute" :INTERACTIONS/RESOURCE_UNMUTE_RECEIVED
                                       "recording-start" :INTERACTIONS/RECORDING_START_RECEIVED
                                       "recording-stop" :INTERACTIONS/RECORDING_STOP_RECEIVED
                                       "transfer-connected" :INTERACTIONS/TRANSFER_CONNECTED_RECEIVED
                                       :INTERACTIONS/GENERIC_AGENT_NOTIFICATION)]
      (merge {:sdk-msg-type inferred-notification-type} message))))

(defn sqs-msg-router [message]
  (when (state/get-blast-sqs-output)
        (log :warn "[BLAST SQS OUTPUT] Message received:" (iu/camelify message)))
  (let [cljsd-msg (iu/kebabify message)
        session-id (or (get cljsd-msg :session-id)
                       (get-in cljsd-msg [:resource :session-id]))
        inferred-msg (case (:type cljsd-msg)
                       "resource-state-change" (merge {:sdk-msg-type :SESSION/CHANGE_STATE_RESPONSE} cljsd-msg)
                       "work-offer" (merge {:sdk-msg-type :INTERACTIONS/WORK_OFFER_RECEIVED} cljsd-msg)
                       "send-script" (merge {:sdk-msg-type :INTERACTIONS/SCRIPT_RECEIVED} cljsd-msg)
                       "agent-notification" (infer-notification-type cljsd-msg)
                       nil)]
    (if (not= (state/get-session-id) session-id)
      (do (log :warn (str "Received a message from a different session than the current one. Current session ID: "
                                (state/get-session-id) " - Session ID on message received: " session-id))
          nil)
      (if inferred-msg
        (msg-router inferred-msg)
        (do (log :warn "Unable to infer message type from sqs")
            nil)))))

(defn messaging-msg-router [message]
  (handle-new-messaging-message message))

(defn twilio-msg-router [message type]
  (log :warn "message in twilio msg router" message))
