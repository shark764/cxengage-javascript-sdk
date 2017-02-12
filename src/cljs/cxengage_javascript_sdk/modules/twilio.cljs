(ns cxengage-javascript-sdk.modules.twilio
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cxengage-cljs-utils.core :as u]
            [cxengage-javascript-sdk.state :as state]))

(def module-state (atom {}))

(defn update-twilio-connection [connection]
  (state/set-twilio-connection connection))

(defn handle-twilio-error [script config error]
  (log :error error.message " for " error.connection))

(defn ^:private twilio-init
  [config done-init< on-msg-fn]
  (let [{:keys [jsApiUrl credentials]} config
        {:keys [token]} credentials
        script (js/document.createElement "script")
        body (.-body js/document)]
    (.setAttribute script "type" "text/javascript")
    (.setAttribute script "src" jsApiUrl)
    (.appendChild body script)
    (go-loop []
      (if (aget js/window "Twilio")
        (do
          (state/set-twilio-device (js/Twilio.Device.setup token))
          (js/Twilio.Device.incoming update-twilio-connection)
          (js/Twilio.Device.ready update-twilio-connection)
          (js/Twilio.Device.cancel update-twilio-connection)
          (js/Twilio.Device.offline update-twilio-connection)
          (js/Twilio.Device.disconnect update-twilio-connection)
          (js/Twilio.Device.error handle-twilio-error))
        (do (a/<! (a/timeout 250))
            (recur))))))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :TWILIO/INIT twilio-init
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in Twilio SDK module." (:type message)))))

(defn module-shutdown-handler [message]
  (log :info "Received shutdown message from Core - Twilio Module shutting down...."))

(defn init
  [env done-init< config on-msg-fn err-chan]
  (log :debug "Initializing SDK module: Twilio")
  (swap! module-state assoc :env env)
  (swap! module-state assoc :config config)
  (swap! module-state assoc :error-channel err-chan)
  (let [module-inputs< (a/chan 1024)
        module-shutdown< (a/chan 1024)
        twilio-config (first (filter #(= (:type %) "twilio") (:integrations config)))]
    (u/start-simple-consumer! module-inputs< module-router)
    (u/start-simple-consumer! module-shutdown< module-shutdown-handler)
    (twilio-init twilio-config done-init< on-msg-fn)
    {:messages module-inputs<
     :shutdown module-shutdown<}))
