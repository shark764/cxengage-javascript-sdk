(ns cxengage-javascript-sdk.modules.twilio
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cljs-sdk-utils.macros :refer [def-sdk-fn]]
                   [lumbajack.macros :refer [log]])
  (:require [cljsjs.paho]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.state :as state]
            [cljs-sdk-utils.interop-helpers :as ih]
            [cljs-sdk-utils.protocols :as pr]
            [cljs-sdk-utils.errors :as e]
            [cljs-sdk-utils.topics :as topics]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs.spec :as s]))

;; -------------------------------------------------------------------------- ;;
;; Twilio Initialization Functions
;; -------------------------------------------------------------------------- ;;

(defn update-twilio-connection [connection]
  (state/set-twilio-connection connection))

(defn handle-twilio-error [script config error]
  (log :error error script config))

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
                              (state/set-twilio-device
                               (js/Twilio.Device.setup token #js {"debug" debug-twilio?
                                                                  "closeProtection" true
                                                                  "warnings" true
                                                                  "region" region}))
                              (js/Twilio.Device.incoming update-twilio-connection)
                              (js/Twilio.Device.ready update-twilio-connection)
                              (js/Twilio.Device.cancel update-twilio-connection)
                              (js/Twilio.Device.offline update-twilio-connection)
                              (js/Twilio.Device.disconnect update-twilio-connection)
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
        (.catch (fn [err] (e/no-microphone-access-error))))))

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
                {:keys [status api-response]} (a/<! (rest/get-config-request))
                {:keys [result]} api-response
                {:keys [integrations]} result
                twilio-integration (peek (filterv #(= (:type %1) "twilio") integrations))
                twilio-token (get-in twilio-integration [:credentials :token])]
            (state/update-integration "twilio" twilio-integration)
            (if (not= status 200)
              (p/publish {:topics topic
                          :error (e/failed-to-refresh-twilio-integration-err)})
              (state/set-twilio-device (js/Twilio.Device.setup twilio-token))))
          (recur))))))
