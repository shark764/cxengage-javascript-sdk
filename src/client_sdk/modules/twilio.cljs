(ns client-sdk.modules.twilio
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [lumbajack.core :refer [log]]
            [client-sdk-utils.core :as u]))

(def module-state (atom {}))

(defn handle-incoming
  [call]
  (.accept call))

(defn ^:private twilio-init
  [config done-init< on-msg-fn]
  (let [{:keys [jsApiUrl credentials]} config
        {:keys [token]} credentials
        script (js/document.createElement "script")
        body (.-body js/document)]
      (.setAttribute script "type" "text/javascript")
      (.setAttribute script "src" jsApiUrl)
      (.appendChild body script)
      (go (a/<! (a/timeout 1000)) ;TODO - make this not dumb
          (js/Twilio.Device.setup token)
          (js/Twilio.Device.incoming (partial on-msg-fn :TWILIO/INCOMING))
          (js/Twilio.Device.ready (partial on-msg-fn :TWILIO/READY))
          (js/Twilio.Device.offline (partial on-msg-fn :TWILIO/OFFLINE))
          (js/Twilio.Device.cancel (partial on-msg-fn :TWILIO/CANCEL))
          (js/Twilio.Device.connect (partial on-msg-fn :TWILIO/CONNECT))
          (js/Twilio.Device.disconnect (partial on-msg-fn :TWILIO/DISCONNECT))
          (js/Twilio.Device.error (partial on-msg-fn :TWILIO/ERROR)))
      (a/put! done-init< {:status :ok})))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in Twilio SDK module." (:type message)))))

(defn module-shutdown-handler [message]
  (log :info "Received shutdown message from Core - Twilio Module shutting down...."))

(defn init
  [env done-init< config on-msg-fn]
  (log :info "Initializing SDK module: Twilio")
  (swap! module-state assoc :env env)
  (swap! module-state assoc :config config)
  (let [module-inputs< (a/chan 1024)
        module-shutdown< (a/chan 1024)
        twilio-config (first (filter #(= (:type %) "twilio") (:integrations config)))]
    (u/start-simple-consumer! module-inputs< module-router)
    (u/start-simple-consumer! module-shutdown< module-shutdown-handler)
    (twilio-init twilio-config done-init< on-msg-fn)
    {:messages module-inputs<
     :shutdown module-shutdown<}))
