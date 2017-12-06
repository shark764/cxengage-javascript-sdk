(ns cxengage-javascript-sdk.state
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [lumbajack.core]
            [clojure.set :as s :refer [intersection]]
            [cljs-uuid-utils.core :as id]))

(def initial-state {:authentication {:token false}
                    :user {}
                    :session {}
                    :config {}
                    :internal {:enabled-modules []
                               :mqtt-client false}
                    :interactions {:pending {}
                                   :active {}
                                   :past {}
                                   :incoming {}}
                    :logs {:unsaved-logs []
                           :saved-logs []
                           :valid-levels [:debug :info :warn :error :fatal]}
                    :time {:offset 0}})

(defonce sdk-state
  (atom initial-state))

(defn get-state []
  @sdk-state)

(defn get-state-value [path]
  (let [value (get-in @sdk-state path)]
    #_(when (nil? value)
        (log :debug "[SDK State] Unable to find value in state. Path:" (clj->js path)))
    value))

(defn reset-state []
  (when-let [mqtt-client (get-state-value [:internal :mqtt-client])]
    (.disconnect mqtt-client))
  (reset! sdk-state (merge initial-state (dissoc (get-state) :authentication :user :session :interactions :internal))))

(defn destroy-state []
  (reset! sdk-state initial-state))

(defn get-state-js []
  (clj->js @sdk-state))

(defn set-base-api-url! [url]
  (swap! sdk-state assoc-in [:config :api-url] url))

(defn get-base-api-url []
  (get-state-value [:config :api-url]))

(defn get-env []
  (get-state-value [:config :env]))

(defn set-env! [env]
  (swap! sdk-state assoc-in [:config :env] env))

(defn set-locale! [locale]
  (swap! sdk-state assoc-in [:config :locale] locale))

(defn get-locale []
  (get-state-value [:config :locale]))

(defn set-consumer-type! [env]
  (swap! sdk-state assoc-in [:config :consumer-type] env))

(defn get-consumer-type []
  (get-state-value [:config :consumer-type]))

(defn set-blast-sqs-output! [blast]
  (swap! sdk-state assoc-in [:config :blast-sqs-output] blast))

(defn get-blast-sqs-output []
  (get-state-value [:config :blast-sqs-output]))

(defn set-reporting-refresh-rate! [rate]
  (swap! sdk-state assoc-in [:config :reporting-refresh-rate] rate))

(defn get-reporting-refresh-rate []
  (get-state-value [:config :reporting-refresh-rate]))

(defn get-config []
  (get @sdk-state :config))

;;;;;;;;;;;
;; Interactions
;;;;;;;;;;;

(defn find-interaction-location
  "Determines which bucket an interaction is in that we have encountered during this session."
  [interaction-id]
  (cond
    (not= nil (get-state-value [:interactions :pending interaction-id])) :pending
    (not= nil (get-state-value [:interactions :active interaction-id])) :active
    (not= nil (get-state-value [:interactions :past interaction-id])) :past
    (not= nil (get-state-value [:interactions :incoming interaction-id])) :incoming
    :else nil))

(defn get-all-pending-interactions []
  (get-state-value [:interactions :pending]))

(defn get-pending-interaction [interaction-id]
  (get-state-value [:interactions :pending interaction-id]))

(defn get-active-interaction [interaction-id]
  (get-state-value [:interactions :active interaction-id]))

(defn get-incoming-interaction [interaction-id]
  (get-state-value [:interactions :incoming interaction-id]))

(defn get-past-interaction [interaction-id]
  (get-state-value [:interactions :past interaction-id]))

(defn active-interactions? []
  (not (empty? (get-state-value [:interactions :active]))))

(defn get-all-active-interactions []
  (get-state-value [:interactions :active]))

(defn get-interaction [interaction-id]
  (let [location (find-interaction-location interaction-id)]
    (get-state-value [:interactions location interaction-id])))

(defn get-interaction-disposition-codes [interaction-id]
  (let [location (find-interaction-location interaction-id)]
    (get-state-value [:interactions location interaction-id :disposition-code-details :dispositions])))

(defn augment-messaging-payload
  "Normalizes the structure of the messaging payload, and updates the :from field depending on if it's a messaging or SMS interaction."
  [msg]
  (let [{:keys [payload]} msg
        {:keys [from to]} payload
        msg-metadata (get-in payload [:metadata])
        msg-type (get-in payload [:metadata :type])
        interaction (get-interaction to)
        {:keys [channel-type messaging-metadata]} interaction
        {:keys [metadata]} messaging-metadata
        {:keys [customer-name]} metadata
        payload (cond
                  (and (= channel-type "sms")
                       (nil? (id/valid-uuid? from))) (assoc-in msg [:payload :from] (str "+" from))
                  (and (= channel-type "messaging")
                       (or (= msg-metadata nil) ;; msg metadata is null for facebook interactions
                           (= msg-type "customer"))) (assoc-in msg [:payload :from] customer-name)
                  :else msg)]
    payload))

