(ns client-sdk.api.interactions.messaging
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [client-sdk.state :as state]
            [cljs.core.async :as a]
            [client-sdk.api.helpers :as h]))

(defn send-message [params]
  (let [module-chan (state/get-module-chan :mqtt)
        response-chan (a/promise-chan)
        message (h/extract-params params)
        {:keys [callback]} message
        _ (log :warn "MESSAGGEEE" message)
        msg (merge (h/base-module-request :MQTT/SEND_MESSAGE
                                          response-chan
                                          (state/get-token))
                   message
                   {:tenantId (state/get-active-tenant-id)
                    :userId (state/get-active-user-id)})]
    (a/put! module-chan msg)))
