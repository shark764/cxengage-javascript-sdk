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
                                   :voice {:hold (partial voice/auxiliary-features "customer-hold")
                                           :resume (partial voice/auxiliary-features "customer-resume")
                                           :mute (partial voice/auxiliary-features "mute-resource")
                                           :unmute (partial voice/auxiliary-features "unmute-resource")
                                           :startRecording (partial voice/auxiliary-features "recording-start")
                                           :endRecording (partial voice/auxiliary-features "recording-stop")}}
                    :subscribe pubsub/subscribe
                    :contacts {:getContact contacts/get-contact
                               :searchContacts contacts/search-contacts
                               :createContact contacts/create-contact
                               :updateContact contacts/update-contact
                               :deleteContact contacts/delete-contact
                               :listAttributes contacts/list-attributes
                               :getLayout contacts/get-layout
                               :listLayouts contacts/list-layouts}})]
    (if (= (state/get-consumer-type) :cljs)
      api
      (clj->js api))))
