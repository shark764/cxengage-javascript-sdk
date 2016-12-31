(ns client-sdk.messaging
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [lumbajack.core :refer [log]]
            [client-sdk-utils.core :as u]))

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
          (log :debug "messaging module got response for get history:" result)
          (a/put! resp-chan result)))))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :MESSAGING/GET_HISTORY get-history
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in Messaging SDK module." (:type message)))))

(defn init [env]
  (swap! module-state assoc :env env)
  (let [module-inputs< (a/chan 2014)]
    (u/start-simple-consumer! module-inputs< module-router)
    module-inputs<))
