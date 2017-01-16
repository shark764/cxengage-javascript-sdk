(ns client-sdk.api.interactions.voice
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [client-sdk.state :as state]
            [cljs.core.async :as a]
            [client-sdk.api.helpers :as h]
            [cljs.spec :as s]
            [client-sdk.domain.specs :as specs]
            [client-sdk.domain.errors :as err]))

(s/def ::voice-features-params
    (s/keys :req-un [::specs/interactionId]
            :opt-un [::specs/callback]))

(defn auxiliary-features [interrupt-type params]
  (let [params (h/extract-params params)
        {:keys [interactionId]} params]
    (if-let [error (cond
                     (not (s/valid? ::voice-features-params params)) (err/invalid-params-err "Invalid interaction ID")
                     (not (state/session-started?)) (err/session-not-started-err)
                     (not (state/active-tenant-set?)) (err/active-tenant-not-set-err)
                     (not (state/presence-state-matches? "ready")) (err/invalid-presence-state-err)
                     (not (state/interaction-exists-in-state? :active interactionId)) (err/interaction-not-found-err)
                     :else false)]
      error
      (let [module-chan (state/get-module-chan :interactions)
            response-chan (a/promise-chan)
            msg (merge (h/base-module-request :INTERACTIONS/SEND_INTERRUPT
                                              response-chan
                                              (state/get-token))
                       {:tenantId (state/get-active-tenant-id)
                        :interruptType interrupt-type
                        :source "client"
                        :resourceId (state/get-active-user-id)
                        :interactionId interactionId})]
        (a/put! module-chan msg)))))
