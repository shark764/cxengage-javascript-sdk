(ns cxengage-javascript-sdk.modules.smooch-messaging
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]])
  (:require [cljsjs.paho]
            [cljs.core.async :as a]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cognitect.transit :as t]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.pubsub :as p]
            [cljs.spec.alpha :as s])
  (:import goog.crypt))

(s/def ::add-attachment-params
  (s/keys :req-un [::specs/interaction-id ::specs/file]
          :opt-un [::specs/callback]))

(def-sdk-fn add-attachment
  "``` javascript
  CxEngage.interactions.smoochMessaging.addAttachment({
    interactionId: {{uuid}}, (required)
    file: {{HTML5 File}}
  });
  ```

  Store files attached temporary for current interaction.

  Topic: cxengage/interactions/smooch-messaging/attachment-added"
  {:validation ::add-attachment-params
   :topic-key :smooch-add-attachment}
  [params]
  (let [{:keys [topic interaction-id file callback]} params
        attachment-id (id/uuid-string (id/make-random-uuid))]
    (state/smooch-add-attachment-to-conversation {:interaction-id interaction-id
                                                  :attachment-id attachment-id
                                                  :file file})
    (p/publish {:topics topic
                :response {:interaction-id interaction-id
                           :attachment-id attachment-id
                           :filename (.-name file)}
                :callback callback})))

(s/def ::attachment-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/agent-message-id ::specs/callback]))

(def-sdk-fn remove-attachment
  "``` javascript
  CxEngage.interactions.smoochMessaging.removeAttachment({
    interactionId: {{uuid}} (required)
  });
  ```

  Remove stored file attached for current interaction.

  Topic: cxengage/interactions/smooch-messaging/attachment-removed"
  {:validation ::attachment-params
   :topic-key :smooch-remove-attachment}
  [params]
  (let [{:keys [topic interaction-id callback]} params]
    (state/smooch-remove-attachment-from-conversation {:interaction-id interaction-id})
    (p/publish {:topics topic
                :response {:interaction-id interaction-id}
                :callback callback})))

(def-sdk-fn send-attachment
  "``` javascript
  CxEngage.interactions.smoochMessaging.sendAttachment({
    interactionId: {{uuid}} (required),
    agentMessageId: {{uuid}} Identifier of agent's pending message.
  });
  ```

  Sends an attachment to all participants in the interaction.

  Topic: cxengage/interactions/smooch-messaging/attachment-sent"
  {:validation ::attachment-params
   :topic-key :smooch-send-attachment}
  [params]
  (let [{:keys [topic interaction-id agent-message-id callback]} params
        file (state/get-smooch-conversation-attachment {:interaction-id interaction-id})
        {:keys [api-response status] :as smooch-response} (a/<! (rest/send-smooch-attachment interaction-id agent-message-id file))
        error-response (cond
                          (= status 410) (e/failed-to-end-interaction-err interaction-id smooch-response)
                          (not= status 200) (e/failed-to-send-smooch-attachment interaction-id agent-message-id file (get-in api-response [:response :message]))
                          :else nil)]
    (p/publish {:topics topic
                :response api-response
                :error error-response
                :callback callback})))

(s/def ::send-message-params
  (s/keys :req-un [::specs/interaction-id
                   ::specs/message]
          :opt-un [::specs/agent-message-id
                   ::specs/callback]))

(def-sdk-fn send-message
  "``` javascript
  CxEngage.interactions.smoochMessaging.sendMessage({
    interactionId: {{uuid}}, (required)
    message: {{string}}, (required)
    agentMessageId: {{string}} Identifier of agent's pending message.
  });
  ```

  Sends a message to all participants in the interaction.

  Topic: cxengage/interactions/smooch-messaging/message-received"
  {:validation ::send-message-params
   :topic-key :smooch-message-received}
  [params]
  (let [{:keys [interaction-id message agent-message-id topic callback]} params
        {:keys [api-response status] :as smooch-response} (a/<! (rest/send-smooch-message interaction-id agent-message-id message))
        error-response (cond
                          (= status 410) (e/failed-to-end-interaction-err interaction-id smooch-response)
                          (not= status 200) (e/failed-to-send-smooch-message interaction-id agent-message-id message)
                          :else nil)]
    (p/publish {:topics topic
                :response api-response
                :error error-response
                :callback callback})))

(s/def ::send-conversation-read-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn send-conversation-read
  "``` javascript
    CxEngage.interactions.smoochMessaging.sendConversationRead({
      interactionId: {{uuid}}, (required)
    });
  ```
  Sends a conversation read event to all participants in the interaction.

  Topic: cxengage/interactions/smooch-messaging/conversation-read-agent-received

  Possible Errors:

  - [Messaging: 6008] (/cxengage-javascript-sdk.domain.errors.html#var-failed-to-send-smooch-conversation-read)"
  {:validation ::send-conversation-read-params
   :topic-key :smooch-conversation-read-agent-received}
  [params]
  (let [{:keys [interaction-id topic callback]} params
        smooch-response (a/<! (rest/send-smooch-conversation-read interaction-id))
        {:keys [api-response status]} smooch-response
        error (if-not (= 200 status) (e/failed-to-send-smooch-conversation-read interaction-id))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

(s/def ::send-typing-indicator-params
  (s/keys :req-un [::specs/interaction-id
                   ::specs/typing]
          :opt-un [::specs/callback]))

(def-sdk-fn send-typing-indicator
  "``` javascript
    CxEngage.interactions.smoochMessaging.sendTypingIndicator({
      interactionId: {{uuid}}, (required)
      typing: {{boolean}}, (required)
    });
  ```
  Sends a typing indicator to all participants in the interaction.

  Topic: cxengage/interactions/smooch-messaging/typing-agent-received

  Possible Errors:

  - [Messaging: 6009] (/cxengage-javascript-sdk.domain.errors.html#var-failed-to-send-smooch-typing)"
  {:validation ::send-typing-indicator-params
   :topic-key :smooch-typing-agent-received}
  [params]
  (let [{:keys [interaction-id typing topic callback]} params
        smooch-response (a/<! (rest/send-smooch-typing interaction-id typing))
        {:keys [api-response status]} smooch-response
        error (if-not (= status 200) (e/failed-to-send-smooch-typing interaction-id typing))]
    (p/publish {:topics topic
                :response api-response
                :error error
                :callback callback})))

;; -------------------------------------------------------------------------- ;;
;; SDK Smooch Messaging Module
;; -------------------------------------------------------------------------- ;;

(defrecord SmoochMessagingModule []
  pr/SDKModule
  (start [this]
    (let [module-name :smooch-messaging]
      (ih/register {:api {:interactions {:smooch-messaging {:send-message send-message
                                                            :send-conversation-read send-conversation-read
                                                            :send-typing-indicator send-typing-indicator
                                                            :add-attachment add-attachment
                                                            :remove-attachment remove-attachment
                                                            :send-attachment send-attachment}}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
