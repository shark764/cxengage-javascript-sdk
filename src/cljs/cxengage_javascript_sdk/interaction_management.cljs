(ns cxengage-javascript-sdk.interaction-management
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cxengage-javascript-sdk.domain.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [clojure.string :refer [starts-with? lower-case]]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.modules.messaging :as messaging]))

(defn get-smooch-history [interaction-id]
  (go (let [history-response (a/<! (rest/get-smooch-interaction-history-request interaction-id))
            {:keys [api-response status]} history-response]
        (if (not= status 200)
          (p/publish {:topics (topics/get-topic :smooch-history-received)
                      :error (e/failed-to-retrieve-smooch-messaging-history-err interaction-id history-response)})
          (p/publish {:topics (topics/get-topic :smooch-history-received)
                      :response api-response})))))

(defn get-messaging-history [interaction-id]
  (go (let [history-response (a/<! (rest/get-messaging-interaction-history-request interaction-id))
            {:keys [api-response status]} history-response
            {:keys [result]} api-response]
        (if (not= status 200)
          (p/publish {:topics (topics/get-topic :failed-to-retrieve-messaging-history)
                      :error (e/failed-to-retrieve-messaging-history-err interaction-id history-response)})
          (do (state/add-messages-to-history! interaction-id result)
              (p/publish {:topics (topics/get-topic :messaging-history-received)
                          :response (state/get-interaction-messaging-history interaction-id)}))))))

(defn get-messaging-metadata [interaction-id]
  (go (let [metadata-response (a/<! (rest/get-messaging-interaction-metadata-request interaction-id))
            {:keys [api-response status]} metadata-response
            {:keys [result]} api-response]
        (if (not= status 200)
          (p/publish {:topics (topics/get-topic :failed-to-retrieve-messaging-metadata)
                      :error (e/failed-to-retrieve-messaging-metadata-err interaction-id metadata-response)})
          (do (state/add-messaging-interaction-metadata! result)
              (get-messaging-history interaction-id))))))

