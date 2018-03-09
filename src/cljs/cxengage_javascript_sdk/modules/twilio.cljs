;; Twilio documentation: https://www.twilio.com/docs/api/client/twilio-js

(ns cxengage-javascript-sdk.modules.twilio
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn]]
                   [lumbajack.macros :refer [log]])
  (:require [cljsjs.paho]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs.spec.alpha :as s]))

;; -------------------------------------------------------------------------- ;;
;; Twilio Initialization Functions
;; -------------------------------------------------------------------------- ;;

(defn on-twilio-incoming [connection]
  (log :debug "Twilio incoming. Connection:" connection)
  (state/set-twilio-connection connection)
  (state/set-twilio-state "incoming")
  (when (state/get-supervisor-mode)
    (.accept connection)))

(defn on-twilio-connect [connection]
  (log :debug "Twilio connect. Connection:" connection)
  (state/set-twilio-connection connection)
  (state/set-twilio-state "connect"))

(defn on-twilio-ready [device]
  (log :debug "Twilio ready. Device:" device)
  (state/set-twilio-device device)
  (state/set-twilio-state "ready")
  (when (state/get-supervisor-mode)
    (p/publish {:topics (topics/get-topic :twilio-device-ready)
                :response {}})))

(defn on-twilio-cancel [connection]
  (log :debug "Twilio cancel. Connection:" connection)
  (state/set-twilio-connection connection)
  (state/set-twilio-state "cancel"))

(defn on-twilio-offline [device]
  (log :debug "Twilio offline. Device:" device)
  (state/set-twilio-device device)
  (state/set-twilio-state "offline"))

(defn on-twilio-disconnect [connection]
  (log :debug "Twilio disconnect. Connection:" connection)
  (state/set-twilio-connection connection)
  (state/set-twilio-state "disconnect"))

(defn handle-twilio-error [error]
  (log :error "Twilio error" error)
  (p/publish {:topics "cxengage/errors/error/twilio-device-error"
              :error (e/failed-to-init-twilio-err error)}))

(defn ^:private twilio-init
  [config]
  (let [audio-params (ih/camelify {"audio" true})
        script-init (fn [& args]
                      (let [{:keys [js-api-url credentials regions]} config
                            region (or (first regions) "gll")
                            {:keys [token]} credentials
                            script (js/document.createElement "script")
                            body (.-body js/document)
                            debug-twilio? (= (keyword (ih/get-log-level)) :debug)]
                        (.setAttribute script "type" "text/javascript")
                        (.setAttribute script "src" js-api-url)
                        (.appendChild body script)
                        ;; The active user's Twilio token as well as the specific twilio
                        ;; SDK version is retrieved from their config details. The url
                        ;; to the twilio SDK is then appended to the window on a script
                        ;; element - which allows us to further initialize the Twilio
                        ;; Device in order to receive voice interactions.
                        (go-loop []
                          (if (ih/twilio-ready?)
                            (do
                              (try
                                (state/set-twilio-device
                                 (js/Twilio.Device.setup token #js {"debug" debug-twilio?
                                                                    "closeProtection" true
                                                                    "warnings" true
                                                                    "region" region}))
                                (catch js/Object e
                                  (handle-twilio-error e)))
                              (js/Twilio.Device.incoming on-twilio-incoming)
                              (js/Twilio.Device.connect on-twilio-connect)
                              (js/Twilio.Device.ready on-twilio-ready)
                              (js/Twilio.Device.cancel on-twilio-cancel)
                              (js/Twilio.Device.offline on-twilio-offline)
                              (js/Twilio.Device.disconnect on-twilio-disconnect)
                              (js/Twilio.Device.error handle-twilio-error))
                            (do (a/<! (a/timeout 250))
                                (recur))))))]
    (-> js/navigator
        (.-mediaDevices)
        (.getUserMedia audio-params)
        (.then script-init)
        ;; If a user "blocks" microphone access through their browser
        ;; it causes issues with Twilio. This is a our way of detecting
        ;; and notifying the user of this problem.
        (.catch (fn [err] (e/no-microphone-access-err err))))))

;; -------------------------------------------------------------------------- ;;
;; SDK Twilio Module
;; -------------------------------------------------------------------------- ;;

(defrecord TwilioModule []
  pr/SDKModule
  (start [this]
    (let [module-name :twilio
          twilio-integration (state/get-integration-by-type "twilio")]
      (if-not twilio-integration
        (log :info "<----- Twilio integration not found, not starting module ----->")
        (do (twilio-init twilio-integration)
            (ih/send-core-message {:type :module-registration-status
                                   :status :success
                                   :module-name module-name})))))
  (stop [this])
  (refresh-integration [this]
    (go-loop []
      (if-not (state/get-integration-by-type "twilio")
        nil
        (let [twilio-token-ttl (get-in (state/get-integration-by-type "twilio") [:credentials :ttl])
              min-ttl (* twilio-token-ttl 500)]
          (a/<! (a/timeout min-ttl))
          (let [topic (topics/get-topic :config-response)
                {:keys [status api-response] :as config-response} (a/<! (rest/get-config-request))
                {:keys [result]} api-response
                {:keys [integrations]} result
                twilio-integration (peek (filterv #(= (:type %1) "twilio") integrations))
                twilio-token (get-in twilio-integration [:credentials :token])]
            (state/update-integration "twilio" twilio-integration)
            (log :info "Refreshing Twilio token")
            (if (not= status 200)
              (p/publish {:topics topic
                          :error (e/failed-to-refresh-twilio-integration-err config-response)})
              (state/set-twilio-device (js/Twilio.Device.setup twilio-token))))
          (recur))))))
