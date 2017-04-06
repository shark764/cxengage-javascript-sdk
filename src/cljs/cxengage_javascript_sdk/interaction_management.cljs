(ns cxengage-javascript-sdk.interaction-management
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a]
            [clojure.string :as s :refer [starts-with? lower-case]]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.errors :as err]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.modules.messaging :as messaging]))

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

(defn get-email-artifact-data [tenant-id interaction-id artifact-id]
  (log :debug "[Email Processing] Tenant, Interaction, and Artifact IDs from work offer:" tenant-id interaction-id artifact-id)
  (let [artifact-request {:method :get
                          :url (str (state/get-base-api-url) "tenants/" tenant-id "/interactions/" interaction-id "/artifacts/" artifact-id)}]
    (go (let [artifact-response (a/<! (iu/api-request artifact-request))
              {:keys [status api-response]} artifact-response]
          (if (not= status 200)
            (p/publish {:topics (p/get-topic :email-artifact-received)
                        :response api-response})
            (do (log :debug (str "[Email Processing] Email artifact received: " (js/JSON.stringify (clj->js api-response) nil 2)))
                (state/add-email-artifact-data interaction-id api-response)))))))

(defn get-email-bodies [interaction-id]
  (let [interaction (state/get-interaction interaction-id)
        tenant-id (state/get-active-tenant-id)
        manifest-id (get-in interaction [:email-artifact :manifest-id])
        files (get-in interaction [:email-artifact :files])
        artifact-id (get-in interaction [:email-artifact :artifact-id])
        attachments (filterv #(= (:filename %) "attachment") (get-in interaction [:email-artifact :files]))
        manifest-url (:url (first (filter #(= (:artifact-file-id %) manifest-id) files)))
        manifest-request (iu/api-request {:method :get
                                          :url manifest-url})]
    (log :debug (str "[Email Processing] Fetching email manifest: " manifest-url))
    (go (let [manifest-response (a/<! manifest-request)
              manifest-body (iu/kebabify (js/JSON.parse (:api-response manifest-response)))
              plain-body-url (:url (first (filter #(and (= (:filename %) "body")
                                                        (starts-with? (lower-case (:content-type %)) "text/plain")) files)))
              html-body-url (:url (first (filter #(and (= (:filename %) "body")
                                                       (starts-with? (lower-case (:content-type %)) "text/html")) files)))
              manifest-body (assoc manifest-body :artifact-id artifact-id)]
          (log :debug (str "[Email Processing] Email manifest received: " (js/JSON.stringify (clj->js manifest-body) nil 2)))
          (p/publish {:topics (p/get-topic :details-received)
                      :response {:interaction-id interaction-id
                                 :body manifest-body}})
          (state/add-email-manifest-details interaction-id manifest-body)
          (when plain-body-url
            (let [plain-body-response (a/<! (iu/api-request {:method :get
                                                             :url plain-body-url}))
                  plain-body (:api-response plain-body-response)]
              (log :debug (str "[Email Processing] Email plain body received: " plain-body))
              (p/publish {:topics (p/get-topic :plain-body-received)
                          :response {:interaction-id interaction-id
                                     :body plain-body}})))
          (when (not= 0 (count attachments))
            (let [attachments (mapv #(-> %
                                         (dissoc :content-length)
                                         (dissoc :filename)
                                         (dissoc :url)
                                         (assoc :artifact-id artifact-id)) attachments)]
              (log :debug (str "[Email Processing] Attachment list received: " (js/JSON.stringify (clj->js attachments) nil 2)))
              (p/publish {:topics (p/get-topic :attachment-list)
                          :response attachments})))
          (when html-body-url
            (let [html-body-response (a/<! (iu/api-request {:method :get
                                                            :url html-body-url}))
                  html-body (:api-response html-body-response)]
              (log :debug (str "[Email Processing] HTML body received: " html-body))
              (p/publish {:topics (p/get-topic :html-body-received)
                          :response {:interaction-id interaction-id
                                     :body html-body}})))))))

(defn handle-work-offer [message]
  (state/add-interaction! :pending message)
  (let [{:keys [channel-type interaction-id timeout]} message
        now (iu/get-now)
        expiry (.getTime (js/Date. timeout))]
    (if (> now expiry)
      (log :warn "Received an expired work offer; doing nothing")
      (do
        (when (or (= channel-type "sms")
                  (= channel-type "messaging"))
          (let [{:keys [tenant-id interaction-id]} message]
            (get-messaging-metadata tenant-id interaction-id)))
        (when (= channel-type "email")
          (let [{:keys [tenant-id interaction-id artifact-id]} message]
            (get-email-artifact-data tenant-id interaction-id artifact-id)))
        (p/publish {:topics (p/get-topic :work-offer-received)
                    :response message}))))
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
        custom-field-details {:custom-fields (:custom-fields message)
                              :interaction-id interaction-id}]
    (state/add-interaction-custom-field-details! custom-field-details interaction-id)
    (p/publish {:topics (p/get-topic :custom-fields-received)
                :response custom-field-details})))

(defn handle-disposition-codes [message]
  (let [{:keys [interaction-id]} message
        disposition-code-details (:disposition-codes message)]
    (when disposition-code-details
      (state/add-interaction-disposition-code-details! disposition-code-details interaction-id)
      (p/publish {:topics (p/get-topic :disposition-codes-received)
                  :response message}))))

