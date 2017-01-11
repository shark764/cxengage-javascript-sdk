(ns client-sdk.api
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [lumbajack.core]
            [client-sdk.state :as state]
            [client-sdk.api.auth :as auth]
            [client-sdk.api.session :as session]
            [client-sdk.api.logging :as logging]
            [client-sdk.pubsub :as pubsub]
            [client-sdk.api.reporting :as reporting]
            [client-sdk.api.interactions :as int]
            [client-sdk.api.interactions.messaging :as msg]
            [client-sdk.api.crud :as crud]))

(defn assemble-api []
  (let [api (merge {:session {:setActiveTenant session/set-active-tenant
                              :changeState session/change-state}
                    :auth {:login auth/login}
                    :interactions {:acceptInteraction int/accept-interaction
                                   :endInteraction int/end-interaction
                                   :messaging {:sendMessage msg/send-message}}
                    :subscribe pubsub/subscribe})]
    (if (= (state/get-consumer-type) :cljs)
      api
      (clj->js api))))
