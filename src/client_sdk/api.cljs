(ns client-sdk.api
  (:require [client-sdk.api.auth :as auth]
            [client-sdk.api.session :as session]
            [client-sdk.api.flow-interrupts :as flow]))

(defn assemble-api []
  (clj->js {:session (session/api)
            :auth (auth/api)}))