(defn get-email-artifact-data [interaction-id artifact-id]
  (go (let [artifact-response (a/<! (rest/get-artifact-by-id-request artifact-id interaction-id nil))
            {:keys [status api-response]} artifact-response
            topic (topics/get-topic :email-artifact-received)]
        (if (not= status 200)
          (p/publish {:topics topic
                      :error (e/failed-to-retrieve-email-artifact-err interaction-id artifact-id artifact-response)})
          (do (log :info (str "[Email Processing] Email artifact received: " (js/JSON.stringify (clj->js api-response) nil 2)))
              (p/publish {:topics topic
                          :response api-response})
              (state/add-email-artifact-data interaction-id api-response)
              (let [interaction (state/get-interaction interaction-id)
                    tenant-id (state/get-active-tenant-id)
                    manifest-id (get-in interaction [:email-artifact :manifest-id])
                    files (get-in interaction [:email-artifact :files])
                    artifact-id (get-in interaction [:email-artifact :artifact-id])
                    attachments (filterv #(= (:filename %) "attachment") (get-in interaction [:email-artifact :files]))
                    manifest-url (:url (first (filter #(= (:artifact-file-id %) manifest-id) files)))]
                (log :info (str "[Email Processing] Fetching email manifest: " manifest-url))
                (let [manifest-response (a/<! (rest/get-raw-url-request manifest-url))
                      manifest-body (ih/kebabify (js/JSON.parse (:api-response manifest-response)))
                      plain-body-url (:url (first (filter #(and (= (:filename %) "body")
                                                                (starts-with? (lower-case (:content-type %)) "text/plain")) files)))
                      html-body-url (:url (first (filter #(and (= (:filename %) "body")
                                                               (starts-with? (lower-case (:content-type %)) "text/html")) files)))
                      manifest-body (assoc manifest-body :artifact-id artifact-id)]
                  (log :info (str "[Email Processing] Email manifest received: " (js/JSON.stringify (clj->js manifest-body) nil 2)))
                  (p/publish {:topics (topics/get-topic :details-received)
                              :response {:interaction-id interaction-id
                                         :body manifest-body}})
                  (state/add-email-manifest-details interaction-id manifest-body)
                  (when plain-body-url
                    (let [plain-body-response (a/<! (rest/get-raw-url-request plain-body-url))
                          plain-body (:api-response plain-body-response)]
                      (log :info (str "[Email Processing] Email plain body received: " plain-body))
                      (p/publish {:topics (topics/get-topic :plain-body-received)
                                  :response {:interaction-id interaction-id
                                             :body plain-body}})))
                  (when (not= 0 (count attachments))
                    (let [attachments (mapv #(-> %
                                                 (dissoc :content-length)
                                                 (dissoc :filename)
                                                 (dissoc :url)
                                                 (assoc :artifact-id artifact-id)) attachments)]
                      (log :info (str "[Email Processing] Attachment list received: " (js/JSON.stringify (clj->js attachments) nil 2)))
                      (p/publish {:topics (topics/get-topic :attachment-list)
                                  :response attachments})))
                  (when html-body-url
                    (let [html-body-response (a/<! (rest/get-raw-url-request html-body-url))
                          html-body (:api-response html-body-response)]
                      (log :info (str "[Email Processing] HTML body received: " html-body))
                      (p/publish {:topics (topics/get-topic :html-body-received)
                                  :response {:interaction-id interaction-id
                                             :body html-body}}))))))))))

(defn handle-work-offer [message]
  (if (= :incoming (state/find-interaction-location (:interaction-id message)))
    (let [message (merge (state/get-interaction (:interaction-id message)) message)]
      (state/transition-interaction! :incoming :pending (:interaction-id message))
      (state/add-interaction! :pending message))
    (state/add-interaction! :pending message))
  (let [{:keys [channel-type interaction-id timeout direction source]} message
        now (iu/get-now)
        expiry (.getTime (js/Date. timeout))]
    (if (> now expiry)
      (log :warn "Received an expired work offer; doing nothing")
      (do (cond
            (and
              (or (= channel-type "sms")
                  (= channel-type "messaging"))
              (= source "smooch"))
            (get-smooch-history interaction-id)
            (or (= channel-type "sms")
                (= channel-type "messaging"))
            (get-messaging-metadata interaction-id)
            (and (= channel-type "email") (= direction "inbound"))
            (let [{:keys [artifact-id]} message]
              (get-email-artifact-data interaction-id artifact-id)))
          (p/publish {:topics (topics/get-topic :work-offer-received)
                      :response message}))))
  nil)

(defn handle-new-messaging-message [payload]
  (let [payload (-> (.-payloadString payload)
                    (js/JSON.parse)
                    (ih/kebabify))
        interaction-id (:to payload)
        channel-id (:id payload)
        from (:from payload)]
    (log :info "[Messaging] Payload prior to filtering:" (clj->js payload))
    (when (= (:type payload) "message")
      (p/publish {:topics (topics/get-topic :new-message-received)
                  :response (:payload (state/augment-messaging-payload {:payload payload}))})
      (state/add-messages-to-history! interaction-id [{:payload payload}]))))

(defn handle-resource-state-change [message]
  (state/set-user-session-state! message)
  (if (not= (:state message) "offline")
    (p/publish {:topics (topics/get-topic :presence-state-changed)
                :response (select-keys message [:state :available-states :direction :reason :reason-id :reason-list-id :supervisor-id :supervisor-name])})
    (p/publish {:topics (topics/get-topic :session-ended)
                :response (select-keys message [:state :supervisor-id :supervisor-name])})))

(defn handle-work-initiated [message]
  (p/publish {:topics (topics/get-topic :work-initiated-received)
              :response message}))

(defn handle-resource-direction-change [message]
  (p/publish {:topics (topics/get-topic :direction-changed)
              :response (select-keys message [:state :available-states :direction :supervisor-id :supervisor-name])}))

(defn handle-work-rejected [message]
  (let [{:keys [interaction-id]} message]
    (when (state/find-interaction-location interaction-id)
      (state/transition-interaction! :pending :past interaction-id))
    (p/publish {:topics (topics/get-topic :work-rejected-received)
                :response {:interaction-id interaction-id}})))

(defn handle-custom-fields [message]
  (let [{:keys [interaction-id]} message
        custom-field-details {:custom-fields (:custom-fields message)
                              :interaction-id interaction-id}]
    (when (state/find-interaction-location interaction-id)
      (state/add-interaction-custom-field-details! custom-field-details interaction-id))
    (p/publish {:topics (topics/get-topic :custom-fields-received)
                :response custom-field-details})))

(defn handle-interaction-update-transfer-menu [message]
  (let [{:keys [interaction-id notification-type]} message
        interaction-update-transfer-menu-details {:transfer-lists-from-flow (get-in message [:extra-params :flow-assigned-transfer-lists])
                                                  :interaction-id interaction-id
                                                  :notification-type notification-type}]
    (when (state/find-interaction-location interaction-id)
      (state/add-interaction-interaction-update-transfer-menu-details! interaction-update-transfer-menu-details interaction-id notification-type))
    (p/publish {:topics (topics/get-topic :interaction-update-transfer-menu)
                :response interaction-update-transfer-menu-details})))

(defn handle-disposition-codes [message]
  (let [{:keys [interaction-id]} message
        disposition-code-details (:disposition-codes message)
        dispositions (:dispositions disposition-code-details)]
    ;; If the user hasn't configured a disposition list in their flow, a default
    ;; platform-level disposition list is going to be provided. This disposition
    ;; list *should not be used*, so we ignore it and don't send them dispos in
    ;; the scenario where it gets that platform-level list back. The way we
    ;; identify whether or not it is that list is if the list of codes comes
    ;; under the key of "items" VS under the key of "dispositions".
    (when (and disposition-code-details dispositions)
      (when (state/find-interaction-location interaction-id)
        (state/add-interaction-disposition-code-details! disposition-code-details interaction-id))
      (p/publish {:topics (topics/get-topic :disposition-codes-received)
                  :response message}))))

(defn handle-session-start [message]
  nil)

(defn handle-interaction-heartbeat [message]
  nil)

(defn handle-work-accepted [message]
  (let [{:keys [interaction-id tenant-id active-resources customer-on-hold recording]} message
        interaction (state/get-pending-interaction interaction-id)
        {:keys [direction channel-type source]} interaction]
    (state/transition-interaction! :pending :active interaction-id)
    (p/publish {:topics (topics/get-topic :work-accepted-received)
                :response {:interaction-id interaction-id
                           :active-resources active-resources
                           :customer-on-hold customer-on-hold
                           :recording recording}})
    (when (and
            (or (= channel-type "sms")
                (= channel-type "messaging"))
            (not= source "smooch"))
      (messaging/subscribe-to-messaging-interaction
       {:tenant-id tenant-id
        :interaction-id interaction-id
        :env (state/get-env)}))
    (when (= channel-type "email")
      (go (let [artifact-body {:artifactType "email"}

                {:keys [api-response status] :as artifact-response}
                (a/<! (rest/create-artifact-request interaction-id artifact-body))

                error-pub-fn
                #(p/publish {:topics (topics/get-topic :failed-to-create-email-reply-artifact)
                             :error (e/failed-to-create-email-reply-artifact-err
                                     interaction-id
                                     artifact-body
                                     artifact-response)})]
            (if (not= status 200)
              (error-pub-fn)
              (let [{:keys [artifact-id]} api-response]
                (if artifact-id
                  (do (state/store-email-reply-artifact-id artifact-id interaction-id)
                      (p/publish {:topics (topics/get-topic :email-reply-artifact-created)
                                  :response {:interaction-id interaction-id}}))
                  (do (log :error "Failed to get artifact id from create-artifact API response")
                      (error-pub-fn))))))))))

