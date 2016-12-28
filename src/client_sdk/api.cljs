(ns client-sdk.api
  (:require [lumbajack.core :refer [log]]
            [client-sdk.api.auth :as auth]
            [client-sdk.api.session :as session]
            [client-sdk.api.logging :as logging]
            [client-sdk.pubsub :as pubsub]
            [client-sdk.api.reporting :as reporting]
            [client-sdk.api.flow-interrupts :as flow]
            [client-sdk.api.crud :as crud]))

(defn assemble-api []
  (clj->js (merge {:session (session/api)
                   :auth (auth/api)
                   :logging (logging/api)
                   :crud (crud/api)
                   :reporting (reporting/api)
                   :pubsub (pubsub/api)})))
