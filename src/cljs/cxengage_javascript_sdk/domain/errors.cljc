(ns cxengage-javascript-sdk.domain.errors)


;;1000 - invalid sdk initialization error (bad options)
;;1001 - invalid sdk initialization error (wrong # of args given - only 1 supported)
;;1002 - a required SDK module failed to start - shutting down
;;1003 - the parameters map passed to the SDK fn didn't meet the spec - check the docs
;;1004 - the # of parameters passed to the SDK fn isn't supported - only 2 (params map, callback)
;;1005 - the value provided for callback isn't a function
;;1006 - the value provided for params map wasn't of type object
;;1007 - failed to perform api request after 3 retries (service unavailable)
;;1008 - api request failed due to internal server error (500)
;;1009 - api request failed due to invalid client request (400)

;;2000 - insufficient permissions to perform this action based on your role on this tenant
;;2001 - failed to get config for user on this tenant, unable to start session
;;2002 - failed to start users session
;;2003 - session heartbeat failed
;;2004 - failed to change presence state
;;2005 - passed an invalid extension, unable to go ready / set your active extension
;;2006 - attempt to update the users extension in order to go ready failed
;;2007 -






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
