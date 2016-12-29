(ns client-sdk.api
  (:require [lumbajack.core :refer [log]]
            [client-sdk.state :as state]
            [client-sdk.api.auth :as auth]
            [client-sdk.api.session :as session]
            [client-sdk.api.logging :as logging]
            [client-sdk.pubsub :as pubsub]
            [client-sdk.api.reporting :as reporting]
            [client-sdk.api.interactions :as int]
            [client-sdk.api.crud :as crud]))

(defn assemble-api []
  (let [api (merge {:session (session/api)
                    :auth (auth/api)
                    :logging (logging/api)
                    :crud (crud/api)
                    :interactions (int/api)
                    :reporting (reporting/api)
                    :pubsub (pubsub/api)})]
    (if (= (state/get-consumer-type) :cljs)
      api
      (clj->js api))))
