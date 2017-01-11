(ns client-sdk.api.interactions
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [client-sdk.api.helpers :as h]
            [client-sdk.state :as state]))

(defn acknowledge-flow-action [params]
  (let [module-chan (state/get-module-chan :interactions)
        response-chan (a/promise-chan)
        msg (merge (h/base-module-request :INTERACTIONS/ACKNOWLEDGE_FLOW_ACTION
                                          response-chan
                                          (state/get-token))
                   params)]
    (a/put! module-chan msg)))

(defn accept-interaction [params]
  (let [module-chan (state/get-module-chan :interactions)
        response-chan (a/promise-chan)
        {:keys [interactionId]} (h/extract-params params)
        msg (merge (h/base-module-request :INTERACTIONS/SEND_INTERRUPT
                                          response-chan
                                          (state/get-token))
                   {:tenantId (state/get-active-tenant-id)
                    :interruptType "offer-accept"
                    :source "client"
                    :resourceId (state/get-active-user-id)
                    :interactionId interactionId})]
    (a/put! module-chan msg)))

(defn end-interaction [params] nil)
