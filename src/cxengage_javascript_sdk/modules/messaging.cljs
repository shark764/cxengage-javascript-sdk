(ns cxengage-javascript-sdk.modules.messaging
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [lumbajack.core]
            [cxengage-cljs-utils.core :as u]))

(def module-state (atom {}))

(defn get-channel-metadata [message])

(defn get-history [message]
  (let [request-chan (a/promise-chan)
        {:keys [tenantId interactionId token resp-chan]} message
        request-map {:method :get
                     :url (u/api-url (:env @module-state)
                                     (str "/messaging/tenants/" tenantId "/channels/" interactionId "/history"))
                     :token token
                     :resp-chan request-chan}]
    (u/api-request request-map)
    (go (let [{:keys [result]} (a/<! request-chan)]
          (a/put! resp-chan result)))))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :MESSAGING/GET_HISTORY get-history
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in Messaging SDK module." (:type message)))))

(defn module-shutdown-handler [message]
  (log :info "Received shutdown message from Core - Messaging Module shutting down...."))

(defn init [env]
  (log :debug "Initializing SDK module: Messaging")
  (swap! module-state assoc :env env)
  (let [module-inputs< (a/chan 1024)
        module-shutdown< (a/chan 1024)]
    (u/start-simple-consumer! module-inputs< module-router)
    (u/start-simple-consumer! module-shutdown< module-shutdown-handler)
    {:messages module-inputs<
     :shutdown module-shutdown<}))