(defn handle-session-start [message]
  nil)

(defn handle-interaction-heartbeat [message]
  nil)

(defn handle-work-accepted [message]
  (let [{:keys [interaction-id tenant-id active-resources customer-on-hold recording]} message
        interaction (state/get-pending-interaction interaction-id)
        channel-type (get interaction :channel-type)]
    (state/transition-interaction! :pending :active interaction-id)
    (p/publish {:topics (p/get-topic :work-accepted-received)
                :response {:interaction-id interaction-id
                           :active-resources active-resources
                           :customer-on-hold customer-on-hold
                           :recording recording}})
    (when (or (= channel-type "sms")
              (= channel-type "messaging"))
      (messaging/subscribe-to-messaging-interaction
       {:tenant-id tenant-id
        :interaction-id interaction-id
        :env (state/get-env)}))
    (when (= channel-type "email")
      (let [api-url (state/get-base-api-url)
            artifact-url (iu/build-api-url-with-params
                          (str api-url "tenants/tenant-id/interactions/interaction-id/artifacts")
                          {:tenant-id tenant-id
                           :interaction-id interaction-id})
            artifact-request {:method :post
                              :url artifact-url
                              :body {:artifactType "email"}}]
        ;;(aset js/window "IID" interaction-id)
        (go (let [artifact-create-response (a/<! (iu/api-request artifact-request))
                  {:keys [api-response status]} artifact-create-response
                  {:keys [artifact-id]} api-response]
              (state/store-email-reply-artifact-id artifact-id interaction-id)
              (get-email-bodies interaction-id)))))))

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
  (p/publish {:topics (p/get-topic :resource-muted)
              :response message}))

(defn handle-resource-unmute [message]
  (p/publish {:topics (p/get-topic :resource-unmuted)
              :response message}))

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
  (let [{:keys [interaction-id sub-id action-id resource-id script]} message]
    (state/add-script-to-interaction! interaction-id {:sub-id sub-id
                                                      :action-id action-id
                                                      :script script})
    (p/publish {:topics (p/get-topic :script-received)
                :response {:interaction-id interaction-id
                           :resource-id resource-id
                           :sub-id sub-id
                           :script script}})))

(defn handle-generic [message]
  nil)

(defn handle-resource-added [message]
  (p/publish {:topics (p/get-topic :resource-added-received)
              :response message}))

(defn handle-resource-removed [message]
  (p/publish {:topics (p/get-topic :resource-removed-received)
              :response message}))

(defn handle-resource-hold [message]
  (p/publish {:topics (p/get-topic :resource-hold-received)
              :response message}))

(defn handle-resource-resume [message]
  (p/publish {:topics (p/get-topic :resource-resume-received)
              :response message}))

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
                      :INTERACTIONS/RESOURCE_ADDED handle-resource-added
                      :INTERACTIONS/RESOURCE_REMOVED handle-resource-removed
                      :INTERACTIONS/RESOURCE_HOLD handle-resource-hold
                      :INTERACTIONS/RESOURCE_RESUME handle-resource-resume
                      nil)]
    (when (and (get message :action-id)
               (not= (get message :interaction-id) "00000000-0000-0000-0000-000000000000")
               (not= (get message :type) "send-script"))
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
                                       "resource-added" :INTERACTIONS/RESOURCE_ADDED
                                       "resource-removed" :INTERACTIONS/RESOURCE_REMOVED
                                       "resource-hold" :INTERACTIONS/RESOURCE_HOLD
                                       "resource-resume" :INTERACTIONS/RESOURCE_RESUME
                                       "transfer-connected" :INTERACTIONS/TRANSFER_CONNECTED_RECEIVED
                                       :INTERACTIONS/GENERIC_AGENT_NOTIFICATION)]
      (merge {:sdk-msg-type inferred-notification-type} message))))

(defn sqs-msg-router [message]
  (let [cljsd-msg (iu/kebabify message)
        session-id (or (get cljsd-msg :session-id)
                       (get-in cljsd-msg [:resource :session-id]))
        inferred-msg (case (:type cljsd-msg)
                       "resource-state-change" (merge {:sdk-msg-type :SESSION/CHANGE_STATE_RESPONSE} cljsd-msg)
                       "work-offer" (merge {:sdk-msg-type :INTERACTIONS/WORK_OFFER_RECEIVED} cljsd-msg)
                       "send-script" (merge {:sdk-msg-type :INTERACTIONS/SCRIPT_RECEIVED} cljsd-msg)
                       "agent-notification" (infer-notification-type cljsd-msg)
                       nil)]
    (when (state/get-blast-sqs-output)
      (log :debug (str "[BLAST SQS OUTPUT] Message received (" (:sdk-msg-type inferred-msg) "):") (iu/camelify message)))
    (if inferred-msg
      (msg-router inferred-msg)
      (do (log :warn "Unable to infer message type from sqs")
          nil))))

(defn messaging-msg-router [message]
  (handle-new-messaging-message message))

(defn twilio-msg-router [message type]
  (log :warn "message in twilio msg router" message))
