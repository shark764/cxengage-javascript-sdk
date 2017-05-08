(ns cxengage-javascript-sdk.domain.errors)

(defn bad-sdk-init-opts-err []
  {:code 1000
   :level :error
   :message "Invalid SDK initialization options provided. Verify your values against the SDK documentation."})

(defn wrong-number-sdk-opts-err []
  {:code 1001
   :level :error
   :message "Incorrect number of arguments provided to SDK initialization. Verify your values against the SDK documentation."})

(defn required-module-failed-to-start-err []
  {:code 1002
   :level :fatal
   :message "A required SDK module failed to start, unable to initialize the SDK."})

(defn args-failed-spec-err []
  {:code 1003
   :level :error
   :message "The parameters object passed to the SDK function did not adhere to the spec defined. Verify your values against the SDK documentation."})

(defn wrong-number-of-sdk-fn-args-err []
  {:code 1004
   :level :error
   :message "Incorrect number of arguments passed to SDK function. All SDK functions support up to 2 arguments; the first being the parameters map, the second being an optional callback. Verify your values against the SDK documentation."})

(defn callback-isnt-a-function-err []
  {:code 1005
   :level :error
   :message "The callback you provided isn't a function. Verify your values against the SDK documentation."})

(defn params-isnt-a-map-err []
  {:code 1006
   :level :error
   :message "The value you provided for your parameters isn't an object. Verify your values against the SDK documentation."})

(defn service-unavailable-api-err []
  {:code 1007
   :level :fatal
   :message "The API returned a 503 error code and failed all retry attempts."})

(defn inernal-server-err []
  {:code 1008
   :level :error
   :message "The API encountered an internal error (500 status code)."})

(defn client-request-err []
  {:code 1009
   :level :error
   :message "The API rejected the values provided (400 status code). Verify your values against the SDK documentation."})

(defn insufficient-permissions-err []
  {:code 2000
   :level :error
   :message "You lack sufficient permissions in order to perform this action."})

(defn failed-to-get-session-config-err []
  {:code 2001
   :level :error
   :message "Failed to get user session config. The API returned an error."})

(defn failed-to-start-agent-session-err []
  {:code 2002
   :level :error
   :message "Failed to start an agent session. The API returned an error."})

(defn session-heartbeats-failed-err []
  {:code 2003
   :level :fatal
   :message "Session heartbeats failed. Unable to continue using agent session."})

(defn failed-to-change-state-err []
  {:code 2004
   :level :error
   :message "Failed to change agent state. The API returned an error."})

(defn invalid-extension-provided-err []
  {:code 2005
   :level :error
   :message "Invalid extension provided. Must be in the list of extensions provided via your user config. Unable to transition agent to a ready state."})

(defn failed-to-update-extension-err []
  {:code 2006
   :level :error
   :message "Failed to update user extension. Unable to transition agent to ready state."})

(defn invalid-reason-info-err []
  {:code 2007
   :level :error
   :message "Invalid reason info provided. Must be in the list of reasons provided via your user config. Unable to transition agent to a not ready"})

(defn no-microphone-access-error []
  {:code 8000
   :level :fatal
   :message "Failed to connect to Twilio. Microphone access must be enabled within your browser to utilize voice features."})
