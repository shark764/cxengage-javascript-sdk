(ns cxengage-javascript-sdk.module-gateway
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.specs :as specs]))

(defn get-module-name [msg-type]
  (case msg-type
    (:AUTH/GET_TOKEN :AUTH/LOGIN :AUTH/GET_CONFIG) :authentication
    (:SESSION/START_SESSION :SESSION/CHANGE_STATE) :presence
    (:INTERACTIONS/ACKNOWLEDGE_FLOW_ACTION :INTERACTIONS/SEND_INTERRUPT) :interactions
    (:MQTT/SEND_MESSAGE) :mqtt
    (:CONTACTS/CREATE_CONTACT :CONTACTS/GET_CONTACT :CONTACTS/SEARCH_CONTACTS :CONTACTS/UPDATE_CONTACT :CONTACTS/DELETE_CONTACT :CONTACTS/LIST_ATTRIBUTES :CONTACTS/GET_LAYOUT :CONTACTS/LIST_LAYOUTS) :contacts
    (do (log :error (str  "No matching type, unable to determine module for type" (name msg-type)))
        nil)))

(defn send-module-message [message]
  (let [{:keys [type]} message
        module-response-chan (a/promise-chan)
        module-input-chan (state/get-module-chan (get-module-name type))]
    (if module-input-chan
      (do (a/put! module-input-chan (assoc message :resp-chan module-response-chan))
          module-response-chan)
      (do (log :error "Unable to determine which module to send this message to: check the module gateway
                       router to see if your message is enumerated.")
          nil))))
