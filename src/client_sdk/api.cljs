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
            [client-sdk.api.interactions.voice :as voice]
            [client-sdk.api.crud :as crud]))

(defn assemble-api []
  (let [api (merge {:session {:setActiveTenant session/set-active-tenant
                              :changeState session/change-presence-state}
                    :auth {:login auth/login}
                    :interactions {:accept int/accept-interaction
                                   :end int/end-interaction
                                   :messaging {:sendMessage msg/send-message}
                                   :voice {:hold (partial voice/auxiliary-features "customer-hold")
                                           :resume (partial voice/auxiliary-features "customer-resume")
                                           :mute (partial voice/auxiliary-features "mute-resource")
                                           :unmute (partial voice/auxiliary-features "unmute-resource")
                                           :startRecording (partial voice/auxiliary-features "recording-start")
                                           :endRecording (partial voice/auxiliary-features "recording-end")}}
                    :subscribe pubsub/subscribe})]
    (if (= (state/get-consumer-type) :cljs)
      api
      (clj->js api))))
