(ns client-sdk.api.interactions
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [client-sdk.api.helpers :as h]
            [lumbajack.core :refer [log]]
            [client-sdk.state :as state]))

(defn acknowledge-flow-action-handler [module-chan response-chan params]
  (let [msg (merge (h/base-module-request :INTERACTIONS/ACKNOWLEDGE_FLOW_ACTION
                                          response-chan
                                          (state/get-token))
                   params)]
    (a/put! module-chan msg)))

(defn accept-offer-handler [module-chan response-chan params]
  (let [{:keys [interactionId]} (h/extract-params params)
        msg (merge (h/base-module-request :INTERACTIONS/SEND_INTERRUPT
                                          response-chan
                                          (state/get-token))
                   {:tenantId (state/get-active-tenant-id)
                    :interruptType "offer-accept"
                    :source "client"
                    :resourceId (state/get-active-user-id)
                    :interactionId interactionId})]
    (a/put! module-chan msg)))


(defn api []
  (let [module-chan (state/get-module-chan :interactions)]
    {:acceptOffer (partial accept-offer-handler module-chan (a/promise-chan))}))
