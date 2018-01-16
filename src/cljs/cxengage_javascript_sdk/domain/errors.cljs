(ns cxengage-javascript-sdk.domain.errors)

(defn bad-sdk-init-opts-err []
  {:code 1000
   :context :general
   :data {}
   :level "error"
   :message "Invalid SDK initialization options provided. Verify your values against the SDK documentation."})

(defn wrong-number-sdk-opts-err []
  {:code 1001
   :context :general
   :data {}
   :level "error"
   :message "Incorrect number of arguments provided to SDK initialization. Verify your values against the SDK documentation."})

(defn required-module-failed-to-start-err []
  {:code 1002
   :context :general
   :data {}
   :level "session-fatal"
   :message "A required SDK module failed to start, unable to initialize the SDK."})

(defn args-failed-spec-err [data]
  {:code 1003
   :context :general
   :data {:problems data}
   :level "error"
   :message "The parameters object passed to the SDK function did not adhere to the spec defined. Verify your values against the SDK documentation."})

(defn wrong-number-of-sdk-fn-args-err []
  {:code 1004
   :context :general
   :data {}
   :level "error"
   :message "Incorrect number of arguments passed to SDK function. All SDK functions support up to 2 arguments; the first being the parameters map, the second being an optional callback. Verify your values against the SDK documentation."})

(defn unknown-agent-notification-type-err [data]
  {:code 1005
   :context :general
   :data {:notification-received data}
   :level "error"
   :message "Received an unknown agent notification type. Unable to parse agent notification."})

(defn insufficient-permissions-err [data]
  {:code 2000
   :context :session
   :data {:required-permissions data}
   :level "error"
   :message "You lack sufficient permissions in order to perform this action."})

