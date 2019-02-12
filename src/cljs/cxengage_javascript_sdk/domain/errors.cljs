(ns cxengage-javascript-sdk.domain.errors)

(defn bad-sdk-init-opts-err
  "## SDK initialization error
   This error is commonly received if the initialize function was provided with invalid
   parameters. The available parameters are listed as following:

   - baseUrl
   - crmModule
   - reporitngRefreshRate
   - logLevel
   - environment

   ``` javascript
    CxEngage.initialize({
      baseUrl: 'http://api.cxengage.net/v1/',
      environment: 'dev',
      reportingRefreshRate: 3000,
      logLevel: 'info',
      crmModule: 'salesforce-classic'
    });
   ```"
  []
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

(defn required-module-failed-to-start-err [module]
  {:code 1002
   :context :general
   :data {:module module}
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

(defn failed-to-get-session-config-err
  "**Error Code:** 2001
   Message: Failed to get user session config. The API returned an error.

   This error can be thrown if an Agent attempts to select their tenant and the
   API returns an error.

   **Solution:** Refresh the browser and attempt to select the tenant once more.
   If it continues to fail, ensure that the tenant's integrations are configured
   properly.

   Support Notes: The Config route returns integration information and is an
   expensive API request. The API could be timing out trying to fetch integration
   data."
  [data]
  {:code 2001
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to get user session config. The API returned an error."})

(defn failed-to-start-agent-session-err
  "**Error Code:** 2002
   Message: Failed to start an agent session. The API returned an error.

   This error can be thrown if an Agent attempts to select their tenant and the
   API returns an error.

   **Solution:** Refresh the browser and attempt to select the tenant once more.

   Support Notes: This error may indicate an issue with Presence."
  [data]
  {:code 2002
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to start an agent session. The API returned an error."})

(defn session-heartbeats-failed-err
  "**Error Code:** 2003
   Message: Session heartbeats failed. Unable to continue using agent session.

   This error can be thrown if an Agent attempts to select their tenant and the
   API returns an error. Alternatively, this error can be thrown if an Agent's
   session was forcefully expired.

   **Solution:** Refresh the browser and login again.

   Support Notes: The error indicates the Agent logged in elsewhere, and as a
   result, their session was forcefully expired. Alternatively, this error may
   indicate an issue with Presence."
  [data]
  {:code 2003
   :context :session
   :data {:api-response data}
   :level "session-fatal"
   :message "Session heartbeats failed. Unable to continue using agent session."})

(defn failed-to-change-state-err
  "**Error Code:** 2004
   Message: Failed to change agent state. The API returned an error.

   This error can be thrown if an Agent attempts to change their state and the API
   request returns an error.

   **Solution:** Attempt to change state again shortly after it fails, if it
   continues to fail try restarting the browser.

   Support Notes: In the case of a 500 error, this would be a problem with
   Presence."
  [data]
  {:code 2004
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to change agent state. The API returned an error."})

(defn invalid-extension-provided-err
  "**Error Code:** 2005
   Message: Invalid extension provided. Must be in the list of extensions provided via your user config. Unable to transition agent to a ready state.

   This error can be thrown if an Agent attempts to go ready with an invalid
   extension.

   **Solution:** Ensure the Agent's extensions are properly configured, refresh
   the browser and attempt to go ready once more.

   Support Notes: this error could indicate misconfigured extensions, or the SDK
   being out of sync with the user's extension data. Relogging should fix this
   issue."
  [data]
  {:code 2005
   :context :session
   :data {:valid-extensions data}
   :level "error"
   :message "Invalid extension provided. Must be in the list of extensions provided via your user config. Unable to transition agent to a ready state."})

(defn failed-to-update-extension-err
  "**Error Code:** 2006
   Message: Failed to update user extension. Unable to transition agent to ready state.

   This error can be thrown if an Agent attempts to go ready with a new extension,
   and the API returns an error while trying to update their active extension.

   **Solution:** If this error occurs after refreshing the browser and logging in
   again, have an administrator change the Agent's primary extension manually."
  [data]
  {:code 2006
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to update user extension. Unable to transition agent to ready state."})

(defn invalid-reason-info-err
  "**Error Code:** 2007
   Message: Invalid reason info provided. Must be in the list of reasons
   provided via your user config. Unable to transition agent to a not ready.

   This error can be thrown if the reason they are attempting to go not ready
   with was not included in their user data on login.

   **Solution:** In the event this error does occur, try changing state without
   a reason code. Once this is done, attempt to switch to the desired reason code.

   Support Notes: The SDK's internal list of available reason codes for the user
   to switch to refreshes on state change. Therefore, switching to not-ready
   without a reason list should refresh that list allowing the user to then switch
   to the desired code."
  [data]
  {:code 2007
   :context :session
   :data {:valid-reasons data}
   :level "error"
   :message "Invalid reason info provided. Must be in the list of reasons provided via your user config. Unable to transition agent to a not ready"})

(defn failed-to-set-direction-err
  "**Error Code:** 2008
   Message: Failed to set user direction. The API returned an error.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 2008
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to set user direction. The API returned an error."})

(defn failed-to-get-user-extensions-err
  "**Error Code:** 2009
   Message: Failed to go ready; unable to retrieve user extensions. The API returned an error.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 2009
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to go ready; unable to retrieve user extensions. The API returned an error."})

(defn failed-to-get-tenant-err
  "**Error Code:** 2010
   Message: Failed to select tenant; unable to retrieve tenant data. The API returned an error.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 2010
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to select tenant; unable to retrieve tenant data. The API returned an error."})

(defn failed-to-get-region-err
  "**Error Code:** 2011
   Message: Failed to select tenant; unable to retrieve region data. The API returned an error.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 2011
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to select tenant; unable to retrieve region data. The API returned an error."})

(defn failed-to-get-tenant-details-err
  "**Error Code:** 2012
   Message: Failed to retrieve user's tenant details. The API returned an error.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 2012
   :context :session
   :data {:api-response data}
   :level "error"
   :message "Failed to retrieve user's tenant details. The API returned an error."})

(defn login-failed-token-request-err
  "**Error Code:** 3000
   Message: Login attempt failed. Unable to retrieve token.

   This error can be thrown if a User attempts to login and the API request
   returns an error. The most common cause for this would be incorrect
   credentials.

   **Solution:** Verify that the credentials being used are correct."
  [data]
  {:code 3000
   :context :authentication
   :data {:api-response data}
   :level "error"
   :message "Login attempt failed. Unable to retrieve token."})

(defn logout-failed-err
  "**Error Code:** 3001
   Message: Logout attempt failed.

   This error can be thrown if an Agent attempts to logout and the API request
   to update the Agent's state to 'offline' fails. This would likely be caused
   by an expired session.

   **Solution:** The simplest solution would be to close the browser, as the session
   has already ended.

   Support Notes: In the case of a 500 error, this would be a problem with
   Presence."
  [data]
  {:code 3001
   :context :authentication
   :data {:api-response data}
   :level "error"
   :message "Logout attempt failed."})

(defn login-failed-login-request-err
  "**Error Code:** 3002
   Message: Login attempt failed. Login request failed.

   This error is typically thrown if the token being used is invalid or expired.

   **Solution:** Refresh the page, and attempt to log in once more."
  [data]
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

(defn failed-to-get-auth-info-err
  "**Error Code:** 3005
   Message: Failed to retrieve SSO authentication information.

   This error can be thrown if we are unable to find any SSO information
   associated with their email address or tenant ID / identity provider ID
   combination.

   **Solution:** Ensure that the email or tenant being used is properly associated
   with a valid identity provider, and that the SAML provider is configured
   correctly."
  [data]
  {:code 3005
   :context :authentication
   :data data
   :level "error"
   :message "Failed to retrieve SSO authentication information."})

(defn failed-to-update-default-tenant-err [data]
  {:code 3006
   :context :authentication
   :data data
   :level "error"
   :message "Failed to update default tenant."})

(defn active-interactions-err
  "**Error Code:** 4000
   Message: Unable to perform this action as there are interactions still active.

   This error can be thrown if an Agent attempts to logout while there are still
   active / unfinished interactions.

   **Solution:** Change your status to a not-ready state, complete/end any remaining
   interactions. Ensure all scripts have been completed, and wrap-up has ended."
  [data]
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

(defn failed-to-acknowledge-flow-action-err [interaction-id data message]
  {:code 4002
   :context :interaction
   :data {:interaction-id interaction-id
          :api-response data
          :flow-message message}
   :level "error"
   :message "Failed to acknowledge action."})

(defn failed-to-end-interaction-err [interaction-id data]
  {:code 4003
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level (if (= 404 (:status data))
            "interaction-fatal"
            "error")
   :message "Failed to end interaction."})

(defn failed-to-accept-interaction-err [interaction-id data]
  {:code 4004
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level (if (= 404 (:status data))
            "interaction-fatal"
            "error")
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
   :level (if (= 404 (:status data))
            "interaction-fatal"
            "error")
   :message "Failed to end wrapup."})

(defn failed-to-deselect-disposition-err [interaction-id data]
  {:code 4012
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level (if (= 404 (:status data))
            "interaction-fatal"
            "error")
   :message "Failed to deselect disposition code."})

(defn failed-to-select-disposition-err [interaction-id data]
  {:code 4013
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level (if (= 404 (:status data))
            "interaction-fatal"
            "error")
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
   :level (if (= 404 (:status data))
            "interaction-fatal"
            "error")
   :message "Failed to send script reply."})

(defn failed-to-send-custom-interrupt-err [interaction-id data]
  {:code 4019
   :context :interaction
   :data {:api-response data
          :interaction-id interaction-id}
   :level (if (= 404 (:status data))
            "interaction-fatal"
            "error")
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

(defn failed-to-delete-sqs-message [error]
  "**Error Code:** 5001
   Message: Failed to delete sqs message. Will likely receive the previous message again.

   This error occurs when the AWS SQS SDK is unable to remove the SQS message from the queue.
   This will likely result in the SQS message coming in again.

   **Solution:** This should only happen once in a while. If it does, it can probably be ignored.
   If it happens more frequently, you may need to inviestigate more into the SQS settings.

   Support Notes: This is most likely caused by internet connectivity issues."
  {:code 5001
   :context :sqs
   :data {:error error}
   :level "warn"
   :log-level "error"
   :message "Failed to delete sqs message. Will likely receive the previous message again."})

(defn failed-to-receive-sqs-message [error]
  "**Error Code:** 5002
    Message: Failed to receive sqs message. Will retry in 2 seconds.

    This error occurs when the AWS SQS SDK is unable to receive a message.
    CxEngage SDK will retry again every 2 seconds until it properly receives it.

    **Solution:** This should only happen once in a while. If it does, it can probably be ignored.
    If it happens more frequently, you may need to inviestigate more into the SQS settings.

    Support Notes: This is most likely caused by internet connectivity issues."
  {:code 5002
   :context :sqs
   :data {:error error}
   :level "warn"
   :log-level "error"
   :message "Failed to receive sqs message. Will retry in 2 seconds."})
  
(defn sqs-uncaught-exception [error]
  {:code 5003
   :context :sqs
   :data {:error error}
   :level "error"
   :message "An uncaught exception was thrown. You may need to refresh the browser if you are not able to perform any actions or receive any work."})

(defn sqs-loop-ended [restart-count]
  {:code 5004
   :context :sqs
   :data {:restart-count restart-count}
   :level "warn"
   :log-level "error"
   :message "Exited the SQS loop. This is likely because the session ended, but could indicate an unintended exit from the loop."})

(defn failed-to-process-sqs-message [sqs-info]
  {:code 5005
   :context :sqs
   :data sqs-info
   :level "error"
   :message "Failed to process message. You may need to refresh the browser if you are not able to perform any actions or receive any work."})

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
  {:code 6004
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
   :level "interaction-error"
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
   :level "interaction-error"
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

(defn failed-to-start-silent-monitoring-no-extension [interaction-id extensions]
  {:code 7025
   :context :voice
   :data {:interaction-id interaction-id
          :extensions extensions}
   :level "error"
   :message "There is no extension to use to start silent monitoring ."})

(defn no-microphone-access-err [error]
  {:code 8000
   :context :twilio
   :data {:error error}
   :level "error"
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

(defn mqtt-connection-lost-err [data]
  {:code 9001
   :context :mqtt
   :data data
   :level "warn"
   :log-level "error"
   :message "The connection to MQTT has been lost."})

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
   :log-level "error"
   :message "Failed to send agent reply started signal. Reporting around this email may be affected."})

(defn failed-to-send-agent-no-reply-err [interaction-id data]
  {:code 10004
   :context :email
   :data {:interaction-id interaction-id
          :api-response data}
   :level "warn"
   :log-level "error"
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

(defn failed-to-get-user-err
  "**Error Code:** 11000
   Message: Failed to get specified user.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the value passed into the sdk function was correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [resource-id data]
  {:code 11000
   :context :entities
   :data {:resource-id resource-id
          :api-response data}
   :level "error"
   :message "Failed to get specified user."})

(defn failed-to-get-users-err
  "**Error Code:** 11001
   Message: Failed to get user list.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11001
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

(defn failed-to-update-user-err
  "**Error Code:** 11007
   Message: Failed to update user.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11007
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update user."})

(defn failed-to-get-dashboards-list-err
  "**Error Code:** 11008
   Message: Failed to get dashboards list.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11008
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get dashboards list."})

(defn failed-to-get-list-err [data]
  {:code 11009
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get list."})

(defn failed-to-get-list-item-err [data]
 {:code 11010
  :context :entities
  :data {:api-response data}
  :level "error"
  :message "Failed to get list item."})

(defn failed-to-get-lists-err [data]
 {:code 11011
  :context :entities
  :data {:api-response data}
  :level "error"
  :message "Failed to get lists."})

(defn failed-to-create-list-err [data]
  {:code 11012
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to create list."})

(defn failed-to-create-list-item-err [data]
 {:code 11013
  :context :entities
  :data {:api-response data}
  :level "error"
  :message "Failed to create list item."})

(defn failed-to-update-list-err [data]
 {:code 11014
  :context :entities
  :data {:api-response data}
  :level "error"
  :message "Failed to update list."})

(defn failed-to-update-list-item-err [data]
 {:code 11015
  :context :entities
  :data {:api-response data}
  :level "error"
  :message "Failed to update list item."})

(defn failed-to-delete-list-item-err [data]
  {:code 11016
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to delete list item."})

(defn failed-to-get-list-types-err [data]
 {:code 11017
  :context :entities
  :data {:api-response data}
  :level "error"
  :message "Failed to get lists types."})

(defn failed-to-download-list-err [data]
 {:code 11018
  :context :entities
  :data {:api-response data}
  :level "error"
  :message "Failed to download list."})

(defn failed-to-upload-list-err [data]
 {:code 11019
  :context :entities
  :data {:api-response data}
  :level "error"
  :message "Failed to upload list."})

(defn failed-to-get-email-types-err [data]
  {:code 11020
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get email types."})

(defn failed-to-get-email-templates-err [data]
  {:code 11021
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get email templates."})

(defn failed-to-create-email-template-err [data]
  {:code 11022
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to create email template."})

(defn failed-to-update-email-template-err [data]
  {:code 11023
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update email template."})

(defn failed-to-delete-email-template-err [data]
  {:code 11024
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to delete email template."})

(defn failed-to-get-groups-err
  "**Error Code:** 11025
   Message: Failed to get groups.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11025
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get groups."})

(defn failed-to-get-skills-err
  "**Error Code:** 11026
   Message: Failed to get skills.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11026
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get skills."})

(defn failed-to-get-artifacts-err [data]
  {:code 11027
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get artifacts."})

(defn failed-to-get-artifact-err [data]
  {:code 11028
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get artifact."})

(defn failed-to-get-tenant-protected-branding-err [data]
  {:code 11029
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get tenant protected branding."})

(defn failed-to-create-outbound-identifier-err [data]
  {:code 11030
    :context :entities
    :data {:api-response data}
    :level "error"
    :message "Failed to create outbound identifier."})

(defn failed-to-get-outbound-identifiers-err [data]
  {:code 11031
    :context :entities
    :data {:api-response data}
    :level "error"
    :message "Failed to get outbound identifiers."})

(defn failed-to-update-outbound-identifier-err [data]
  {:code 11032
    :context :entities
    :data {:api-response data}
    :level "error"
    :message "Failed to update outbound identifier."})

(defn failed-to-get-outbound-identifier-lists-err [data]
  {:code 11033
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get outbound identifier lists."})

(defn failed-to-create-outbound-identifier-list-err
  "**Error Code:** 11034
   Message: Failed to create outbound identifier list.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function are correct.

   **Solution:** Check your browsers dev tools console for additional error information."
  [data]
  {:code 11034
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to create outbound identifier list."})

(defn failed-to-update-outbound-identifier-list-err [data]
  {:code 11035
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update outbound identifier list."})

(defn failed-to-get-outbound-identifier-list-err [data]
  {:code 11036
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get outbound identifier list."})

(defn failed-to-delete-outbound-identifier-err [data]
  {:code 11037
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to delete outbound identifier."})

(defn failed-to-add-outbound-identifier-list-member-err [data]
  {:code 11038
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to add outbound identifier to list."})

(defn failed-to-remove-outbound-identifier-list-member-err [data]
  {:code 11039
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to remove outbound identifier from list."})

(defn failed-to-get-custom-metrics-err
  "**Error Code:** 11040
   Message: Failed to get custom metrics list.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11040
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get custom metrics list."})

(defn failed-to-get-custom-metric-err
  "**Error Code:** 11041
   Message: Failed to get custom metric.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the value passed into the sdk function is correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11041
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get custom metric."})

(defn failed-to-update-custom-metric-err
  "**Error Code:** 11042
   Message: Failed to update custom metric.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function are correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11042
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update custom metric."})

(defn failed-to-get-flows-err
  "**Error Code:** 11043
   Message: Failed to get flows.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11043
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get flows."})

(defn failed-to-get-roles-err
  "**Error Code:** 11044
   Message: Failed to get roles.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11044
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get roles."})

(defn failed-to-get-integrations-err
  "**Error Code:** 11045
   Message: Failed to get integrations.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11045
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get integrations."})

(defn failed-to-get-capacity-rules-err
  "**Error Code:** 11046
   Message: Failed to get capacity rules.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11046
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get capacity rules."})

(defn failed-to-get-reasons-err
  "**Error Code:** 11047
   Message: Failed to get presence reasons.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11047
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get presence reasons."})

(defn failed-to-get-reason-lists-err
  "**Error Code:** 11048
   Message: Failed to get presence reasons lists.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11048
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get presence reason lists."})

(defn failed-to-get-permissions-err
  "**Error Code:** 11049
   Message: Failed to get permissions.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11049
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get permissions."})

(defn failed-to-create-role-err
  "**Error Code:** 11050
   Message: Failed to create role.

   This error is usually due to an unexpected status code returned from the API.
   Validate the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information."
  [data]
  {:code 11050
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to create role."})

(defn failed-to-update-role-err
  "**Error Code:** 11051
   Message: Failed to get integrations.

   This error is usually due to an unexpected status code returned from the API.
   Validate the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11051
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update role."})

(defn failed-to-get-historical-report-folders-err
  "**Error Code:** 11052
   Message: Failed to get historical report folders.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11052
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get historical report folders."})

(defn failed-to-get-data-access-reports-err
  "**Error Code:** 11053
   Message: Failed to get all data access reports.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11053
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get all data access reports."})

(defn failed-to-get-data-access-report-err
  "**Error Code:** 11054
   Message: Failed to get data access report.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the value passed into the sdk function was correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11054
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get data access report."})

(defn failed-to-create-data-access-report-err
  "**Error Code:** 11055
   Message: Failed to create data access report.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11055
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to create data access report."})

(defn failed-to-update-data-access-report-err
  "**Error Code:** 11056
   Message: Failed to update data access report.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11056
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update data access report."})

(defn failed-to-get-skill-err
  "**Error Code:** 11057
   Message: Failed to get skill.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the value passed into the sdk function was correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11057
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get skill."})

(defn failed-to-create-skill-err
  "**Error Code:** 11058
   Message: Failed to create skill.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11058
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to create skill."})

(defn failed-to-update-skill-err
  "**Error Code:** 11059
   Message: Failed to update skill.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11059
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update skill."})

(defn failed-to-get-group-err
  "**Error Code:** 11060
   Message: Failed to get group.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the value passed into the sdk function was correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11060
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get group."})

(defn failed-to-create-group-err
  "**Error Code:** 11061
   Message: Failed to create group.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11061
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to create group."})

(defn failed-to-update-group-err
  "**Error Code:** 11062
   Message: Failed to update group.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11062
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update group."})

(defn failed-to-get-platform-roles-err
  "**Error Code:** 11063
   Message: Failed to get all platform roles predefined.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11063
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get all platform roles predefined."})

(defn failed-to-dissociate-err
  "**Error Code:** 11064
   Message: Failed to dissociate entity.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11064
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to dissociate entity."})

(defn failed-to-create-user-err
  "**Error Code:** 11065
   Message: Failed to create user.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11065
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to create user."})

(defn failed-to-associate-err
  "**Error Code:** 11066
   Message: Failed to associate entity.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11066
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to associate entity."})


(defn failed-to-get-data-access-member-err
  "**Error Code:** 11067
   Message: Failed to get data access member.
   This error is usually due to an unexpected status code returned from the API.
   Validate that the value passed into the sdk function was correct.
   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11067
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get data access member."})


(defn failed-to-get-user-outbound-identifier-lists-err
  "**Error Code:** 11068
    Message: Failed to get the user's outbound identifiers list.

    This error is usually due to an unexpected status code returned from the API.
    Validate that the values passed into the sdk function were correct.

    **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11068
   :context :entities
   :data {:api-response data}
   :level "error"

   :message "Failed to get user's outbound identifier lists."})

(defn failed-to-get-entity-err
  "**Error Code:** 11068
   Message: Failed to get entity.
   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.
   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11069
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get entity."})

(defn failed-to-get-message-templates-err
  "**Error Code:** 11068
   Message: Failed to get message templates.
   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.
   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11070
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get message templates."})

(defn failed-to-update-platform-user-err
  "**Error Code:** 11071
   Message: Failed to update platform user.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11071
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update platform user."})

(defn failed-to-get-platform-user-err
  "**Error Code:** 11072
   Message: Failed to get platform user.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11072
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get platform user."})

(defn failed-to-get-identity-providers-err
  "**Error Code:** 11073
   Message: Failed to get identity providers.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11073
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get identity providers."})

(defn failed-to-update-users-capacity-rule-err
  "**Error Code:** 11074
   Message: Failed to update users capacity rule.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11074
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update users capacity rule."})

(defn failed-to-update-user-skill-member-err
  "**Error Code:** 11075
   Message: Failed to update skill's proficiency of user.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the values passed into the sdk function were correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11075
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update skill's proficiency of user."})

(defn failed-to-get-platform-user-email-err
  "**Error Code:** 11076
  Failed to get user details searching by email address.

    The email address specified as parameter does not exist within the platform. Validate the values passed to the SDK to make sure they are correct.

    **Solution:** Check your browsers dev tools console for additional info."
  [data]
  {:code 11076
    :context :entities
    :data {:api-response data}
    :level "error"
    :message "Failed to get user details searching by email address."})
  
(defn failed-to-update-reason-err
  "**Error Code:** 11077
   Message: Failed to update reason.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11077
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update reason."})

(defn failed-to-create-reason-err
  "**Error Code:** 11078
   Message: Failed to create reason.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11078
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to create reason."})

(defn failed-to-get-role-err
  "**Error Code:** 11079
   Message: Failed to get role.

   This error is usually due to an unexpected status code returned from the API.
   Validate that the value passed into the sdk function was correct.

   **Solution:** Check your browsers dev tools console for additional error information"
  [data]
  {:code 11079
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get role."})

(defn failed-to-create-reason-list-err
  "**Error Code:** 11080
   Message: Failed to create reason list.

   This error is usually due to an unexpected status code returned from the API.

   Validate that the values passed into the SDK function were correct.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11080
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to create reason list."})

(defn failed-to-update-reason-list-err
  "**Error Code:** 11081
   Message: Failed to update reason list.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11081
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update reason list."})

(defn failed-to-get-reason-err
  "**Error Code:** 11082
  Message: Failed to get a reason.

  This error is usually due to an unexpected status code returned from the API.

  **Solution:** Check network tab for additional error information."
  [data]
  {:code 11082
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get a Reason."})

(defn failed-to-get-reason-list-err
  "**Error Code:** 11083
    Message: Failed to get a Reason List.

    This error is usually due to an unexpected status code returned from the API.

    **Solution:** Check network tab for additional error information."
  [data]
  {:code 11083
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get a Reason."})

(defn failed-to-get-flow-err
  "**Error Code:** 11084
    Message: Failed to get a Flow.

    This error is usually due to an unexpected status code returned from the API.

    **Solution:** Check network tab for additional error information."
  [data]
  {:code 11084
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get a Flow."})

(defn failed-to-create-flow-err
  "**Error Code:** 11085
   Message: Failed to create/copy flow with draft.

   This error is usually due to an unexpected status code returned from the API.

   Validate that the values passed into the SDK function were correct.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11085
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to create/copy flow with draft."})

(defn failed-to-create-flow-draft-err
  "**Error Code:** 11086
   Message: Failed to create draft for flow.

   This error is usually due to an unexpected status code returned from the API.

   Validate that the values passed into the SDK function were correct.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11086
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to create draft for flow."})

(defn failed-to-update-flow-err
  "**Error Code:** 11087
   Message: Failed to update flow.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11087
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update flow."})

(defn failed-to-remove-flow-draft-err
  "**Error Code:** 11088
   Message: Failed to remove draft from flow.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11088
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to remove draft from flow."})

(defn failed-to-get-dispositions-err
  "**Error Code:** 11089
   Message: Failed to get dispositions.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11089
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get dispositions."})

(defn failed-to-get-disposition-err
  "**Error Code:** 11090
   Message: Failed to get disposition.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11090
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to get disposition."})

(defn failed-to-create-disposition-err
  "**Error Code:** 11091
   Message: Failed to create disposition.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11091
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to create disposition."})

(defn failed-to-update-disposition-err
  "**Error Code:** 11092
   Message: Failed to update disposition.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code 11092
   :context :entities
   :data {:api-response data}
   :level "error"
   :message "Failed to update disposition."})

;;hygen-insert-before-11000s

(defn reporting-batch-request-failed-err [batch-body api-response]
  {:code 12000
   :context :reporting
   :data {:batch-body batch-body
          :api-response api-response}
   :level "error"
   :message "Reporting batch request failed."})

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

(defn failed-to-perform-bulk-stat-query-err [stats response]
  {:code 12007
   :context :reporting
   :data {:api-response response
          :stats stats}
   :level "error"
   :message "Failed to perform bulk stat query."})

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

(defn failed-to-get-current-salesforce-classic-user-id-err [error]
  {:code 15008
   :context :salesforce-classic
   :data {:error error}
   :level "error"
   :message "Failed to get current salesforce classic user ID. Managed package may not have been installed or not be the correct version."})

(defn failed-to-get-current-salesforce-classic-org-id-err [error]
  {:code 15009
   :context :salesforce-classic
   :data {:error error}
   :level "error"
   :message "Failed to get current salesforce classic organization ID. Managed package may not have been installed or not be the correct version."})

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

(defn failed-to-get-current-salesforce-lightning-user-id-err [error]
  {:code 16008
   :context :salesforce-lightning
   :data error
   :level "error"
   :message "Failed to retrieve Salesforce Lightning user id."})

(defn failed-to-get-current-salesforce-lightning-org-id-err [error]
  {:code 16009
   :context :salesforce-lightning
   :data error
   :level "error"
   :message "Failed to get Salesforce Lightning organization ID. Managed package may not have been installed or not be the correct version."})

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
