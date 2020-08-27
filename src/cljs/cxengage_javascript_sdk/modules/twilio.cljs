;; Twilio documentation: https://www.twilio.com/docs/api/client/twilio-js

(ns cxengage-javascript-sdk.modules.twilio
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]])
  (:require [cljsjs.paho]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs.spec.alpha :as s]))

;; -------------------------------------------------------------------------- ;;
;; Twilio Initialization Functions
;; -------------------------------------------------------------------------- ;;

(defn- on-twilio-incoming [connection]
  (log :debug "Twilio incoming. Connection:" connection)
  (state/set-twilio-connection connection)
  (state/set-twilio-state "incoming")
  (when (state/get-supervisor-mode)
    (.accept connection)))

(defn- on-twilio-connect [connection]
  (log :debug "Twilio connect. Connection:" connection)
  (state/set-twilio-connection connection)
  (state/set-twilio-state "connect"))

(defn- on-twilio-ready [device]
  (log :debug "Twilio ready. Device:" device)
  (state/set-twilio-device device)
  (state/set-twilio-state "ready")
  (when (state/get-supervisor-mode)
    (p/publish {:topics (topics/get-topic :twilio-device-ready)
                :response {}})))

(defn- on-twilio-cancel [connection]
  (log :debug "Twilio cancel. Connection:" connection)
  (state/set-twilio-connection connection)
  (state/set-twilio-state "cancel"))

(defn- on-twilio-offline [device]
  (log :debug "Twilio offline. Device:" device)
  (state/set-twilio-device device)
  (state/set-twilio-state "offline"))

(defn- on-twilio-disconnect [connection]
  (log :debug "Twilio disconnect. Connection:" connection)
  (state/set-twilio-connection connection)
  (state/set-twilio-state "disconnect"))

(defn- handle-twilio-error [error]
  (log :error "Twilio error" error)
  (p/publish {:topics (topics/get-topic :twilio-device-error)
              :error (e/failed-to-init-twilio-err error)}))

(defn- handle-device-change [lost-active-devices]
  ;; The parameter, lostActiveDevices, is an array of MediaDeviceInfo objects that represents
  ;; all devices that were currently active in either .speakerDevices or .ringtoneDevices
  ;; at the time they were lost, if any.
  ;; A non-empty array is an indicator that the user’s experience was likely affected by this event.
  ;; https://www.twilio.com/docs/voice/client/javascript/device#event-twiliodeviceaudiodevicechange
  (when (not= 0 (aget lost-active-devices "length"))
    (log :warn "Twilio audio. Lost devices:" lost-active-devices))
  (let [available-output-devices (aget js/Twilio.Device "audio" "availableOutputDevices")]
    (log :info "[Twilio] New output devices available:" (clj->js available-output-devices))
    (p/publish {:topics (topics/get-topic :twilio-output-devices-changed)
                            ;; New list of available output devices
                            ;; Returns a Map containing the MediaDeviceInfo object of all available output devices
                            ;; (hardware devices capable of outputting audio), indexed by deviceId.
                            ;; https://www.twilio.com/docs/voice/client/javascript/device#audioavailableoutputdevices
                :response {:available-output-devices available-output-devices
                            ;; New list of active output devices
                            ;; Back to default if active was just unplugged
                           :active-output-ringtone-devices (.get (aget js/Twilio.Device "audio" "ringtoneDevices"))
                           :active-output-speaker-devices (.get (aget js/Twilio.Device "audio" "speakerDevices"))
                            ;; Disconnected devices
                           :lost-active-devices lost-active-devices}})))

(s/def ::set-active-output-devices-params
  (s/keys :req-un [::specs/device-ids]
          :opt-un [::specs/callback]))

