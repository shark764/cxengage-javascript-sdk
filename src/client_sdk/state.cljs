(ns client-sdk.state
  (:require [lumbajack.core :refer [log]]
            [cljs.core.async :as a]))

(defonce sdk-state
  (atom {:async-module-registration (a/chan 1024)
         :module-channels {}
         :authentication {}
         :user {}
         :session {}
         :interactions []}))

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

(defn set-config!
  [config]
  (swap! sdk-state assoc-in [:session :config] config))

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

(defn set-session-details!
  [session]
  (swap! sdk-state assoc-in [:session :active-session] session))

(defn get-session-id
  []
  (get-in @sdk-state [:session :active-session :sessionId]))

(defn set-capacity!
  [capacity]
  (swap! sdk-state assoc-in [:session :capacity] capacity))

(defn set-user-state!
  [state]
  (swap! sdk-state assoc-in [:session :state] state))

;;;;;;;;;;;
;; Chans
;;;;;;;;;;;

(defn get-module-chan [module]
  (get-in @sdk-state [:module-channels module]))
