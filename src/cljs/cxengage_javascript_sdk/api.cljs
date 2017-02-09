(ns cxengage-javascript-sdk.api
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [lumbajack.core]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.api.auth :as auth]
            [cxengage-javascript-sdk.api.session :as session]
            [cxengage-javascript-sdk.api.logging :as logging]
            [cxengage-javascript-sdk.pubsub :as pubsub]
            [cxengage-javascript-sdk.api.reporting :as reporting]
            [cxengage-javascript-sdk.api.interactions :as int]
            [cxengage-javascript-sdk.api.interactions.messaging :as msg]
            [cxengage-javascript-sdk.api.interactions.voice :as voice]
            [cxengage-javascript-sdk.api.contacts :as contacts]
            [cxengage-javascript-sdk.api.crud :as crud]))

(defn assemble-api []
  (let [api (merge {:session {:setActiveTenant session/set-active-tenant
                              :changeState session/change-presence-state}
                    :auth {:login auth/login}
                    :interactions {:accept int/accept-interaction
                                   :end int/end-interaction
                                   :messaging {:sendMessage msg/send-message}
                                   :assignContact (partial contacts/contact-interaction-assignment :assign)
                                   :unassignContact (partial contacts/contact-interaction-assignment :unassign)
                                   :voice {:hold (partial voice/auxiliary-features "customer-hold")
                                           :resume (partial voice/auxiliary-features "customer-resume")
                                           :mute (partial voice/auxiliary-features "mute-resource")
                                           :unmute (partial voice/auxiliary-features "unmute-resource")
                                           :startRecording (partial voice/auxiliary-features "recording-start")
                                           :endRecording (partial voice/auxiliary-features "recording-stop")
                                           :warmTransfer (partial voice/transfer "warm-transfer")
                                           :coldTransfer (partial voice/transfer "cold-transfer")}}
                    :api {:getQueue (partial crud/get-entity "queues")
                           :getQueues (partial crud/get-entities "queues")
                           :getUser (partial crud/get-entity "users")
                           :getUsers (partial crud/get-entities "users")}
                    :subscribe pubsub/subscribe
                    :contacts {:get contacts/get-contact
                               :search contacts/search-contacts
                               :create contacts/create-contact
                               :update contacts/update-contact
                               :delete contacts/delete-contact
                               :listAttributes contacts/list-attributes
                               :getLayout contacts/get-layout
                               :listLayouts contacts/list-layouts}})]
    (if (= (state/get-consumer-type) :cljs)
      api
      (clj->js api))))
