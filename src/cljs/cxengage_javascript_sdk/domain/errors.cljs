(ns cxengage-javascript-sdk.domain.errors)

(defn bad-sdk-init-opts-err []
  {:code 1000
   :level "error"
   :message "Invalid SDK initialization options provided. Verify your values against the SDK documentation."})

(defn wrong-number-sdk-opts-err []
  {:code 1001
   :level "error"
   :message "Incorrect number of arguments provided to SDK initialization. Verify your values against the SDK documentation."})

(defn required-module-failed-to-start-err []
  {:code 1002
   :level "fatal"
   :message "A required SDK module failed to start, unable to initialize the SDK."})

(defn args-failed-spec-err []
  {:code 1003
   :level "error"
   :message "The parameters object passed to the SDK function did not adhere to the spec defined. Verify your values against the SDK documentation."})

(defn wrong-number-of-sdk-fn-args-err []
  {:code 1004
   :level "error"
   :message "Incorrect number of arguments passed to SDK function. All SDK functions support up to 2 arguments; the first being the parameters map, the second being an optional callback. Verify your values against the SDK documentation."})

(defn callback-isnt-a-function-err []
  {:code 1005
   :level "error"
   :message "The callback you provided isn't a function. Verify your values against the SDK documentation."})

(defn params-isnt-a-map-err []
  {:code 1006
   :level "error"
   :message "The value you provided for your parameters isn't an object. Verify your values against the SDK documentation."})

(defn service-unavailable-api-err []
  {:code 1007
   :level "fatal"
   :message "The API returned a 503 error code and failed all retry attempts."})

(defn internal-server-err []
  {:code 1008
   :level "error"
   :message "The API encountered an internal error (500 status code)."})

(defn client-request-err []
  {:code 1009
   :level "error"
   :message "The API rejected the request (400-range status code). Verify your values against the SDK documentation."})

(defn unknown-agent-notification-type-err []
  {:code 1010
   :level "error"
   :message "Received an unknown agent notification type. Unable to parse agent notification."})

(defn resource-not-found-err []
  {:code 1011
   :level "error"
   :message "API returned 404. Check your URL and if the resources exists on the system."})

(defn insufficient-permissions-err []
  {:code 2000
   :level "error"
   :message "You lack sufficient permissions in order to perform this action."})

(defn failed-to-get-session-config-err []
  {:code 2001
   :level "error"
   :message "Failed to get user session config. The API returned an error."})

(defn failed-to-start-agent-session-err []
  {:code 2002
   :level "error"
   :message "Failed to start an agent session. The API returned an error."})

(defn session-heartbeats-failed-err []
  {:code 2003
   :level "fatal"
   :message "Session heartbeats failed. Unable to continue using agent session."})

(defn failed-to-change-state-err []
  {:code 2004
   :level "error"
   :message "Failed to change agent state. The API returned an error."})

(defn invalid-extension-provided-err []
  {:code 2005
   :level "error"
   :message "Invalid extension provided. Must be in the list of extensions provided via your user config. Unable to transition agent to a ready state."})

(defn failed-to-update-extension-err []
  {:code 2006
   :level "error"
   :message "Failed to update user extension. Unable to transition agent to ready state."})

(defn invalid-reason-info-err []
  {:code 2007
   :level "error"
   :message "Invalid reason info provided. Must be in the list of reasons provided via your user config. Unable to transition agent to a not ready"})

(defn login-failed-err []
  {:code 3000
   :level "error"
   :message "Login attempt failed."})

(defn logout-failed-err []
  {:code 3001
   :level "error"
   :message "Logout attempt failed"})

(defn active-interactions-err []
  {:code 4000
   :level "error"
   :message "Unable to perform this action as there are interactions still active."})

(defn work-offer-expired-err []
  {:code 4001
   :level "error"
   :message "Attempted to accept a work offer that is already expired."})

(defn failed-to-refresh-sqs-integration-err []
  {:code 5000
   :level "fatal"
   :message "Failed to refresh SQS Queue object. Unable to continue agent notification polling."})

(defn failed-to-retrieve-messaging-history-err []
  {:code 6000
   :level "error"
   :message "Failed to retrieve messaging interaction history."})

(defn failed-to-retrieve-messaging-metadata-err []
  {:code 6001
   :level "error"
   :message "Failed to retrieve messaging interaction metadata."})

(defn failed-to-refresh-twilio-integration-err []
  {:code 7000
   :level "fatal"
   :message "Failed to refresh Twilio credentials."})

(defn failed-to-send-digits-invalid-interaction-err []
  {:code 7001
   :level "error"
   :message "Unable to send digits to specified interaction. Interaction must be active and of type voice."})

(defn no-twilio-integration-err []
  {:code 7002
   :level "error"
   :message "Unable to perform action - no twilio integration set up"})

(defn no-microphone-access-error []
  {:code 8000
   :level "fatal"
   :message "Failed to connect to Twilio. Microphone access must be enabled within your browser to utilize voice features."})

(defn failed-to-send-twilio-digits-err []
  {:code 8001
   :level "error"
   :message "Failed to send digits via Twilio. Potentially invalid dial tones."})

(defn failed-to-connect-to-mqtt-err []
  {:code 9000
   :level "fatal"
   :message "Unable to connect to MQTT."})

(defn failed-to-create-email-reply-artifact-err []
  {:code 10000
   :level "error"
   :message "Failed to create email artifact for email reply."})

(defn failed-to-retrieve-email-artifact-err []
  {:code 10001
   :level "error"
   :message "Failed to retrieve email artifact data."})

(defn failed-to-create-outbound-email-interaction-err []
  {:code 10002
   :level "error"
   :message "Failed to create outbound email interaction."})

(defn reporting-batch-request-failed-err []
  {:code 12000
   :level "error"
   :message "Reporting batch request failed. Ceasing further polling."})
