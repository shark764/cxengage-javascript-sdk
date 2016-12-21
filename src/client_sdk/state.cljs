(ns client-sdk.state)

(defonce sdk-state
  (atom {:module-channels {}
         :authentication {}
         :user {}
         :session {}
         :interactions []}))

(defn get-state []
  sdk-state)

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

(defn get-active-user
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

(defn get-active-tenant
  []
  (get-in @sdk-state [:session :tenant-id]))


;;;;;;;;;;;
;; Chans
;;;;;;;;;;;

(defn get-module-chan [module topic]
  (get-in @sdk-state [:module-channels module topic]))
