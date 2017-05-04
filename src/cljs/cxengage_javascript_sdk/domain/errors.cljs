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

(defn failed-to-update-extension-err []
  {:code 2006
   :level :error
   :message "Failed to update user extension. Unable to go ready."})

(defn invalid-extension-provided []
  {:code 2005
   :level :error
   :message "Invalid extension provided. Must be in the list of extensions provided via your user config."})

;;2000 - insufficient permissions to perform this action based on your role on this tenant
;;2001 - failed to get config for user on this tenant, unable to start session
;;2002 - failed to start users session
;;2003 - session heartbeat failed
;;2004 - failed to change presence state
;;2005 - passed an invalid extension, unable to go ready / set your active extension
;;2006 - attempt to update the users extension in order to go ready failed
;;2007 -






(defn token-error [a] nil)
(defn wrong-number-of-args-error [] {:err "wrong # of args"})
(defn missing-required-permissions-error [] {:err "missing required perms"})
(defn invalid-args-error
  ([] {:err "invalid args"})
  ([a] {:err "invalid args"}))
(defn api-error
  ([] {:err "api error"})
  ([a] {:err "api error"}))
(defn no-entity-found-for-specified-id [] {:err "no entity of that id"})
(defn incorrect-disposition-selected [] {:err "no dispo"})
(defn invalid-artifact-file [] {:err "no artifact file"})
(defn no-microphone-access-error
  ([] {:err "no mic access"})
  ([a] {:err "no mic access"}))
(defn not-a-valid-extension [] {:err "no valid ext"})
(defn invalid-logging-level-specified-error [] {:err "invalid log level"})
