(ns cxengage-javascript-sdk.state
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [lumbajack.core]
            [cljs.core.async :as a]
            [cljs.spec :as s]))

(def initial-state {:authentication {}
                    :user {}
                    :session {}
                    :config {:api-url "https://api.cxengage.net/v1/"
                             :consumer-type :js}
                    :interactions {:pending {}
                                   :active {}
                                   :past {}}})

(defonce sdk-state
  (atom initial-state))

(defn reset-state []
  (reset! sdk-state initial-state))

(defn get-state []
  sdk-state)

(defn set-base-api-url! [url]
  (swap! sdk-state assoc-in [:config :api-url] url))

(defn get-base-api-url []
  (get-in @sdk-state [:config :api-url]))

(defn get-env []
  (get-in @sdk-state [:config :env]))

(defn set-env! [env]
  (swap! sdk-state assoc-in [:config :env] env))

(defn set-consumer-type! [env]
  (swap! sdk-state assoc-in [:config :consumer-type] env))

(defn get-consumer-type []
  (get-in @sdk-state [:config :consumer-type]))

(defn set-blast-sqs-output! [blast]
  (swap! sdk-state assoc-in [:config :blast-sqs-output] blast))

(defn get-blast-sqs-output []
  (get-in @sdk-state [:config :blast-sqs-output]))

(defn set-log-level! [level]
  (swap! sdk-state assoc-in [:config :log-level] level))

(defn get-log-level []
  (get-in @sdk-state [:config :log-level]))

(defn get-config []
  (get @sdk-state :config))

;;;;;;;;;;;
;; Interactions
;;;;;;;;;;;

(defn find-interaction-location [interaction-id]
  (cond
    (not= nil (get-in @sdk-state [:interactions :pending interaction-id])) :pending
    (not= nil (get-in @sdk-state [:interactions :active interaction-id])) :active
    (not= nil (get-in @sdk-state [:interactions :past interaction-id])) :past
    :else (log :error "Unable to find interaction location - we have never received that interaction")))

(defn get-all-interactions []
  (get @sdk-state :interactions))

(defn get-all-pending-interactions []
  (get-in @sdk-state [:interactions :pending]))

(defn get-all-active-interactions []
  (get-in @sdk-state [:interactions :active]))

(defn get-pending-interaction [interaction-id]
  (get-in @sdk-state [:interactions :pending interaction-id]))

(defn get-active-interaction [interaction-id]
  (get-in @sdk-state [:interactions :active interaction-id]))

(defn get-interaction [interaction-id]
  (let [location (find-interaction-location interaction-id)]
    (get-in @sdk-state [:interactions location interaction-id])))

(defn insert-fb-name-to-messages [interaction-id messages]
  (let [interaction (get-interaction interaction-id)
        {:keys [channelType messaging-metadata]} interaction
        {:keys [customerName]} messaging-metadata
        messages (if (= channelType "messaging")
                   (map #(assoc-in % [:payload :from] customerName) messages)
                   messages)]
    messages))

(defn add-messages-to-history! [interaction-id messages]
  (let [interaction-location (find-interaction-location interaction-id)
        old-msg-history (or (get-in @sdk-state [:interactions interaction-location interaction-id :message-history]) [])
        messages (->> messages
                      (insert-fb-name-to-messages interaction-id)
                      (mapv #(dissoc % :channel-id :timestamp)))
        new-msg-history (reduce conj old-msg-history messages)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :message-history] new-msg-history)))

(defn get-interaction-messaging-history [interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (get-in @sdk-state [:interactions interaction-location interaction-id :message-history])))

(defn add-interaction! [type interaction]
  (let [{:keys [interaction-id]} interaction]
    (swap! sdk-state assoc-in [:interactions type interaction-id] interaction)))

(defn add-interaction-custom-field-details! [custom-field-details interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :custom-field-details] custom-field-details)))

(defn get-interaction-wrapup-details [interaction-id]
  (let [location (find-interaction-location interaction-id)]
    (get-in @sdk-state [:interactions location interaction-id :wrapup-details])))

(defn add-interaction-wrapup-details! [wrapup-details interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)
        current-details (get-interaction-wrapup-details interaction-id)
        wrapup-details (merge current-details wrapup-details)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :wrapup-details] wrapup-details)))

(defn add-interaction-disposition-code-details! [disposition-code-details interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :disposition-code-details] disposition-code-details)))

(defn transition-interaction! [from to interaction-id]
  (let [interaction (get-in @sdk-state [:interactions from interaction-id])
        updated-interactions-from (dissoc (get-in @sdk-state [:interactions from]) interaction-id)
        updated-interactions-to (assoc (get-in @sdk-state [:interactions to]) interaction-id interaction)]
    (swap! sdk-state assoc-in [:interactions from] updated-interactions-from)
    (swap! sdk-state assoc-in [:interactions to] updated-interactions-to)))

(defn add-messaging-interaction-metadata! [metadata]
  (let [{:keys [id]} metadata
        interaction-location (find-interaction-location id)]
    (swap! sdk-state assoc-in [:interactions interaction-location id :messaging-metadata] metadata)))

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
  (get-in @sdk-state [:user :user-id]))

;;;;;;;;;;;;;;;;;;
;; Sessiony Things
;;;;;;;;;;;;;;;;;;

(defn get-user-tenants []
  (get-in @sdk-state [:user :tenants]))

(defn get-tenant-permissions [tenant-id]
  (let [tenants (get-in @sdk-state [:user :tenants])
        permissions (->> tenants
                         (filter #(= tenant-id (:tenant-id %)))
                         (first)
                         (:tenant-permissions))]
    permissions))

(defn get-session-details []
  (get @sdk-state :session))

(defn set-session-details!
  [session]
  (swap! sdk-state assoc :session (merge (get-session-details) session)))

(defn set-config!
  [config]
  (swap! sdk-state assoc-in [:session :config] config))

(defn get-all-extensions []
  (get-in @sdk-state [:session :config :extensions]))

(defn get-all-integrations []
  (get-in @sdk-state [:session :config :integrations]))

(defn get-active-extension []
  (get-in @sdk-state [:session :config :active-extension :value]))

(defn get-extension-by-id [id]
  (let [extensions (get-all-extensions)]
    (first (filter #(= id (:value %)) extensions))))

(defn get-integration-by-type [type]
  (first (filter #(= (:type %) type) (get-all-integrations))))

(defn set-active-tenant!
  [tenant-id]
  (swap! sdk-state assoc-in [:session :tenant-id] tenant-id))

(defn get-active-tenant-id
  []
  (get-in @sdk-state [:session :tenant-id]))

(defn get-active-tenant-region
  []
  (get-in @sdk-state [:session :region]))

(defn set-direction!
  [direction]
  (swap! sdk-state assoc-in [:session :direction] direction))

(defn get-session-id
  []
  (get-in @sdk-state [:session :session-id]))

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

(defn has-permissions? [resource-perms req-perms]
  (let [req-perms (set req-perms)
        resource-perms (set resource-perms)
        check (clojure.set/intersection resource-perms req-perms)]
    (= check req-perms)))
