(ns client-sdk.modules.presence
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [client-sdk-utils.core :as u]))

(def module-state (atom {}))

(defn send-heartbeats
  [session tenant-id user-id token resp-chan]
  (log :debug "Sending heartbeats...")
  (let [{:keys [sessionId]} session
        heartbeat-req-map {:method :post
                           :body {:sessionId sessionId}
                           :url (u/api-url (:env @module-state)
                                           (str "/tenants/" tenant-id "/presence/" user-id "/heartbeat"))
                           :token token}]
    (go-loop [first-heartbeat? true
              next-heartbeat-resp-chan (a/promise-chan)]
      (u/api-request (merge heartbeat-req-map {:resp-chan next-heartbeat-resp-chan}))
      (let [{:keys [result]} (a/<! next-heartbeat-resp-chan)
            next-heartbeat-delay (* 1000 (or (:heartbeatDelay result) 30))]
        (log :debug "Heartbeat sent!")
        (when first-heartbeat?
          (a/put! resp-chan (merge result session)))
        (a/<! (a/timeout next-heartbeat-delay))
        (recur false (a/promise-chan))))))

(defn change-presence-state
  [result-chan message]
  (let [{:keys [token resp-chan user-id tenant-id]} message
        body (select-keys message [:state :reasonId :reasonListId :sessionId])
        req-map {:method :post
                 :body body
                 :resp-chan result-chan
                 :url (u/api-url (:env @module-state)
                                 (str "/tenants/" tenant-id "/presence/" user-id))
                 :token token}]
    (u/api-request req-map)
    (go (let [response (a/<! result-chan)]
          (log :fatal "PRESENCE RESPONSE " response)
          (a/put! resp-chan response)))))

(defn start-session
  [result-chan message]
  (let [{:keys [token user-id tenant-id resp-chan]} message
        req-map {:method :post
                 :url (u/api-url (:env @module-state)
                                 (str "/tenants/" tenant-id "/presence/" user-id "/session"))
                 :token token
                 :resp-chan result-chan}]
    (u/api-request req-map)
    (go (let [{:keys [result]} (a/<! result-chan)]
          (send-heartbeats result tenant-id user-id token resp-chan)))))

(defn set-direction
  [result-chan message]
  (let [{:keys [resp-chan tenant-id user-id token]} message
        body (select-keys message [:sessionId :initiatorId :direction])
        req-map {:method :post
                 :body body
                 :url (u/api-url (:env @module-state)
                                 (str "/tenants/" tenant-id "/presence/" user-id "/direction"))
                 :token token
                 :resp-chan result-chan}]
    (u/api-request req-map)
    (go (let [result (a/<! result-chan)]
          (a/put! resp-chan result)))))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :SESSION/START_SESSION (partial start-session (a/promise-chan))
                      :SESSION/CHANGE_STATE (partial change-presence-state (a/promise-chan))
                      :SESSION/SET_DIRECTION (partial set-direction (a/promise-chan))
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in Presence SDK module." (:type message)))))

(defn module-shutdown-handler [message]
  (log :debug "Received shutdown message from Core - Presence Module shutting down...."))

(defn init [env]
  (log :debug "Initializing SDK module: Presence")
  (swap! module-state assoc :env env)
  (let [module-inputs< (a/chan 1024)
        module-shutdown< (a/chan 1024)]
    (u/start-simple-consumer! module-inputs< module-router)
    (u/start-simple-consumer! module-shutdown< module-shutdown-handler)
    {:messages module-inputs<
     :shutdown module-shutdown<}))
