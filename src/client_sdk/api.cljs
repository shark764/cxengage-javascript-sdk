(ns client-sdk.api
  (:require [client-sdk.api.auth :as auth]
            [client-sdk.api.session :as session]))

(def api (clj->js {:session session/api
                   :auth auth/api}))