(defn handle-work-ended [message]
  (let [{:keys [interaction-id tenant-id]} message
        interaction (state/get-interaction interaction-id)
        {:keys [channel-type source]} interaction]
    (cond
      (and (= channel-type "voice")
        (state/get-integration-by-type "twilio")
        (= (:provider (state/get-active-extension)) "twilio"))
      (let [connection (state/get-twilio-device)]
        (.disconnectAll connection))
      (and
        (or (= channel-type "sms")
            (= channel-type "messaging"))
        (not= source "smooch"))
      (messaging/unsubscribe-to-messaging-interaction
       {:tenant-id tenant-id
        :interaction-id interaction-id
        :env (state/get-env)}))
    (state/transition-interaction! :active :past interaction-id)
    (p/publish {:topics (topics/get-topic :work-ended-received)
                :response {:interaction-id interaction-id}})))

(defn handle-customer-hold [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (topics/get-topic :customer-hold)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-customer-resume [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (topics/get-topic :customer-resume)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-resource-mute [message]
  (p/publish {:topics (topics/get-topic :resource-muted)
              :response message}))

(defn handle-resource-unmute [message]
  (p/publish {:topics (topics/get-topic :resource-unmuted)
              :response message}))

(defn handle-recording-start [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (topics/get-topic :recording-started)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-recording-stop [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (topics/get-topic :recording-ended)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-transfer-connected [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (topics/get-topic :transfer-connected)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-transfer-started [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (topics/get-topic :transfer-started)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-transfer-cancelled [message]
  (let [{:keys [interaction-id resource-id]} message]
    (p/publish {:topics (topics/get-topic :transfer-cancelled)
                :response {:interaction-id interaction-id
                           :resource-id resource-id}})))

(defn handle-script-received [message]
  (let [{:keys [interaction-id sub-id action-id resource-id script]} message
        interaction-location (state/find-interaction-location interaction-id)]
    (when (not interaction-location)
      (state/add-interaction! :incoming message))
    (state/add-script-to-interaction! interaction-id {:sub-id sub-id
                                                      :action-id action-id
                                                      :script script})
    (p/publish {:topics (topics/get-topic :script-received)
                :response {:interaction-id interaction-id
                           :script-id action-id
                           :script script}})))

(defn handle-generic [message]
  (log :warn "Couldn't handle message: " (clj->js message)))

(defn handle-resource-added [message]
  (p/publish {:topics (topics/get-topic :resource-added-received)
              :response message}))

(defn handle-resource-removed [message]
  (p/publish {:topics (topics/get-topic :resource-removed-received)
              :response message}))

(defn handle-resource-hold [message]
  (p/publish {:topics (topics/get-topic :resource-hold-received)
              :response message}))

(defn handle-resource-resume [message]
  (p/publish {:topics (topics/get-topic :resource-resume-received)
              :response message}))

(defn handle-screen-pop [message]
  (let [{:keys [pop-type new-window size target-blank pop-uri]} message
        target (if target-blank "_blank" "skylight-new-window")]
    (when (= pop-type "external-url") 
      (if new-window
        (js/window.open pop-uri target (str "width=" (:width size) ",height=" (:height size)))
        (js/window.open pop-uri target)))
    (p/publish {:topics (topics/get-topic :generic-screen-pop-received)
                :response message})))

(defn handle-wrapup [message]
  (let [wrapup-details (select-keys message [:wrapup-time :wrapup-enabled :wrapup-update-allowed :target-wrapup-time])
        {:keys [interaction-id]} message]
    (when (state/find-interaction-location interaction-id)
      (state/add-interaction-wrapup-details! wrapup-details interaction-id))
    (p/publish {:topics (topics/get-topic :wrapup-details-received)
                :response (assoc wrapup-details :interaction-id interaction-id)})))

(defn handle-wrapup-started [message]
  (let [{:keys [interaction-id tenant-id]} message
        interaction (state/get-interaction interaction-id)
        {:keys [channel-type source]} interaction
        wrapup-details (state/get-interaction-wrapup-details interaction-id)]
    (when (and
            (or (= channel-type "sms")
                (= channel-type "messaging"))
            (not= source "smooch"))
          (messaging/unsubscribe-to-messaging-interaction
           {:tenant-id tenant-id
            :interaction-id interaction-id
            :env (state/get-env)}))
    (p/publish {:topics (topics/get-topic :wrapup-started)
                :response {:interaction-id interaction-id}})))

(defn handle-wrapup-ended [message]
  (let [{:keys [interaction-id]} message
        wrapup-details (state/get-interaction-wrapup-details interaction-id)]
    (p/publish {:topics (topics/get-topic :wrapup-ended)
                :response {:interaction-id interaction-id}})))

(defn handle-update-call-controls [message]
  (p/publish {:topics (topics/get-topic :update-call-controls)
              :response message}))

(defn handle-show-banner [message]
  (p/publish {:topics (topics/get-topic :show-banner)
              :response message}))

(defn handle-silent-monitor-start [message]
  (p/publish {:topics (topics/get-topic :silent-monitor-start)
              :response message}))

(defn handle-silent-monitor-end [message]
  (state/set-monitored-interaction! nil)
  (p/publish {:topics (topics/get-topic :silent-monitor-end)
              :response message}))

(defn handle-customer-connected [message]
  (p/publish {:topics (topics/get-topic :customer-connected)
              :response message}))

(defn handle-customer-hold-error [message]
  (let [{:keys [interaction-id]} message]
    (p/publish {:topics (topics/get-topic :customer-hold-error)
                :error (e/failed-to-place-customer-on-hold-err interaction-id message)})))

(defn handle-customer-resume-error [message]
  (let [{:keys [interaction-id]} message]
    (p/publish {:topics (topics/get-topic :customer-resume-error)
                :error (e/failed-to-resume-customer-err interaction-id message)})))

(defn handle-send-message [message]
  (p/publish {:topics (topics/get-topic :smooch-message-received)
              :response message}))

(defn handle-conversation-read [message]
  (p/publish {:topics (topics/get-topic :smooch-conversation-read-received)
              :response message}))

(defn handle-typing [message]
  (p/publish {:topics (topics/get-topic :smooch-typing-received)
              :response message}))

(defn msg-router [message msg-type]
  (let [handling-fn (case (:sdk-msg-type message)
                      :INTERACTIONS/WORK_ACCEPTED_RECEIVED handle-work-accepted
                      :INTERACTIONS/WORK_OFFER_RECEIVED handle-work-offer
                      :INTERACTIONS/WORK_REJECTED_RECEIVED handle-work-rejected
                      :INTERACTIONS/WORK_INITIATED_RECEIVED handle-work-initiated
                      :INTERACTIONS/WORK_ENDED_RECEIVED handle-work-ended
                      :INTERACTIONS/CUSTOM_FIELDS_RECEIVED handle-custom-fields
                      :INTERACTIONS/INTERACTION_UPDATE_TRANSFER_MENU handle-interaction-update-transfer-menu
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
                      :INTERACTIONS/TRANSFER_STARTED handle-transfer-started
                      :INTERACTIONS/TRANSFER_CANCELLED handle-transfer-cancelled
                      :INTERACTIONS/SCRIPT_RECEIVED handle-script-received
                      :SESSION/CHANGE_STATE_RESPONSE handle-resource-state-change
                      :SESSION/CHANGE_DIRECTION_RESPONSE handle-resource-direction-change
                      :SESSION/START_SESSION_RESPONSE handle-session-start
                      :INTERACTIONS/RESOURCE_ADDED handle-resource-added
                      :INTERACTIONS/RESOURCE_REMOVED handle-resource-removed
                      :INTERACTIONS/RESOURCE_HOLD handle-resource-hold
                      :INTERACTIONS/RESOURCE_RESUME handle-resource-resume
                      :INTERACTIONS/UPDATE_CALL_CONTROLS handle-update-call-controls
                      :INTERACTIONS/SHOW_BANNER handle-show-banner
                      :INTERACTIONS/SILENT_MONITOR_START handle-silent-monitor-start
                      :INTERACTIONS/SILENT_MONITOR_END handle-silent-monitor-end
                      :INTERACTIONS/CUSTOMER_CONNECTED handle-customer-connected
                      :INTERACTIONS/CUSTOMER_HOLD_ERROR handle-customer-hold-error
                      :INTERACTIONS/CUSTOMER_RESUME_ERROR handle-customer-resume-error
                      :INTERACTIONS/SEND_MESSAGE_RECEIVED handle-send-message
                      :INTERACTIONS/CONVERSATION_READ_RECEIVED handle-conversation-read
                      :INTERACTIONS/TYPING_RECEIVED handle-typing
                      nil)]
    (when (and (get message :action-id)
               (not= (get message :interaction-id) "00000000-0000-0000-0000-000000000000")
               (not= (get message :type) "send-script")
               (not= msg-type "send-message"))
      (log :info (str "Acknowledging receipt of flow action: "
                      (or (:notification-type message) (:type message))))
      (when (or (:notification-type message) (:type message))
        (let [{:keys [action-id sub-id resource-id tenant-id interaction-id]} message
              action-body {:source "client"
                           :sub-id sub-id
                           :update {:resource-id resource-id}}]
          (go (let [ack-response (a/<! (rest/send-flow-action-request interaction-id action-id action-body))
                    {:keys [api-response status]} ack-response]
                (if (not= status 200)
                  (do (p/publish {:topics (topics/get-topic :flow-action-acknowledged)
                                  :error (e/failed-to-acknowledge-flow-action-err interaction-id ack-response message)})
                      (log :error "Failed to acknowledge flow action"))
                  (p/publish {:topics (topics/get-topic :flow-action-acknowledged)
                              :response {:interaction-id interaction-id}})))))))
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
                                       "interaction-update-transfer-menu" :INTERACTIONS/INTERACTION_UPDATE_TRANSFER_MENU
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
                                       "resource-added" :INTERACTIONS/RESOURCE_ADDED
                                       "resource-removed" :INTERACTIONS/RESOURCE_REMOVED
                                       "resource-hold" :INTERACTIONS/RESOURCE_HOLD
                                       "resource-resume" :INTERACTIONS/RESOURCE_RESUME
                                       "transfer-connected" :INTERACTIONS/TRANSFER_CONNECTED_RECEIVED
                                       "transfer-started" :INTERACTIONS/TRANSFER_STARTED
                                       "transfer-cancelled" :INTERACTIONS/TRANSFER_CANCELLED
                                       "update-call-controls" :INTERACTIONS/UPDATE_CALL_CONTROLS
                                       "show-banner" :INTERACTIONS/SHOW_BANNER
                                       "silent-monitor-start" :INTERACTIONS/SILENT_MONITOR_START
                                       "silent-monitor-end" :INTERACTIONS/SILENT_MONITOR_END
                                       "customer-connected" :INTERACTIONS/CUSTOMER_CONNECTED
                                       "customer-hold-error" :INTERACTIONS/CUSTOMER_HOLD_ERROR
                                       "customer-resume-error" :INTERACTONS/CUSTOMER_RESUME_ERRRO
                                       :INTERACTIONS/GENERIC_AGENT_NOTIFICATION)]
      (merge {:sdk-msg-type inferred-notification-type} message))))

(defn infer-message-type
  [message]
  (let [type (:message-type message)
        inferred-message-type (case type
                                "received-message" :INTERACTIONS/SEND_MESSAGE_RECEIVED
                                "conversation-read" :INTERACTIONS/CONVERSATION_READ_RECEIVED
                                "typing-start" :INTERACTIONS/TYPING_RECEIVED
                                "typing-stop" :INTERACTIONS/TYPING_RECEIVED
                                "show-banner" :INTERACTIONS/SHOW_BANNER
                                :INTERACTIONS/GENERIC_AGENT_NOTIFICATION)]
    (merge {:sdk-msg-type inferred-message-type} message)))

(defn sqs-msg-router [message]
  (let [cljsd-msg (ih/kebabify message)
        msg-type (:type cljsd-msg)
        channel-type (:channel-type cljsd-msg)
        session-id (or (get cljsd-msg :session-id)
                       (get-in cljsd-msg [:resource :session-id]))
        inferred-msg (case msg-type
                       "resource-state-change" (merge {:sdk-msg-type :SESSION/CHANGE_STATE_RESPONSE} cljsd-msg)
                       "resource-direction-change" (merge {:sdk-msg-type :SESSION/CHANGE_DIRECTION_RESPONSE} cljsd-msg)
                       "work-offer" (merge {:sdk-msg-type :INTERACTIONS/WORK_OFFER_RECEIVED} cljsd-msg)
                       "send-script" (merge {:sdk-msg-type :INTERACTIONS/SCRIPT_RECEIVED} cljsd-msg)
                       "send-message" (infer-message-type cljsd-msg)
                       "agent-notification" (infer-notification-type cljsd-msg)
                       nil)]
    (when (state/get-blast-sqs-output)
      (log :debug (str "[SQS] Message received (" (:sdk-msg-type inferred-msg) "):") (ih/camelify message)))
    (if inferred-msg
      (msg-router inferred-msg msg-type)
      (do (log :warn "Unable to infer message type from sqs")
          (p/publish {:topics (topics/get-topic :unknown-agent-notification-type-received)
                      :error (e/unknown-agent-notification-type-err inferred-msg)})
          nil))))

(defn messaging-msg-router [message]
  (handle-new-messaging-message message))

(defn twilio-msg-router [message type]
  (log :warn "message in twilio msg router" message))