(defn add-messages-to-history!
  "Appends a list of messages to the messaging history for a given interaction."
  [interaction-id messages]
  (let [interaction-location (find-interaction-location interaction-id)
        old-msg-history (or (get-state-value [:interactions interaction-location interaction-id :message-history]) [])
        messages (->> messages
                      (mapv augment-messaging-payload)
                      (mapv #(dissoc % :channel-id :timestamp))
                      (mapv :payload))
        new-msg-history (reduce conj old-msg-history messages)
        new-msg-history (reduce (fn [acc msg]
                                  (if (zero? (count (filter #(= (:id msg) (:id %)) old-msg-history)))
                                    (conj acc msg)
                                    acc)) old-msg-history messages)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :message-history] new-msg-history)))

(defn get-interaction-messaging-history [interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (filter #(= (get % :type) "message")
            (get-state-value [:interactions interaction-location interaction-id :message-history]))))

(defn add-interaction! [type interaction]
  (let [{:keys [interaction-id]} interaction
        ;; add past interaction's scripts to new interaction in case scripts have not yet been submitted
        existing-past-interaction (get-past-interaction interaction-id)
        interaction-with-past-scripts (if existing-past-interaction
                                          (assoc interaction :scripts (get existing-past-interaction :scripts))
                                          interaction)]
    (swap! sdk-state assoc-in [:interactions type interaction-id] interaction-with-past-scripts)))

(defn add-interaction-custom-field-details! [custom-field-details interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :custom-field-details] custom-field-details)))

(defn get-interaction-wrapup-details [interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (get-state-value [:interactions interaction-location interaction-id :wrapup-details])))

(defn add-interaction-wrapup-details! [wrapup-details interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)
        current-details (get-interaction-wrapup-details interaction-id)
        wrapup-details (merge current-details wrapup-details)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :wrapup-details] wrapup-details)))

(defn add-interaction-disposition-code-details! [disposition-code-details interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :disposition-code-details] disposition-code-details)))

(defn transition-interaction!
  "Moves an interaction from one state internally to another. E.G. from pending (we've received a work offer) to active (we've accepted the work offer)."
  [from to interaction-id]
  (let [interaction (get-state-value [:interactions from interaction-id])
        updated-interactions-from (dissoc (get-state-value [:interactions from]) interaction-id)
        updated-interactions-to (assoc (get-state-value [:interactions to]) interaction-id interaction)]
    (swap! sdk-state assoc-in [:interactions from] updated-interactions-from)
    (swap! sdk-state assoc-in [:interactions to] updated-interactions-to)))

(defn add-messaging-interaction-metadata! [metadata]
  (let [{:keys [id]} metadata
        interaction-location (find-interaction-location id)]
    (swap! sdk-state assoc-in [:interactions interaction-location id :messaging-metadata] metadata)))

(defn add-script-to-interaction! [interaction-id script]
  (let [interaction-location (find-interaction-location interaction-id)
        existing-scripts (or (get-state-value [:interactions interaction-location interaction-id :scripts]) [])
        new-scripts (conj existing-scripts script)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :scripts] new-scripts)))

(defn get-script [interaction-id script-id]
  (let [interaction-location (find-interaction-location interaction-id)
        scripts (or (get-state-value [:interactions interaction-location interaction-id :scripts]) [])
        filtered-script (first
                         (filter
                          (fn [i]
                            (= script-id (:action-id i)))
                          scripts))]
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
    (get-state-value [:interactions interaction-location interaction-id :email-artifact :reply :artifact-id])))

(defn get-all-reply-email-attachments [interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (get-state-value [:interactions interaction-location interaction-id :email-artifact :reply :attachments])))

(defn remove-attachment-from-reply [file-info]
  (let [{:keys [interaction-id attachment-id]} file-info
        interaction-location (find-interaction-location interaction-id)
        attachments (get-all-reply-email-attachments interaction-id)
        new-attachments (dissoc attachments attachment-id)]
    (swap! sdk-state assoc-in [:interactions interaction-location interaction-id :email-artifact :reply :attachments] new-attachments)))

(defn get-email-reply-to-id [interaction-id]
  (let [interaction-location (find-interaction-location interaction-id)]
    (get-state-value [:interactions interaction-location interaction-id :email-artifact :manifest-details :id])))

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
  (get-state-value [:authentication :token]))

(defn set-sso-token!
  [token]
  (swap! sdk-state assoc-in [:authentication :sso-token] token))

(defn get-sso-token
  []
  (get-state-value [:authentication :sso-token]))

(defn set-sso-client-details!
  [details]
  (swap! sdk-state assoc-in [:authentication :sso-client] details))

