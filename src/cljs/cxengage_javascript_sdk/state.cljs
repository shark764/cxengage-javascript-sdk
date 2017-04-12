(ns cxengage-javascript-sdk.state
  (:require [lumbajack.core]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cljs.core.async :as a]
            [cljs.spec :as s]
            [cljs-uuid-utils.core :as id]))

(def initial-state {:authentication {}
                    :user {}
                    :session {}
                    :config {}
                    :interactions {:pending {}
                                   :active {}
                                   :past {}}
                    :logs {:unsaved-logs []
                           :saved-logs []
                           :valid-levels [:debug :info :warn :error :fatal]}
                    :time {:offset 0}})

(defonce sdk-state
  (atom initial-state))

(defn reset-state []
  (reset! sdk-state initial-state))

(defn get-state []
  @sdk-state)

(defn get-state-js []
  (clj->js @sdk-state))

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

(defn set-reporting-refresh-rate! [rate]
  (swap! sdk-state assoc-in [:config :reporting-refresh-rate] rate))

(defn get-reporting-refresh-rate []
  (get-in @sdk-state [:config :reporting-refresh-rate]))

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
    :else (js/console.error "Unable to find interaction location - we have never received that interaction")))

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

(defn get-interaction-disposition-codes [interaction-id]
  (let [location (find-interaction-location interaction-id)]
    (get-in @sdk-state [:interactions location interaction-id :disposition-code-details :dispositions])))

(defn augment-messaging-payload [msg]
  (let [{:keys [payload]} msg
        {:keys [from id to]} payload
        interaction (get-interaction to)
        {:keys [channel-type messaging-metadata]} interaction
        {:keys [metadata]} messaging-metadata
        {:keys [customer-name]} metadata
        payload (cond
                  (and (= channel-type "sms")
                       (nil? (id/valid-uuid? from))) (assoc-in msg [:payload :from] (str "+" from))
                  (= channel-type "messaging") (assoc-in msg [:payload :from] customer-name)
                  :else msg)]
    payload))

(defn add-messages-to-history! [interaction-id messages]
  (let [interaction-location (find-interaction-location interaction-id)
        old-msg-history (or (get-in @sdk-state [:interactions interaction-location interaction-id :message-history]) [])
        messages (->> messages
                      (mapv augment-messaging-payload)
                      (mapv #(dissoc % :channel-id :timestamp))
                      (mapv :payload))
        new-msg-history (reduce conj old-msg-history messages)
        new-msg-history (reduce (fn [acc msg]
                                  (if (= 0 (count (filter #(= (:id msg) (:id %)) old-msg-history)))
                                    (conj acc msg)
                                    acc)) old-msg-history messages)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :message-history] new-msg-history)))

(defn get-interaction-messaging-history [interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (filter #(= (get % :type) "message")
            (get-in @sdk-state [:interactions interaction-location interaction-id :message-history]))))

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

(defn add-script-to-interaction! [interaction-id script]
  (let [interaction-location (find-interaction-location interaction-id)
        existing-scripts (or (get-in @sdk-state [:interactions interaction-location interaction-id :scripts]) [])
        new-scripts (conj existing-scripts script)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :scripts] new-scripts)))

(defn get-script [interaction-id script-id]
  (let [interaction-location (find-interaction-location interaction-id)
        scripts (or (get-in @sdk-state [:interactions interaction-location interaction-id :scripts]) [])
        filtered-script (first (filterv #(= script-id (:id (js->clj (js/JSON.parse (:script %)) :keywordize-keys true))) scripts))]
    filtered-script))

(defn add-email-artifact-data [interaction-id artifact-data]
  (let [interaction-location (find-interaction-location interaction-id)
        email-artifact-data (assoc-in artifact-data [:reply :attachments] {})]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :email-artifact] email-artifact-data)))

(defn store-email-reply-artifact-id [artifact-id interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :email-artifact :reply :artifact-id] artifact-id)))

(defn get-reply-artifact-id-by-interaction-id [interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (get-in @sdk-state [:interactions interaction-location interaction-id :email-artifact :reply :artifact-id])))

(defn get-all-reply-email-attachments [interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (get-in @sdk-state [:interactions interaction-location interaction-id :email-artifact :reply :attachments])))

(defn remove-attachment-from-reply [file-info]
  (let [{:keys [interaction-id attachment-id]} file-info
        interaction-location (find-interaction-location interaction-id)
        attachments (get-all-reply-email-attachments interaction-id)
        new-attachments (dissoc attachments attachment-id)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :email-artifact :reply :attachments] new-attachments)))

