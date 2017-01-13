(ns client-sdk.api.interactions
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [client-sdk.api.helpers :as h]
            [client-sdk.state :as state]
            [client-sdk.domain.specs :as specs]
            [client-sdk.domain.errors :as err]))

(defn acknowledge-flow-action [params]
  (let [module-chan (state/get-module-chan :interactions)
        response-chan (a/promise-chan)
        msg (merge (h/base-module-request :INTERACTIONS/ACKNOWLEDGE_FLOW_ACTION
                                          response-chan
                                          (state/get-token))
                   params)]
    (a/put! module-chan msg)))

(s/def ::interactionId string?)
(s/def ::accept-interaction-params
    (s/keys :req-un [::interactionId]
            :opt-un [::specs/callback]))

(defn accept-interaction [params]
    (let [params (h/extract-params params)
          {:keys [interactionId callback]} params]
      (if-let [error (cond
                       (not (s/valid? ::accept-interaction-params params)) (err/invalid-params-err)
                       (not (state/session-started?)) (err/session-not-started-err)
                       (not (state/active-tenant-set?)) (err/active-tenant-not-set-err)
                       (not (state/presence-state-matches? "ready")) (err/invalid-presence-state-err)
                       (not (state/interaction-exists-in-state? :pending interactionId)) (err/interaction-not-found-err)
                       :else false)]
       error
       (let [module-chan (state/get-module-chan :interactions)
             response-chan (a/promise-chan)
             msg (merge (h/base-module-request :INTERACTIONS/SEND_INTERRUPT
                                               response-chan
                                               (state/get-token))
                        {:tenantId (state/get-active-tenant-id)
                         :interruptType "offer-accept"
                         :source "client"
                         :resourceId (state/get-active-user-id)
                         :interactionId interactionId})]
         (a/put! module-chan msg)))))

(defn end-interaction [module-chan response-chan params]
  (let [{:keys [interactionId]} (h/extract-params params)
        msg (merge (h/base-module-request :INTERACTIONS/SEND_INTERRUPT
                                          response-chan
                                          (state/get-token))
                   {:tenantId (state/get-active-tenant-id)
                    :interruptType "resource-disconnect"
                    :source "client"
                    :resourceId (state/get-active-user-id)
                    :interactionId interactionId})]
    (a/put! module-chan msg)))

(s/def ::send-message-params
    (s/keys :req-un [::specs/message]
            :opt-un [::specs/callback]))

(defn send-message-handler [params]
  (if-not (s/valid? ::send-message-params (js->clj params :keywordize-keys true))
      (err/invalid-params-err))
  (let [module-chan (state/get-module-chan :mqtt)
        response-chan (a/promise-chan)
        message (h/extract-params params)
        {:keys [callback]} message
        msg (merge (h/base-module-request :MQTT/SEND_MESSAGE
                                          response-chan
                                          (state/get-token))
                   message
                   {:tenantId (state/get-active-tenant-id)
                    :userId (state/get-active-user-id)})]
    (a/put! module-chan msg)))
