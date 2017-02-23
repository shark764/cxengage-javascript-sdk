(ns cxengage-javascript-sdk.api
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [lumbajack.core]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.api.auth :as auth]
            [cxengage-javascript-sdk.api.session :as session]
            [cxengage-javascript-sdk.pubsub :as pubsub]
            [cxengage-javascript-sdk.api.reporting :as reporting]
            [cxengage-javascript-sdk.api.interactions :as int]
            [cxengage-javascript-sdk.api.interactions.messaging :as msg]
            [cxengage-javascript-sdk.api.interactions.voice :as voice]
            [cxengage-javascript-sdk.api.contacts :as contacts]
            [cxengage-javascript-sdk.api.crud :as crud]
            [cxengage-javascript-sdk.api.reporting :as reporting]))

(defn assemble-api []
  (let [api (merge {:session {:setActiveTenant session/set-active-tenant
                              :goReady (partial session/change-presence-state-ready "ready")
                              :goNotReady (partial session/change-presence-state {:state "notready"})
                              :goOffline (partial session/change-presence-state {:state "offline"})
                              :setDirectionInbound session/set-direction-inbound
                              :setDirectionOutbound session/set-direction-outbound}
                    :auth {:login auth/login
                           :logout auth/logout}
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
                                           :transferToResource voice/transfer-to-resource
                                           :transferToQueue voice/transfer-to-queue
                                           :transferToExtension voice/transfer-to-extension
                                           :cancelTransfer voice/cancel-transfer
                                           :dial voice/dial}
                                   :enableWrapup int/enable-wrapup
                                   :disableWrapup int/disable-wrapup
                                   :endWrapup int/end-wrapup}
                    :api {:getQueue (partial crud/get-entity "queues")
                          :getQueues (partial crud/get-entities "queues")
                          :getUser (partial crud/get-entity "users")
                          :getUsers (partial crud/get-entities "users")
                          :getTransferList (partial crud/get-entity "transfer-lists")
                          :getTransferLists (partial crud/get-entities "transfer-lists")}
                    :subscribe pubsub/subscribe
                    :contacts {:get contacts/get-contact
                               :search contacts/search-contacts
                               :create contacts/create-contact
                               :update contacts/update-contact
                               :delete contacts/delete-contact
                               :listAttributes contacts/list-attributes
                               :getLayout contacts/get-layout
                               :listLayouts contacts/list-layouts}
                    :reporting {:startPolling reporting/start-polling
                                :checkCapacity reporting/check-capacity
                                :getAvailableStats reporting/available-stats}})]

    (if (= (state/get-consumer-type) :cljs)
      api
      (clj->js api))))