(defn get-email-reply-to-id [interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (get-in @sdk-state [:interactions interaction-location interaction-id :email-artifact :manifest-details :id])))

(defn add-email-manifest-details [interaction-id manifest]
  (let [interaction-location (find-interaction-location interaction-id)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :email-artifact :manifest-details] manifest)))

(defn add-attachment-to-reply [file-info]
  (let [{:keys [interaction-id attachment-id file]} file-info
        interaction-location (find-interaction-location interaction-id)
        attachments (get-all-reply-email-attachments interaction-id)
        new-attachments (assoc attachments attachment-id file)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :email-artifact :reply :attachments] new-attachments)))

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

(defn set-session-expired! [expired?]
  (swap! sdk-state assoc-in [:session :expired?] expired?))

(defn get-session-expired []
  (get-in @sdk-state [:session :expired?]))

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

(defn get-all-reason-lists []
  (get-in @sdk-state [:session :config :reason-lists]))

(defn get-all-reason-codes-by-list [reason-list-id]
  (let [reason-list (reduce (fn [acc x]
                              (if (= (:id x) reason-list-id)
                                (merge acc x)
                                acc)) {} (get-all-reason-lists))
        reason-codes (reduce (fn [acc x] (conj acc (select-keys x [:reason-id :name]))) [] (:reasons reason-list))]
    reason-codes))

(defn valid-reason-codes? [reason reason-id reason-list-id]
  (let [reason-map {:reason-id reason-id
                    :name reason}
        found-reason (filterv #(= (:reason-id %1) reason-id) (get-all-reason-codes-by-list reason-list-id))]
    (= reason-map (peek found-reason))))

(defn get-extension-by-value [value]
  (let [extensions (get-all-extensions)]
    (first (filter #(= value (:value %)) extensions))))

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
  (swap! sdk-state assoc-in [:internal :twilio-device] device))

(defn get-twilio-device
  []
  (get-in @sdk-state [:internal :twilio-device]))

(defn set-twilio-connection
  [connection]
  (swap! sdk-state assoc-in [:internal :twilio-connection] connection))

(defn get-twilio-connection
  []
  (get-in @sdk-state [:internal :twilio-connection]))

;;;;;;;;;;;
;; Logging
;;;;;;;;;;;

(defn get-log-level []
  (get-in @sdk-state [:config :log-level]))

(defn set-valid-log-levels! [levels]
  (swap! sdk-state assoc-in [:logs :valid-levels] levels))

(defn get-valid-log-levels []
  (get-in @sdk-state [:logs :valid-levels]))

(defn set-log-level! [level levels]
  (if (not= -1 (.indexOf (vec (keys levels)) level))
    (let [idx {:fatal 1 :error 2 :warn 3 :info 4 :debug 5}
          updated-valid-levels (take (or (get idx level) 0) (vec (reverse (keys levels))))]
      (set-valid-log-levels! updated-valid-levels)
      (swap! sdk-state assoc-in [:config :log-level] level))))

(defn get-saved-logs []
  (get-in @sdk-state [:logs :saved-logs]))

(defn get-unsaved-logs []
  (get-in @sdk-state [:logs :unsaved-logs]))

(defn append-logs!
  [& logs]
  (let [unsaved (get-unsaved-logs)
        appended (into unsaved logs)]
    (swap! sdk-state assoc-in [:logs :unsaved-logs] appended)))

(defn save-logs []
  (let [unsaved (get-unsaved-logs)
        saved (get-saved-logs)
        appended (into saved unsaved)]
    (swap! sdk-state assoc-in [:logs :saved-logs] appended)
    (swap! sdk-state assoc-in [:logs :unsaved-logs] [])))

;;;;;;;;;;;
;; Time
;;;;;;;;;;;

(defn set-time-offset!
  [offset]
  (swap! sdk-state assoc-in [:time :offset] offset))

(defn get-time-offset
  []
  (get-in @sdk-state [:time :offset]))

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