(def-sdk-fn set-active-output-ringtone-devices
  "``` javascript
  CxEngage.twilio.setActiveOutputRingtoneDevices({
    deviceIds: {{array | string}} (required)
  });
  ```

  Receives an array and sets active devices
  Example: ['default', '51dbcf089be6a1a35e5226a0b1a7841e8d9e27696d421d3285073e1666a0331f']

  Returns a Set<MediaDeviceInfo> of the current ringtoneDevices
  https://www.twilio.com/docs/voice/client/javascript/device#quick-api-reference_1

  Topic: cxengage/twilio/active-output-devices-changed

  Possible Errors:

  - [Messaging: 8004] (/cxengage-javascript-sdk.domain.errors.html#var-failed-to-change-twilio-active-output-ringtone-devices-err)"
  {:validation ::set-active-output-devices-params
   :topic-key :twilio-active-output-devices-changed}
  [params]
  (let [{:keys [topic device-ids callback]} params
        device-ids (clj->js device-ids)]
    ;; Set active device(s)
    ;; https://www.twilio.com/docs/voice/client/javascript/device#quick-api-reference
    ;;
    ;; ringtoneDevices is an AudioOutputCollection that controls which output devices are used to play
    ;; the ringing sound when receiving an incoming call. Changing this set of devices will switch
    ;; the devices used for the incoming call sound.
    ;; https://www.twilio.com/docs/voice/client/javascript/device#twiliodeviceaudioringtonedevices
    (-> js/Twilio.Device
      (aget "audio" "ringtoneDevices")
      (.set device-ids)
      (.then
        (fn []
          ;; Returns a Set<MediaDeviceInfo>
          ;; https://www.twilio.com/docs/voice/client/javascript/device#quick-api-reference_1
          (let [active-output-ringtone-devices (.get (aget js/Twilio.Device "audio" "ringtoneDevices"))]
            (log :info "[Twilio] New output devices for ringtone have been set:" active-output-ringtone-devices)
            (p/publish {:topics topic
                        :response {:active-output-ringtone-devices active-output-ringtone-devices}
                        :callback callback}))))
      (.catch
        (fn [err]
          (p/publish {:topics topic
                      :error (e/failed-to-change-twilio-active-output-ringtone-devices-err err)
                      :callback callback}))))))

(def-sdk-fn set-active-output-speaker-devices
  "``` javascript
  CxEngage.twilio.setActiveOutputSpeakerDevices({
    deviceIds: {{array | string}} (required)
  });
  ```

  Receives an array and sets active devices
  Example: ['default', '51dbcf089be6a1a35e5226a0b1a7841e8d9e27696d421d3285073e1666a0331f']

  Returns a Set<MediaDeviceInfo> of the current speakerDevices
  https://www.twilio.com/docs/voice/client/javascript/device#quick-api-reference

  Topic: cxengage/twilio/active-output-devices-changed

  Possible Errors:

  - [Messaging: 8005] (/cxengage-javascript-sdk.domain.errors.html#var-failed-to-change-twilio-active-output-speaker-devices-err)"
  {:validation ::set-active-output-devices-params
   :topic-key :twilio-active-output-devices-changed}
  [params]
  (let [{:keys [topic device-ids callback]} params
        device-ids (clj->js device-ids)]
    ;; Set active device(s)
    ;; https://www.twilio.com/docs/voice/client/javascript/device#quick-api-reference
    ;;
    ;; speakerDevices is an AudioOutputCollection that controls which output devices are used to play standard speaker sounds:
    ;; the ringtone you hear when initiating a call, the disconnect sound, DTMF tones the user might press and the audio received from the remote participant.
    ;; Changing this set of devices will switch the device(s) used for these sounds.
    ;; If you change these during an active call, the remote participant’s audio will immediately be played through the new set of outputs.
    ;; https://www.twilio.com/docs/voice/client/javascript/device#twiliodeviceaudiospeakerdevices
    (-> js/Twilio.Device
      (aget "audio" "speakerDevices")
      (.set device-ids)
      (.then
        (fn []
          ;; Returns a Set<MediaDeviceInfo>
          ;; https://www.twilio.com/docs/voice/client/javascript/device#quick-api-reference
          (let [active-output-speaker-devices (.get (aget js/Twilio.Device "audio" "speakerDevices"))]
            (log :info "[Twilio] New output devices for speaker have been set:" active-output-speaker-devices)
            (p/publish {:topics topic
                        :response {:active-output-speaker-devices active-output-speaker-devices}
                        :callback callback}))))
      (.catch
        (fn [err]
          (p/publish {:topics topic
                      :error (e/failed-to-change-twilio-active-output-speaker-devices-err err)
                      :callback callback}))))))

(defn- twilio-init
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
                              ;; Twilio Client is moving toward the standard EventEmitter interface,
                              ;; meaning events should be managed with .on(eventName, handler) and .removeListener(eventName, handler),
                              ;; replacing our legacy handlers (such as .accept(handler), .error(handler), etc...).
                              ;; https://www.twilio.com/docs/voice/client/javascript/device#deprecated-methods
                              (js/Twilio.Device.on "incoming" on-twilio-incoming)
                              (js/Twilio.Device.on "connect" on-twilio-connect)
                              (js/Twilio.Device.on "ready" on-twilio-ready)
                              (js/Twilio.Device.on "cancel" on-twilio-cancel)
                              (js/Twilio.Device.on "offline" on-twilio-offline)
                              (js/Twilio.Device.on "disconnect" on-twilio-disconnect)
                              (js/Twilio.Device.on "error" handle-twilio-error)
                              ;; Register a handler that will be fired whenever a new audio device is found,
                              ;; an existing audio device is lost or the label of an existing device is changed.
                              ;; This typically happens when the user plugs in or unplugs an audio device, like a headset or a microphone.
                              ;; This will also trigger after the customer has given access to user media via getUserMedia for the first time,
                              ;; as the labels will become populated.
                              ;; https://www.twilio.com/docs/voice/client/javascript/device#event-twiliodeviceaudiodevicechange
                              (-> js/Twilio.Device
                                (aget "audio")
                                (.on "deviceChange" handle-device-change))
                              (ih/publish {:topics (topics/get-topic :twilio-initialization)
                                                      ;; False if the browser does not support setSinkId or enumerateDevices and Twilio
                                                      ;; can not facilitate output selection functionality.
                                                      ;; If changing device is not supported by the browser,
                                                      ;; then we disable feature on UI
                                                      ;; https://twilio.github.io/twilio-client.js/classes/voice.audiohelper.html#isoutputselectionsupported
                                           :response {:is-output-selection-supported (aget js/Twilio.Device "audio" "isOutputSelectionSupported")
                                                      ;; Get current available devices
                                                      ;; https://www.twilio.com/docs/voice/client/javascript/device#audioavailableoutputdevices
                                                      :available-output-devices (aget js/Twilio.Device "audio" "availableOutputDevices")
                                                      ;; Start with "default" as active ringtone device
                                                      ;; https://www.twilio.com/docs/voice/client/javascript/device#quick-api-reference_1
                                                      :active-output-ringtone-devices (.get (aget js/Twilio.Device "audio" "ringtoneDevices"))
                                                      ;; Start with "default" as active speaker device
                                                      ;; https://www.twilio.com/docs/voice/client/javascript/device#quick-api-reference
                                                      :active-output-speaker-devices (.get (aget js/Twilio.Device "audio" "speakerDevices"))}}))
                            (do (a/<! (a/timeout 250))
                                (recur))))))]
    (-> js/navigator
        (.-mediaDevices)
        (.getUserMedia audio-params)
        (.then script-init)
        ;; If a user "blocks" microphone access through their browser
        ;; it causes issues with Twilio. This is a our way of detecting
        ;; and notifying the user of this problem.
        (.catch
          (fn [err]
            (p/publish {:topics (topics/get-topic :twilio-microphone-not-available)
                        :error (e/no-microphone-access-err err)}))))))

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
            (ih/register {:api
                            {:twilio
                              {:set-active-output-ringtone-devices set-active-output-ringtone-devices
                               :set-active-output-speaker-devices set-active-output-speaker-devices}}
                          :module-name module-name})
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
