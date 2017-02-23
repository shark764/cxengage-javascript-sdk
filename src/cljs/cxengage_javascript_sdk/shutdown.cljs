(ns cxengage-javascript-sdk.shutdown
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.domain.errors :as err]
            [clojure.string :as str]
            [lumbajack.core :as logging]
            [cxengage-cljs-utils.core :as u]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.module-gateway :as mg]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.api.interactions :as flow]
            [cxengage-javascript-sdk.api.session :as session]))

(defn shutdown!
  [message]
  (when (= :fatal/SHUTDOWN (:type message))
    (let [active-interactions (reduce-kv (fn [acc k v]
                                           (conj acc k))
                                         []
                                         (state/get-all-active-interactions))
          pub-chan (mg/>get-publication-channel)]
      (log :info "Received shutdown notification, ending interactions and user session.")
      (go (doseq [interaction-id active-interactions]
            (flow/end-interaction {:interactionId interaction-id})
            (state/transition-interaction! :active :past interaction-id)))
      (go (let [parker (a/<! (session/change-presence-state {:state "offline"}))] ;;Do everything needed before shutting down all modules
            (a/put! pub-chan {:type :modules/SHUTDOWN
                              :parked? parker})
            (state/reset-state))))))

(defn messaging-shutdown!
  [message]
  (when (= :messaging/SHUTDOWN (:type message))
    (let [active-messaging-interactions (reduce-kv (fn [acc k v]
                                                     (when (or (= (:channelType v) "sms")
                                                               (= (:channelType v) "messaging"))
                                                       (conj acc k)))
                                                   []
                                                   (state/get-all-active-interactions))
          pub-chan (mg/>get-publication-channel)]
      (log :info "Received messaging shutdown notification, ending all messaging interactions.")
      (go (doseq [interaction-id active-messaging-interactions]
            (let [_ (a/<! (flow/end-interaction {:interactionId interaction-id}))]
              (state/transition-interaction! :active :past interaction-id)))
          (a/put! pub-chan {:type :MQTT/SHUTDOWN})))))

(defn interaction-shutdown!
  [message]
  (let [{:keys [type interaction-id]} message]
    (when (= :interaction/SHUTDOWN type)
      (when-let [_ (state/get-active-interaction interaction-id)]
        (flow/end-interaction {:interactionId interaction-id})
        (state/transition-interaction! :active :past interaction-id)))))

(defn msg-router
  [message]
  (let [handling-fn (case (:type message)
                      :fatal/SHUTDOWN shutdown!
                      :messaging/SHUTDOWN messaging-shutdown!
                      :interaction/SHUTDOWN interaction-shutdown!
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No proper shutdown fn for this problem!" (:type message)))))