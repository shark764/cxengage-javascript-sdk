(ns client-sdk.state
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [lumbajack.core]
            [cljs.core.async :as a]))

(defonce sdk-state
  (atom {:async-module-registration (a/chan 1024)
         :module-channels {}
         :authentication {}
         :user {}
         :session {}
         :interactions {:pending {}
                        :active {}
                        :past {}}}))

(defn get-state []
  sdk-state)

(defn get-env []
  (get @sdk-state :env))

(defn set-env! [env]
  (swap! sdk-state assoc :env env))

(defn get-async-module-registration []
  (get @sdk-state :async-module-registration))

(defn set-consumer-type! [env]
  (swap! sdk-state assoc :consumer-type env))

(defn get-consumer-type []
  (or (get @sdk-state :consumer-type) :js))

;;;;;;;;;;;
;; Interactions
;;;;;;;;;;;

(defn find-interaction-location [interaction-id]
  (cond
    (not= nil (get-in @sdk-state [:interactions :pending interaction-id])) :pending
    (not= nil (get-in @sdk-state [:interactions :active interaction-id])) :active
    (not= nil (get-in @sdk-state [:interactions :past interaction-id])) :past
    :else (log :error "Unable to find interaction location")))

(defn get-all-interactions []
  (get @sdk-state :interactions))

(defn get-all-pending-interactions []
  (get-in @sdk-state [:interactions :pending]))

(defn add-messages-to-history! [interaction-id messages]
  (let [interaction-location (find-interaction-location interaction-id)
        old-msg-history (or (get-in @sdk-state [:interactions interaction-location interaction-id :message-history]) [])
        new-msg-history (reduce conj old-msg-history messages)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :message-history] new-msg-history)))

(defn get-pending-interaction [interaction-id]
  (get-in @sdk-state [:interactions :pending interaction-id]))

(defn get-active-interaction [interaction-id]
  (get-in @sdk-state [:interactions :active interaction-id]))

(defn add-interaction! [type interaction]
  (let [{:keys [interactionId]} interaction]
    (swap! sdk-state assoc-in [:interactions type interactionId] interaction)))

(defn add-interaction-custom-field-details! [custom-field-details interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :custom-field-details custom-field-details])))

(defn add-interaction-wrapup-details! [wrapup-details interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :wrapup-details wrapup-details])))

(defn add-interaction-disposition-code-details! [disposition-code-details interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :disposition-code-details disposition-code-details])))

(defn transition-interaction! [from to interaction-id]
  (let [interaction (get-in @sdk-state [:interactions from interaction-id])
        updated-interactions-from (dissoc (get-in @sdk-state [:interactions from]) interaction-id)
        updated-interactions-to (assoc (get-in @sdk-state [:interactions to]) interaction-id interaction)]
    (swap! sdk-state assoc-in [:interactions from] updated-interactions-from)
    (swap! sdk-state assoc-in [:interactions to] updated-interactions-to)))

;;;;;;;;;;;
;; Auth
;;;;;;;;;;;

(defn set-token!
  [token]
  (swap! sdk-state assoc-in [:authentication :token] token))

(defn get-token
  []
  (get-in @sdk-state [:authentication :token]))

;;;;;;;;;;;;;;;;
;; User Identity
;;;;;;;;;;;;;;;;

(defn set-user-identity!
  [identity]
  (swap! sdk-state assoc :user identity))

(defn get-active-user-id
  []
  (get-in @sdk-state [:user :userId]))

;;;;;;;;;;;;;;;;;;
;; Sessiony Things
;;;;;;;;;;;;;;;;;;

(defn get-session-details []
  (get @sdk-state :session))

(defn set-session-details!
  [session]
  (swap! sdk-state assoc :session (merge (get-session-details) session)))

(defn set-config!
  [config]
  (swap! sdk-state assoc :session (merge (get-session-details) config)))

(defn set-active-tenant!
  [tenant-id]
  (swap! sdk-state assoc-in [:session :tenant-id] tenant-id))

(defn get-active-tenant-id
  []
  (get-in @sdk-state [:session :tenant-id]))

(defn get-active-tenant-region
  []
  (log :debug (get :session @(get-state)))
  (get-in @sdk-state [:session :region]))

(defn set-direction!
  [direction]
  (swap! sdk-state assoc-in [:session :direction] direction))

(defn get-session-id
  []
  (get-in @sdk-state [:session :sessionId]))

(defn set-capacity!
  [capacity]
  (swap! sdk-state assoc-in [:session :capacity] capacity))

(defn set-user-session-state!
  [state]
  (swap! sdk-state assoc :session (merge (get-session-details) state)))

(defn get-user-session-state
  []
  (get-in @sdk-state [:session :state]))

;;;;;;;;;;;
;; twilio
;;;;;;;;;;;

(defn set-twilio-device
  [device]
  (swap! sdk-state assoc-in [:interal :twilio-device] device))

(defn get-twilio-device
  []
  (get-in @sdk-state [:interal :twilio-device]))

(defn set-twilio-connection
  [connection]
  (swap! sdk-state assoc-in [:interal :twilio-connection] connection))

(defn get-twilio-connection
  []
  (get-in @sdk-state [:interal :twilio-connection]))

;;;;;;;;;;;
;; Chans
;;;;;;;;;;;

(defn get-module-chan [module]
  (get-in @sdk-state [:module-channels module :messages]))

(defn get-module-shutdown-chan [module]
  (get-in @sdk-state [:module-channels module :shutdown]))


;;;;;;;;;;;
;; Predicates
;;;;;;;;;;;

(defn session-started? []
  (get-session-id))

(defn active-tenant-set? []
  (get-active-tenant-id))

(defn presence-state-matches? [state]
  (= state (get-user-session-state)))

(defn interaction-exists-in-state? [interaction-state interaction-id]
  (get-in @sdk-state [:interactions interaction-state interaction-id]))
