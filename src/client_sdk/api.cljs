(ns client-sdk.api
  (:require [lumbajack.core :refer [log]]
            [client-sdk.api.auth :as auth]
            [client-sdk.api.session :as session]
            [client-sdk.api.reporting :as reporting]))

(defn assemble-api []
  (clj->js (merge {:session (session/api)
                   :auth (auth/api)
                   :logging (logging/api)
                   :reporting (reporting/api)}
                  (pubsub/api))))
