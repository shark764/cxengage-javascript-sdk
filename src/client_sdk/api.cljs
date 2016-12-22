(ns client-sdk.api
  (:require [client-sdk.api.auth :as auth]
            [client-sdk.api.session :as session]
            [client-sdk.api.flow-interrupts :as flow]))

(def api (clj->js {:session session/api
                   :auth auth/api
                   :flow flow/api}))