(defn get-sso-client-details
  []
  (get-state-value [:authentication :sso-client]))

;;;;;;;;;;;;;;;;
;; User Identity
;;;;;;;;;;;;;;;;

(defn set-user-identity!
  [identity]
  (swap! sdk-state assoc :user identity))

(defn get-active-user-id
  []
  (let [user-id (get-state-value [:user :user-id])]
    (if user-id
      user-id
      (do (js/console.warn "[SDK State] Unable to find user id in state; likely to have unintended side-effects.")
          nil))))

;;;;;;;;;;;;;;;;;;
;; Sessiony Things
;;;;;;;;;;;;;;;;;;

(defn set-session-expired! [expired?]
  (swap! sdk-state assoc-in [:session :expired?] expired?))

(defn get-session-expired []
  (get-state-value [:session :expired?]))

(defn get-tenant-permissions [tenant-id]
  (let [tenants (get-state-value [:user :tenants])
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

(defn set-extensions!
  [extensions]
  (swap! sdk-state assoc-in [:session :config :extensions] extensions))

(defn get-all-extensions []
  (get-state-value [:session :config :extensions]))

(defn get-all-integrations []
  (get-state-value [:session :config :integrations]))

(defn get-outbound-integration-type []
  (get-state-value [:session :config :outbound-integration :type]))

(defn get-active-extension []
  (get-state-value [:session :config :active-extension]))

(defn get-all-reason-lists []
  (get-state-value [:session :config :reason-lists]))

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

(defn update-integration [type integration]
  (let [state-integrations (get-all-integrations)
        other-integrations (filterv #(not= type (:type %1)) state-integrations)
        new-integrations (conj other-integrations integration)]
    (swap! sdk-state assoc-in [:session :config :integrations] new-integrations)))

(defn set-tenant-data!
  [tenant-data]
  (swap! sdk-state assoc-in [:session :tenant] tenant-data))

(defn get-tenant-data
  []
  (let [tenant-data (get-state-value [:session :tenant])]
    (if tenant-data
      tenant-data
      (do (js/console.warn "[SDK State] Unable to find tenant data in state; likely to have unintended side-effects.")
          nil))))

(defn get-region
  []
  (let [region (get-state-value [:session :region])]
    (if region
      region
      (do (js/console.warn "[SDK State] Unable to find region in state; likely to have unintended side-effects.")
          nil))))

(defn set-region!
  [region]
  (swap! sdk-state assoc-in [:session :region] region))

(defn set-active-tenant!
  [tenant-id]
  (swap! sdk-state assoc-in [:session :tenant-id] tenant-id))

(defn get-active-tenant-id
  []
  (let [tenant-id (get-state-value [:session :tenant-id])]
    tenant-id))

(defn get-session-id
  []
  (let [session-id (get-state-value [:session :session-id])]
    (if session-id
      session-id
      (do (js/console.warn "[SDK State] Unable to find session id in state; likely to have unintended side-effects.")
          nil))))

(defn set-user-session-state!
  [state]
  (swap! sdk-state assoc :session (merge (get-session-details) state)))

(defn get-user-session-state
  []
  (get-state-value [:session :state]))

;;;;;;;;;;;
;; twilio
;;;;;;;;;;;

(defn set-twilio-device
  [device]
  (swap! sdk-state assoc-in [:internal :twilio-device] device))

(defn get-twilio-device
  []
  (get-state-value [:internal :twilio-device]))

(defn set-twilio-connection
  [connection]
  (swap! sdk-state assoc-in [:internal :twilio-connection] connection))

(defn get-twilio-connection
  []
  (get-state-value [:internal :twilio-connection]))

;;;;;;;;;;;;;
;; Messaging
;;;;;;;;;;;;;

(defn set-mqtt-client
  [client]
  (swap! sdk-state assoc-in [:internal :mqtt-client] client))

(defn get-mqtt-client
  []
  (get-state-value [:internal :mqtt-client]))

;;;;;;;;;;;
;; Time
;;;;;;;;;;;

(defn set-time-offset!
  [offset]
  (swap! sdk-state assoc-in [:time :offset] offset))

(defn get-time-offset
  []
  (get-state-value [:time :offset]))

(defn has-permissions?
  "Checks if a user has all of the permissions necessary within a list of required permissions for a given action."
  [resource-perms req-perms]
  (let [req-perms (set req-perms)
        resource-perms (set resource-perms)
        check (intersection resource-perms req-perms)]
    (= check req-perms)))

;;;;;;;;;;;;;;;;;;
;; Module status
;;;;;;;;;;;;;;;;;;

(defn get-enabled-modules
  []
  (get-state-value [:internal :enabled-modules]))

(defn set-module-enabled!
  [module-name]
  (let [modules (get-enabled-modules)
        appended (conj modules module-name)]
    (swap! sdk-state assoc-in [:internal :enabled-modules] appended)))
