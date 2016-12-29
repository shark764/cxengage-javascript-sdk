(ns client-sdk.state
  (:require [lumbajack.core :refer [log]]
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

(defn get-all-interactions []
  (get @sdk-state :interactions))

(defn get-all-pending-interactions []
  (get-in @sdk-state [:interactions :pending]))

(defn get-pending-interaction [interaction-id]
  (get-in @sdk-state [:interactions :pending interaction-id]))

(defn remove-interaction! [type message]
  (let [{:keys [interactionId]} message
        interaction (get-in @sdk-state [:interactions type interactionId])
        updated-interactions-of-type (-> (get-in @sdk-state [:interactions type])
                                         (dissoc interactionId))]
    (swap! sdk-state assoc-in [:interactions type] updated-interactions-of-type)
    (swap! sdk-state assoc-in [:interactions :past interactionId] interaction)))

(defn add-interaction! [type interaction]
  (let [{:keys [interactionId]} interaction]
    (swap! sdk-state assoc-in [:interactions type interactionId] interaction)))

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

;;;;;;;;;;;
;; Chans
;;;;;;;;;;;

(defn get-module-chan [module]
  (get-in @sdk-state [:module-channels module]))