(defn failed-to-get-session-config-err [data]
  {:code 2001
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to get user session config. The API returned an error."})

(defn failed-to-start-agent-session-err [data]
  {:code 2002
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to start an agent session. The API returned an error."})

(defn session-heartbeats-failed-err [data]
  {:code 2003
   :context :session
   :data {:api-response data}
   :level "session-fatal"
   :message "Session heartbeats failed. Unable to continue using agent session."})

(defn failed-to-change-state-err [data]
  {:code 2004
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to change agent state. The API returned an error."})

(defn invalid-extension-provided-err [data]
  {:code 2005
   :context :session
   :data {:valid-extensions data}
   :level "error"
   :message "Invalid extension provided. Must be in the list of extensions provided via your user config. Unable to transition agent to a ready state."})

(defn failed-to-update-extension-err [data]
  {:code 2006
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to update user extension. Unable to transition agent to ready state."})

(defn invalid-reason-info-err [data]
  {:code 2007
   :context :session
   :data {:valid-reasons data}
   :level "error"
   :message "Invalid reason info provided. Must be in the list of reasons provided via your user config. Unable to transition agent to a not ready"})

(defn failed-to-set-direction-err [data]
  {:code 2008
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to set user direction. The API returned an error."})

(defn failed-to-get-user-extensions-err [data]
  {:code 2009
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to go ready; unable to retrieve user extensions. The API returned an error."})

(defn failed-to-get-tenant-err [data]
  {:code 2010
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to select tenant; unable to retrieve tenant data. The API returned an error."})

(defn failed-to-get-region-err [data]
  {:code 2011
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to select tenant; unable to retrieve region data. The API returned an error."})

(defn failed-to-get-tenant-details-err [data]
  {:code 2012
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to retrieve user's tenant details. The API returned an error."})

(defn login-failed-token-request-err [data]
  {:code 3000
   :context :authentication
   :data {:api-response data}
   :level "error"
   :message "Login attempt failed. Unable to retrieve token."})

(defn logout-failed-err [data]
  {:code 3001
   :context :authentication
   :data {:api-response data}
   :level "error"
   :message "Logout attempt failed."})

(defn login-failed-login-request-err [data]
  {:code 3002
   :context :authentication
   :data {:api-response data}
   :level "error"
   :message "Login attempt failed. Login request failed."})

(defn failed-to-init-cognito-sdk-err [data]
  {:code 3003
   :context :authentication
   :data data
   :level "error"
   :message "Failed to initalize the AWS Cognito SDK."})

(defn failed-cognito-auth-err [data]
  {:code 3004
   :context :authentication
   :data data
   :level "error"
   :message "Failed to authenticate with AWS Cognito."})

(defn failed-to-get-auth-info-err [data]
  {:code 3005
   :context :authentication
   :data data
   :level "error"
   :message "Failed to retrieve SSO authentication information."})

(defn active-interactions-err [data]
  {:code 4000
   :context :interaction
   :data {:active-interactions data}
   :level "error"
   :message "Unable to perform this action as there are interactions still active."})

(defn work-offer-expired-err []
  {:code 4001
   :context :interaction
   :data {}
   :level "error"
   :message "Attempted to accept a work offer that is already expired."})

(defn failed-to-acknowledge-flow-action-err [interaction-id data]
  {:code 4002
   :context :interaction
   :data {:interaction-id interaction-id
          :api-response data}
   :level "interaction-fatal"
   :message "Failed to acknowledge flow action."})

(defn failed-to-end-interaction-err [interaction-id data]
  {:code 4003
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "interaction-fatal"
   :message "Failed to end interaction."})

(defn failed-to-accept-interaction-err [interaction-id data]
  {:code 4004
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "interaction-fatal"
   :message "Failed to accept interaction."})

(defn failed-to-focus-interaction-err [interaction-id data]
  {:code 4005
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failed to focus interaction."})

(defn failed-to-unfocus-interaction-err [interaction-id data]
  {:code 4006
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failed to unfocus interaction."})

(defn failed-to-assign-contact-to-interaction-err [interaction-id data]
  {:code 4007
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failed to assign specified contact to interaction."})

(defn failed-to-unassign-contact-from-interaction-err [interaction-id data]
  {:code 4008
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failed to unassign specified contact to interaction."})

(defn failed-to-enable-wrapup-err [interaction-id data]
  {:code 4009
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failed to enable wrapup."})

(defn failed-to-disable-wrapup-err [interaction-id data]
  {:code 4010
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failed to disable wrapup."})

(defn failed-to-end-wrapup-err [interaction-id data]
  {:code 4011
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failed to end wrapup."})

(defn failed-to-deselect-disposition-err [interaction-id data]
  {:code 4012
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failde to deselect disposition code."})

(defn failed-to-select-disposition-err [interaction-id data]
  {:code 4013
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failed to select disposition code."})

(defn failed-to-get-interaction-note-err [interaction-id data]
  {:code 4014
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failed to get specified interaction note."})

(defn failed-to-list-interaction-notes-err [interaction-id data]
  {:code 4015
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failed to list interaction notes for specified interaction."})

(defn failed-to-update-interaction-note-err [interaction-id data]
  {:code 4016
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failed to update specified interaction note."})

(defn failed-to-create-interaction-note-err [interaction-id data]
  {:code 4017
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failed to create interaction note."})

(defn failed-to-send-interaction-script-response-err [interaction-id data]
  {:code 4018
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "error"
   :message "Failed to send script reply."})

(defn failed-to-send-custom-interrupt-err [interaction-id data]
  {:code 4019
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level "interaction-fatal"
   :message "Failed to send custom interrupt."})

(defn invalid-disposition-provided-err [interaction-id data]
  {:code 4020
   :context :interaction
   :level "error"
   :data {:valid-dispositions data
          :interaction-id interaction-id}
   :message "Invalid disposition code provided."})

(defn unable-to-find-script-err [interaction-id data]
  {:code 4021
   :context :interaction
   :level "error"
   :data {:interaction-id interaction-id
          :script-id-provided data}
   :message "Unable to find a script that matches that ID."})

(defn failed-to-refresh-sqs-integration-err [data]
  {:code 5000
   :context :sqs
   :data {:api-response data}
   :level "session-fatal"
   :message "Failed to refresh SQS Queue object. Unable to continue agent notification polling."})

(defn failed-to-retrieve-messaging-history-err [interaction-id data]
  {:code 6000
   :context :messaging
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to retrieve messaging interaction history."})

(defn failed-to-retrieve-messaging-metadata-err [interaction-id data]
  {:code 6001
   :context :messaging
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to retrieve messaging interaction metadata."})

(defn failed-to-get-messaging-transcripts-err [interaction-id data]
  {:code 6002
   :context :messaging
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to retrieve messaging transcripts."})

(defn failed-to-create-outbound-sms-interaction-err [phone-number message data]
  {:code 6003
   :context :messaging
   :data {:phone-number phone-number
          :message message
          :api-response data}
   :level "error"
   :message "Failed to create outbound SMS interaction."})

(defn failed-to-send-outbound-sms-err [interaction-id message data]
  {:Code 6004
   :context :messaging
   :data {:interaction-id interaction-id
          :message message
          :api-response data}
   :level "error"
   :message "Failed to send outbound SMS reply."})

(defn failed-to-get-specific-messaging-transcript-err [interaction-id artifact-id data]
  {:code 6005
   :context :messaging
   :data {:interaction-id interaction-id
          :artifact-id artifact-id
          :api-response data}
   :level "error"
   :message "Failed to retrieve specific messaging transcript."})

(defn failed-to-refresh-twilio-integration-err [data]
  {:code 7000
   :context :voice
   :data {:api-response data}
   :level "session-fatal"
   :message "Failed to refresh Twilio credentials."})

(defn failed-to-send-digits-invalid-interaction-err [interaction-id]
  {:code 7001
   :context :voice
   :data {:interaction-id interaction-id}
   :level "error"
   :message "Unable to send digits to specified interaction. Interaction must be active and of type voice."})

(defn no-twilio-integration-err []
  {:code 7002
   :context :voice
   :data {}
   :level "error"
   :message "Unable to perform action - no twilio integration set up"})

(defn failed-to-place-resource-on-hold-err [interaction-id target-resource-id resource-id data]
  {:code 7003
   :context :voice
   :data {:interaction-id interaction-id
          :target-resource-id target-resource-id
          :resource-id resource-id
          :api-response data}
   :level "error"
   :message "Failed to place resource on hold."})

(defn failed-to-resume-resource-err [interaction-id target-resource-id resource-id data]
  {:code 7004
   :context :voice
   :data {:interaction-id interaction-id
          :target-resource-id target-resource-id
          :resource-id resource-id
          :api-response data}
   :level "error"
   :message "Failed to resume resource from hold."})

(defn failed-to-mute-target-resource-err [interaction-id target-resource-id resource-id data]
  {:code 7005
   :context :voice
   :data {:interaction-id interaction-id
          :target-resource-id target-resource-id
          :resource-id resource-id
          :api-response data}
   :level "error"
   :message "Failed to mute target resource."})

(defn failed-to-unmute-target-resource-err [interaction-id target-resource-id resource-id data]
  {:code 7006
   :context :voice
   :data {:interaction-id interaction-id
          :target-resource-id target-resource-id
          :resource-id resource-id
          :api-response data}
   :level "error"
   :message "Failed to unmute target resource."})

(defn failed-to-resume-all-err [interaction-id data]
  {:code 7007
   :context :voice
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to resume all resources."})

(defn failed-to-start-recording-err [interaction-id resource-id data]
  {:code 7008
   :context :voice
   :data {:interaction-id interaction-id
          :resource-id resource-id
          :api-response data}
   :level "error"
   :message "Failed to start recording on interaction."})

(defn failed-to-transfer-to-resource-err [transfer-body data]
  {:code 7009
   :context :voice
   :data {:requested-transfer transfer-body
          :api-response data}
   :level "error"
   :message "Failed to transfer customer to another resource."})

(defn failed-to-cancel-resource-transfer-err [transfer-body data]
  {:code 7010
   :context :voice
   :data {:requested-transfer transfer-body
          :api-response data}
   :level "error"
   :message "Failed to cancel pending transfer to resource."})

(defn failed-to-place-customer-on-hold-err [interaction-id data]
  {:code 7011
   :context :voice
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to place customer on hold."})

(defn failed-to-stop-recording-err [interaction-id resource-id data]
  {:code 7012
   :context :voice
   :data {:interaction-id interaction-id
          :resource-id resource-id
          :api-response data}
   :level "error"
   :message "Failed to stop recording on interaction."})

(defn failed-to-resume-customer-err [interaction-id data]
  {:code 7013
   :context :voice
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to resume customer from hold."})

(defn failed-to-remove-resource-err [interaction-id target-resource-id resource-id data]
  {:code 7014
   :context :voice
   :data {:interaction-id interaction-id
          :target-resource-id target-resource-id
          :resource-id resource-id
          :api-response data}
   :level "error"
   :message "Failed to remove the resource from the interaction."})

(defn failed-to-transfer-to-queue-err [transfer-body data]
  {:code 7015
   :context :voice
   :data {:requested-transfer transfer-body
          :api-response data}
   :level "error"
   :message "Failed to transfer customer to queue."})

(defn failed-to-cancel-queue-transfer-err [transfer-body data]
  {:code 7016
   :context :voice
   :data {:requested-transfer transfer-body
          :api-response data}
   :level "error"
   :message "Failed to cancel pending transfer to queue."})

(defn failed-to-transfer-to-extension-err [transfer-body data]
  {:code 7017
   :context :voice
   :data {:requested-transfer transfer-body
          :api-response data}
   :level "error"
   :message "Failed to transfer customer to extension."})

(defn failed-to-cancel-extension-transfer-err [transfer-body data]
  {:code 7018
   :context :voice
   :data {:requested-transfer transfer-body
          :api-response data}
   :level "error"
   :message "Failed to cancel pending transfer to extension."})

(defn failed-to-perform-outbound-dial-err [phone-number data]
  {:code 7019
   :context :voice
   :data {:phone-number phone-number
          :api-response data}
   :level "error"
   :message "Failed to perform outbound dial."})

(defn failed-to-cancel-outbound-dial-err [interaction-id data]
  {:code 7020
   :context :voice
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to cancel outbound dial."})

(defn failed-to-get-specific-recording-err [interaction-id artifact-id data]
  {:code 7021
   :context :voice
   :data {:interaction-id interaction-id
          :artifact-id artifact-id
          :api-response data}
   :level "error"
   :message "Failed to retrieve the specified recording."})

(defn failed-to-start-silent-monitoring [interaction-id data]
  {:code 7022
   :context :voice
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to start silent monitoring on interaction."})

(defn failed-to-init-twilio-err [error]
  {:code 7023
   :context :voice
   :data {:error error}
   :level "interaction-fatal"
   :message "Twilio Device encountered an error."})

(defn failed-to-send-voice-interaction-heartbeat-err [interaction-id data]
  {:code 7024
   :context :voice
   :data {:api-response data
          :interaction-id interaction-id}
   :level "interaction-fatal"
   :message "Voice interaction heartbeat failed. The interaction on longer exists in CxEngage."})

(defn failed-to-send-voice-interaction-heartbeat-5xx-err [interaction-id data]
  {:code 7025
   :context :voice
   :data {:api-response data
          :interaction-id interaction-id}
   :level "warn"
   :message "Voice interaction heartbeat failed with 5xx error. Retrying."})

(defn no-microphone-access-err [error]
  {:code 8000
   :context :twilio
   :data {:error error}
   :level "session-fatal"
   :message "Failed to connect to Twilio. Microphone access must be enabled within your browser to utilize voice features."})

(defn failed-to-send-twilio-digits-err [digit]
  {:code 8001
   :context :twilio
   :data {:digit digit}
   :level "error"
   :message "Failed to send digits via Twilio. Potentially invalid dial tones."})

(defn failed-to-find-twilio-connection-object
  [interaction-id]
  {:code 8002
   :context :twilio
   :data {:interaction-id interaction-id}
   :level "interaction-fatal"
   :message "Failed to find the twilio connection object in state"})

(defn force-killed-twilio-connection-err
  [interaction-id]
  {:code 8003
   :context :twilio
   :data {:interaction-id interaction-id}
   :level "interaction-fatal"
   :message "Force-killed the connection with Twilio; previous attempts to end it naturally were unsuccessful."})

(defn failed-to-connect-to-mqtt-err [msg]
  {:code 9000
   :context :mqtt
   :data {:message msg}
   :level "session-fatal"
   :message "Unable to connect to MQTT."})

(defn failed-to-create-email-reply-artifact-err [interaction-id artifact-body data]
  {:code 10000
   :context :email
   :data {:interaction-id interaction-id
          :artifact-body artifact-body
          :api-response data}
   :level "interaction-fatal"
   :message "Failed to create email artifact for email reply."})

(defn failed-to-retrieve-email-artifact-err [interaction-id artifact-id data]
  {:code 10001
   :context :email
   :data {:interaction-id interaction-id
          :artifact-id artifact-id
          :api-response data}
   :level "interaction-fatal"
   :message "Failed to retrieve email artifact data."})

(defn failed-to-create-outbound-email-interaction-err [interaction-body interaction-id data]
  {:code 10002
   :context :email
   :data {:interaction-body interaction-body
          :interaction-id interaction-id
          :api-response data}
   :level "interaction-fatal"
   :message "Failed to create outbound email interaction."})

(defn failed-to-send-agent-reply-started-err [interaction-id data]
  {:code 10003
   :context :email
   :data {:interaction-id interaction-id
          :api-response data}
   :level "warn"
   :message "Failed to send agent reply started signal. Reporting around this email may be affected."})

(defn failed-to-send-agent-no-reply-err [interaction-id data]
  {:code 10004
   :context :email
   :data {:interaction-id interaction-id
          :api-response data}
   :level "warn"
   :message "Failed to send agent no reply signal. Reporting around this email may be affected."})

(defn failed-to-send-email-reply-err [interaction-id data]
  {:code 10005
   :context :email
   :data {:interaction-id interaction-id
          :api-response data}
   :level "interaction-fatal"
   :message "Failed to send email reply. The API returned an error."})

(defn failed-to-get-attachment-url-err [interaction-id artifact-file-id artifact-id data]
  {:code 10006
   :context :email
   :data {:interaction-id interaction-id
          :artifact-file-id artifact-file-id
          :artifact-id artifact-id
          :api-response data}
   :level "error"
   :message "Failed to fetch attachment URL. The API returned an error."})

(defn failed-to-send-agent-cancel-reply-err [interaction-id data]
  {:code 10007
   :context :email
   :data {:interaction-id interaction-id
          :api-response data}
   :level "warn"
   :message "Failed to send agent cancelled reply signal. Reporting around this email may be affected."})

(defn failed-to-send-email-reply-no-artifact-err [interaction-id artifact-id]
  {:code 10008
   :context :email
   :data {:interaction-id interaction-id
          :artifact-id artifact-id}
   :level "error"
   :message "Failed to send email reply. There is no artifact for the interaction."})

(defn failed-to-get-user-err [resource-id data]
  {:code 11000
   :context :entities
   :data {:resource-id resource-id
          :api-response data}
   :level "error"
   :message "Failed to get specified user."})

(defn failed-to-get-user-list-err [data]
  {:Code 11001
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get user list."})

(defn failed-to-get-queue-err [queue-id data]
  {:code 11002
   :context :entities
   :data {:queue-id queue-id
          :api-response data}
   :level "error"
   :message "Failed to get specified queue."})

(defn failed-to-get-queue-list-err [data]
  {:code 11003
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get queue list."})

(defn failed-to-get-transfer-list-err [data]
  {:code 11004
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get transfer list."})

(defn failed-to-get-transfer-lists-err [data]
  {:code 11005
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get transfer lists."})

(defn failed-to-get-tenant-branding-err [data]
  {:code 11006
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get tenant branding."})

(defn failed-to-update-user-err [body resource-id data]
  {:code 11007
   :context :entities
   :data {:body body
          :resource-id resource-id
          :api-response data}
   :level "error"
   :message "Failed to update user."})

(defn reporting-batch-request-failed-err [batch-body api-response]
  {:code 12000
   :context :reporting
   :data {:batch-body batch-body
          :api-response api-response}
   :level "error"
   :message "Reporting batch request failed. Ceasing further polling."})

(defn failed-to-get-interaction-reporting-err [interaction-id data]
  {:code 12001
   :context :reporting
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to retrieve reporting information for the interaction id specified."})

(defn failed-to-get-contact-interaction-history-err [contact-id data]
  {:code 12002
   :context :reporting
   :data {:contact-id contact-id
          :api-response data}
   :level "error"
   :message "Failed to get interaction history for the contact id specified."})

(defn failed-to-get-available-stats-err [data]
  {:code 12003
   :context :reporting
   :data {:api-response data}
   :level "error"
   :message "Failed to get available stats."})

(defn failed-to-perform-stat-query-err [stat-query data]
  {:code 12004
   :context :reporting
   :data {:stat-query stat-query
          :api-response data}
   :level "error"
   :message "Failed to perform stat query."})

(defn failed-to-get-capacity-err [data]
  {:code 12005
   :context :reporting
   :data {:api-response data}
   :level "error"
   :message "Failed to get capacity."})

(defn failed-to-get-crm-interactions-err [id crm sub-type data]
  {:code 12006
   :context :reporting
   :data {:api-response data
          :crm crm
          :id id
          :sub-type sub-type}
   :level "error"
   :message "Failed to retrieve CRM Interaction information."})

(defn failed-to-retrieve-contact-layouts-list-err [data]
  {:code 13000
   :context :contacts
   :data {:api-response data}
   :level "error"
   :message "Failed to retrieve list of contact layouts. The API encountered an error."})

(defn failed-to-retrieve-contact-layout-err [layout-id data]
  {:code 13001
   :context :contacts
   :data {:layout-id layout-id
          :api-response data}
   :level "error"
   :message "Failed to retrieve specified contact layout. The API encountered an error."})

(defn failed-to-list-contact-attributes-err [data]
  {:code 13002
   :context :contacts
   :data {:api-response data}
   :level "error"
   :message "Failed to list contact attributes. The API encountered an error."})

(defn failed-to-merge-contacts-err [contact-ids data]
  {:code 13003
   :context :contacts
   :data {:contact-ids contact-ids
          :api-response data}
   :level "error"
   :message "Failed to merge contacts. The API encountered an error."})

(defn failed-to-delete-contact-err [contact-id data]
  {:code 13004
   :context :contacts
   :data {:contact-id contact-id
          :api-response data}
   :level "error"
   :message "Failed to delete contact. The API encountered an error."})

(defn failed-to-update-contact-err [contact-id attributes data]
  {:code 13005
   :context :contacts
   :data {:contact-id contact-id
          :attributes attributes
          :api-response data}
   :level "error"
   :message "Failed to update contact. The API encountered an error."})

(defn failed-to-create-contact-err [attributes data]
  {:code 13006
   :context :contacts
   :data {:attributes attributes
          :api-response data}
   :level "error"
   :message "Failed to create contact. The API encountered an error."})

(defn failed-to-search-contacts-err [query data]
  {:code 13007
   :context :contacts
   :data {:query query
          :api-response data}
   :level "error"
   :message "Failed to search contacts. The API encountered an error."})

(defn failed-to-list-all-contacts-err [data]
  {:code 13008
   :context :contacts
   :data {:api-response data}
   :level "error"
   :message "Failed to retrieve all contacts. The API encountered an error."})

(defn failed-to-get-contact-err [contact-id data]
  {:code 13009
   :context :contacts
   :data {:contact-id contact-id
          :api-response data}
   :level "error"
   :message "Failed to retrieve specified contact. The API encountered an error."})

(defn failed-to-save-logs-err [logs data]
  {:code 14000
   :context :logging
   :data {:logs logs
          :api-response data}
   :level "error"
   :message "Failed to save logs to API."})

(defn failed-to-send-salesforce-classic-assign-err [interaction-id data]
  {:code 15000
   :context :salesforce-classic
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to assign item. The API returned an error."})

(defn failed-to-update-salesforce-classic-interaction-tab-id-err [interaction-id]
  {:code 15001
   :context :salesforce-classic
   :data {:interaction-id interaction-id}
   :level "error"
   :message "Failed to update interaction tab id."})

(defn failed-to-send-salesforce-classic-unassign-err [interaction-id data]
  {:code 15002
   :context :salesforce-classic
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to unassign item. The API returned an error."})

(defn failed-to-focus-salesforce-classic-interaction-err [interaction-id]
  {:code 15003
   :context :salesforce-classic
   :data {:interaction-id interaction-id}
   :level "warn"
   :message "Failed to focus salesforce classic interaction."})

(defn failed-to-assign-salesforce-classic-item-to-interaction-err [interaction-id]
  {:code 15004
   :context :salesforce-classic
   :data {:interaction-id interaction-id}
   :level "warn"
   :message "Failed to assign item. Interaction already has been assigned to an item."})

(defn failed-to-unassign-salesforce-classic-item-from-interaction-err [interaction-id]
  {:code 15005
   :context :salesforce-classic
   :data {:interaction-id interaction-id}
   :level "warn"
   :message "Failed to unassign item. No item has been assigned."})

(defn failed-to-assign-salesforce-classic-item-no-interaction-err [interaction-id]
  {:code 15006
   :context :salesforce-classic
   :data {:interaction-id interaction-id}
   :level "warn"
   :message "Failed to assign/unassign item. Interaction id does not correspond to active interaction."})

(defn failed-to-assign-blank-salesforce-classic-item-err [interaction-id]
  {:code 15007
   :context :salesforce-classic
   :data {:interaction-id interaction-id}
   :level "warn"
   :message "Failed to assign item. Cannot assign blank active tab."})

(defn failed-to-send-salesforce-lightning-assign-err [interaction-id data]
  {:code 16000
   :context :salesforce-lightning
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to assign item. The API returned an error."})

(defn failed-to-update-salesforce-lightning-interaction-tab-id-err [interaction-id]
  {:code 16001
   :context :salesforce-lightning
   :data {:interaction-id interaction-id}
   :level "error"
   :message "Failed to assign item. The API returned an error."})

(defn failed-to-send-salesforce-lightning-unassign-err [interaction-id data]
  {:code 16002
   :context :salesforce-lightning
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to unassign item. The API returned an error."})

(defn failed-to-focus-salesforce-lightning-interaction-err [interaction-id]
  {:code 16003
   :context :salesforce-lightning
   :data {:interaction-id interaction-id}
   :level "warn"
   :message "Failed to focus salesforce lightning interaction."})

(defn failed-to-assign-salesforce-lightning-item-to-interaction-err [interaction-id]
  {:code 16004
   :context :salesforce-lightning
   :data {:interaction-id interaction-id}
   :level "warn"
   :message "Failed to assign item. Interaction already has been assigned to an item."})

(defn failed-to-unassign-salesforce-lightning-item-from-interaction-err [interaction-id]
  {:code 16005
   :context :salesforce-lightning
   :data {:interaction-id interaction-id}
   :level "warn"
   :message "Failed to unassign item. No item has been assigned."})

(defn failed-to-assign-salesforce-lightning-item-no-interaction-err [interaction-id]
  {:code 16006
   :context :salesforce-lightning
   :data {:interaction-id interaction-id}
   :level "warn"
   :message "Failed to assign/unassign item. Interaction id does not correspond to active interaction."})

(defn failed-to-assign-blank-salesforce-lightning-item-err [interaction-id]
  {:code 16007
   :context :salesforce-lightning
   :data {:interaction-id interaction-id}
   :level "warn"
   :message "Failed to assign item. Cannot assign blank active tab."})

(defn failed-to-init-zendesk-client-err [data]
  {:code 17000
   :context :zendesk
   :data {:api-response data}
   :level "error"
   :message "Failed to initialize Zendesk Client."})

(defn failed-to-focus-zendesk-interaction-err [interaction-id data]
  {:code 17001
   :context :zendesk
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to focus Zendesk interaction."})

(defn failed-to-set-zendesk-visibility-err [data]
  {:code 17002
   :context :zendesk
   :data {:api-response data}
   :level "error"
   :message "Failed to set visibility of Zendesk Toolbar."})

(defn failed-to-set-zendesk-dimensions-err [data]
  {:code 17003
   :context :zendesk
   :data {:api-response data}
   :level "error"
   :message "Failed to set dimensions of Zendesk Toolbar."})

(defn failed-to-send-zendesk-assign-err [interaction-id data]
  {:code 17004
   :context :zendesk
   :data {:interaction-id interaction-id
          :api-response data}
   :level "error"
   :message "Failed to assign item. The API returned an error."})
