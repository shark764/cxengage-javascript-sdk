(ns cxengage-javascript-sdk.modules.interaction
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]])
  (:require [cljs.core.async :as a]
            [cljs.spec.alpha :as s]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.interaction-management :as int]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]))

(s/def ::generic-interaction-fn-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(s/def ::disposition-operation-params
  (s/keys :req-un [::specs/interaction-id ::specs/disposition-id]
          :opt-un [::specs/callback]))

(s/def ::contact-operation-params
  (s/keys :req-un [::specs/interaction-id ::specs/contact-id]
          :opt-un [::specs/callback]))

(defn- build-detailed-interrupt-body
  [interaction-id]
  (let [tenant-id (state/get-active-tenant-id)
        {:keys [sub-id action-id channel-type resource-id tenant-id
                resource direction timeout timeout-end]} (state/get-interaction interaction-id)
        {:keys [extension role-id session-id work-offer-id]} resource]
    {:tenant-id tenant-id
     :interaction-id interaction-id
     :sub-id sub-id
     :action-id action-id
     :work-offer-id work-offer-id
     :session-id session-id
     :resource-id resource-id
     :direction direction
     :channel-type channel-type}))

(def-sdk-fn end
  "The reject/end function is used to reject a work offer or end an interaction from the agent perspective and disconnect from the customer.

  ```javascript
  CxEngage.interactions.end({
    interactionId: '{{uuid}}'
  });

  // Same as
  CxEngage.interactions.reject({
    interactionId: '{{uuid}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4003](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-end-interaction-err)
  - [Twilio: 8002](/cxengage-javascript-sdk.domain.errors.html#var-force-killed-twilio-connection-err)
  "
  {:validation ::generic-interaction-fn-params
   :topic-key :interaction-end-acknowledged}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        interaction (state/get-interaction interaction-id)
        {:keys [channel-type]} interaction
        interrupt-body (if (= channel-type "voice")
                         {:resource-id (state/get-active-user-id)
                          :target-resource (state/get-active-user-id)}
                         {:resource-id (state/get-active-user-id)})
        interrupt-type (if (= channel-type "voice")
                         "remove-resource"
                         "resource-disconnect")
        resp (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))
        {:keys [api-response status]} resp]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (do (when (and (= channel-type "voice") (= status 404))
            (when-let [twilio-device (state/get-twilio-device)]
              (.disconnectAll twilio-device)
              (p/publish {:topics (topics/get-topic :force-killed-twilio-connection)
                          :error (e/force-killed-twilio-connection-err interaction-id)
                          :callback callback})))
          (p/publish {:topics topic
                      :error (e/failed-to-end-interaction-err interaction-id resp)
                      :callback callback})))))

(def-sdk-fn accept
  "The accept function is used to accept a work offer/interaction and initiate communication with the customer.

  ```javascript
  CxEngage.interactions.accept({
    interactionId: '{{uuid}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4001](/cxengage-javascript-sdk.domain.errors.html#var-work-offer-expired-err)
  - [Interaction: 4004](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-accept-interaction-err)
  - [Twilio: 8002](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-find-twilio-connection-object)
  "
  {:validation ::generic-interaction-fn-params
   :topic-key :interaction-accept-acknowledged}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        tenant-id (state/get-active-tenant-id)
        interrupt-body {:resource-id (state/get-active-user-id)}
        interrupt-type "offer-accept"
        {:keys [timeout timeout-end channel-type source]} (state/get-interaction interaction-id)
        {:keys [status] :as resp} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (not= status 200)
      (p/publish {:topics topic
                  :error (e/failed-to-accept-interaction-err interaction-id resp)
                  :callback callback})
      (do (p/publish {:topics topic
                      :response (merge {:interaction-id interaction-id} interrupt-body)
                      :callback callback})
          (if (<= (js/Date.parse (or timeout timeout-end)) (iu/get-now))
            (p/publish {:topics topic
                        :error (e/work-offer-expired-err)
                        :callback callback})
            (cond 
              (and (= channel-type "voice")
                   (= (:provider (state/get-active-extension)) "twilio"))
              (go-loop [t (a/timeout 1000)
                        attempts 1]
                ;; Wait 35 seconds for Twilio to be in an incoming state after the accept, to account for network latencies, etc.
                (if (= attempts 35)
                  (p/publish {:topics topic
                              :error (e/failed-to-find-twilio-connection-object interaction-id)
                              :callback callback})
                  (let [connection (state/get-twilio-connection)
                        twilio-state (state/get-twilio-state)]
                    (if (and connection
                          (.-accept connection)
                          (= "incoming" twilio-state))
                      (do
                        (log :debug "Accepting Twilio connection.")
                        (.accept connection))
                      (do
                        (log :debug "Twilio not in an incoming state to accept. Waiting 1 second to try again." (str "State: " twilio-state) "Connection: " connection)
                        (a/<! t)
                        (recur (a/timeout 1000)
                               (inc attempts)))))))
              (= "smooch" source)
              (int/get-smooch-history interaction-id)
              (or (= channel-type "sms")
                  (= channel-type "messaging"))
              (int/get-messaging-history interaction-id)))))))

(def-sdk-fn focus
  "The focus function is used to signal reporting that the agent is actively viewing the interaction.

  ```javascript
  CxEngage.interactions.focus({
    interactionId: '{{uuid}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4005](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-focus-interaction-err)
  "
  {:validation ::generic-interaction-fn-params
   :topic-key :interaction-focus-acknowledged}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        interrupt-body (build-detailed-interrupt-body interaction-id)
        interrupt-type "interaction-focused"
        {:keys [status] :as resp} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-focus-interaction-err interaction-id resp)
                  :callback callback}))))

(def-sdk-fn unfocus
  "The unfocus function is used to signal reporting that the agent is no longer viewing the interaction.

  ```javascript
  CxEngage.interactions.unfocus({
    interactionId: '{{uuid}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4006](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-unfocus-interaction-err)
  "
  {:validation ::generic-interaction-fn-params
   :topic-key :interaction-unfocus-acknowledged}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        interrupt-body (build-detailed-interrupt-body interaction-id)
        interrupt-type "interaction-unfocused"
        {:keys [status] :as resp} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-unfocus-interaction-err interaction-id resp)
                  :callback callback}))))

(def-sdk-fn assign
  "The assignContact function is used to assign a Skylight CRM contact to the interaction.

  ```javascript
  CxEngage.interactions.assignContact({
    interactionId: '{{uuid}}',
    contactId: '{{uuid}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4007](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-assign-contact-to-interaction-err)"
  {:validation ::contact-operation-params
   :topic-key :contact-assignment-acknowledged}
  [params]
  (let [{:keys [callback topic interaction-id contact-id]} params
        interrupt-body (assoc (build-detailed-interrupt-body interaction-id) :contact-id contact-id)
        interrupt-type "interaction-contact-selected"
        {:keys [status] :as resp} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-assign-contact-to-interaction-err interaction-id resp)
                  :callback callback}))))

(def-sdk-fn unassign
  "The unassignContact function is used to unassign a Skylight CRM contact from the interaction.

  ```javascript
  CxEngage.interactions.unassignContact({
    interactionId: '{{uuid}}',
    contactId: '{{uuid}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4008](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-unassign-contact-from-interaction-err)"
  {:validation ::contact-operation-params
   :topic-key :contact-unassignment-acknowledged}
  [params]
  (let [{:keys [callback topic interaction-id contact-id]} params
        interrupt-body (assoc (build-detailed-interrupt-body interaction-id) :contact-id contact-id)
        interrupt-type "interaction-contact-deselected"
        {:keys [status] :as resp} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-unassign-contact-from-interaction-err interaction-id resp)
                  :callback callback}))))

(def-sdk-fn enable-wrapup
  "The enableWrapup function is used during an interaction to turn wrap up on following the end of the interaction.
  This should only be called when the interaction has 'Allow Wrap Up Update' configured on the interaction.

  ```javascript
  CxEngage.interactions.enableWrapup({
    interactionId: '{{uuid}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4009](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-enable-wrapup-err)
  "
  {:validation ::generic-interaction-fn-params
   :topic-key :enable-wrapup-acknowledged}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        interrupt-body {:resource-id (state/get-active-user-id)}
        interrupt-type "wrapup-on"
        {:keys [status] :as resp} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-enable-wrapup-err interaction-id resp)
                  :callback callback}))))

(def-sdk-fn disable-wrapup
  "The disableWrapup function is used during an interaction to turn wrap up off on following the end of the interaction.
  This should only be called when the interaction has 'Allow Wrap Up Update' configured on the interaction.

  ```javascript
  CxEngage.interactions.disableWrapup({
    interactionId: '{{uuid}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4010](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-disable-wrapup-err)
  "
  {:validation ::generic-interaction-fn-params
   :topic-key :disable-wrapup-acknowledged}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        interrupt-body {:resource-id (state/get-active-user-id)}
        interrupt-type "wrapup-off"
        {:keys [status] :as resp} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-disable-wrapup-err interaction-id resp)
                  :callback callback}))))

(def-sdk-fn end-wrapup
  "The endWrapup function can be used during wrap up to end the wrap up phase of the interaction,
  freeing the resource for new work.

  ```javascript
  CxEngage.interactions.endWrapup({
    interactionId: '{{uuid}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4011](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-end-wrapup-err)"
  {:validation ::generic-interaction-fn-params
   :topic-key :end-wrapup-acknowledged}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        interrupt-body {:resource-id (state/get-active-user-id)}
        interrupt-type "wrapup-end"
        {:keys [status] :as resp} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-end-wrapup-err interaction-id resp)
                  :callback callback}))))

(def-sdk-fn deselect-disposition
  "The deselectDispositionCode is used to remove all selected dispositions from the interaction.

  ```javascript
  CxEngage.interactions.deselectDispositionCode({
    interactionId: '{{uuid}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4012](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-deselect-disposition-err)"
  {:validation ::generic-interaction-fn-params
   :topic-key :disposition-code-changed}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        interrupt-body {:resource-id (state/get-active-user-id)}
        interrupt-type "disposition-select"
        {:keys [status] :as resp} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-deselect-disposition-err interaction-id resp)
                  :callback callback}))))

(def-sdk-fn select-disposition
  "The selectDispositionCode is used to assign a disposition to the interaction.

  ```javascript
  CxEngage.interactions.selectDispositionCode({
    interactionId: '{{uuid}}',
    dispositionId: '{{uuid}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4020](/cxengage-javascript-sdk.domain.errors.html#var-invalid-disposition-provided-err)
  - [Interaction: 4013](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-select-disposition-err)"
  {:validation ::disposition-operation-params
   :topic-key :disposition-code-changed}
  [params]
  (let [{:keys [topic interaction-id disposition-id callback]} params
        dispositions (state/get-interaction-disposition-codes interaction-id)
        dv (filterv #(= (:disposition-id %1) disposition-id) dispositions)]
    (if (empty? dv)
      (p/publish {:topics topic
                  :error (e/invalid-disposition-provided-err dispositions interaction-id)
                  :callback callback})
      (let [disposition (first dv)
            interrupt-disposition (assoc disposition :selected true)
            interrupt-body {:resource-id (state/get-active-user-id)
                            :disposition interrupt-disposition}
            interrupt-type "disposition-select"
            {:keys [status] :as resp} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
        (if (= status 200)
          (p/publish {:topics topic
                      :response (merge {:interaction-id interaction-id} interrupt-body)
                      :callback callback})
          (p/publish {:topics topic
                      :error (e/failed-to-select-disposition-err interaction-id resp)
                      :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interaction.getNote({
;;  interactionId: {{uuid}},
;;  noteId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-one-note-params
  (s/keys :req-un [::specs/interaction-id ::specs/note-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-note
  ""
  {:validation ::get-one-note-params
   :topic-key :get-note-response}
  [params]
  (let [{:keys [callback topic interaction-id note-id]} params
        tenant-id (state/get-active-tenant-id)
        {:keys [status api-response] :as resp} (a/<! (rest/get-note-request interaction-id note-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-get-interaction-note-err interaction-id resp)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interaction.getAllNotes({
;;  interactionId: {{uuid}},
;;  noteId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::get-all-notes-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn get-all-notes
  ""
  {:validation ::get-all-notes-params
   :topic-key :get-notes-response}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        tenant-id (state/get-active-tenant-id)
        {:keys [status api-response] :as resp} (a/<! (rest/get-notes-request interaction-id))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-list-interaction-notes-err interaction-id resp)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interaction.updateNote({
;;  interactionId: {{uuid}},
;;  noteId: {{uuid}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::update-note-params
  (s/keys :req-un [::specs/interaction-id ::specs/note-id]
          :opt-un [::specs/callback ::specs/title ::specs/body ::specs/contact-id]))

(def-sdk-fn update-note
  ""
  {:validation ::update-note-params
   :topic-key :update-note-response}
  [params]
  (let [{:keys [callback topic interaction-id note-id]} params
        tenant-id (state/get-active-tenant-id)
        body (select-keys params [:title :body :contact-id])
        {:keys [status api-response] :as resp} (a/<! (rest/update-note-request interaction-id note-id body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-update-interaction-note-err interaction-id resp)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interaction.createNote({
;;  interactionId: {{uuid}},
;;  title: {{string}},
;;  body: {{string}}
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::create-note-params
  (s/keys :req-un [::specs/interaction-id ::specs/title ::specs/body]
          :opt-un [::specs/callback ::specs/contact-id ::specs/tenant-id ::specs/resource-id]))

(def-sdk-fn create-note
  ""
  {:validation ::create-note-params
   :topic-key :create-note-response}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        tenant-id (state/get-active-tenant-id)
        body (select-keys params [:title :body :contact-id :tenant-id :resource-id])
        {:keys [status api-response] :as resp} (a/<! (rest/create-note-request interaction-id body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-create-interaction-note-err interaction-id resp)
                  :callback callback}))))

(s/def ::resource-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/resource-id]
          :opt-un [::specs/transfer-type ::specs/callback]))

(def-sdk-fn transfer-to-resource
  "Transfer an interaction to another resource (user).
  Warm transfer type will keep the current agent on the conference and put the customer on hold.
  Cold transfer type will immediately remove the current agent from the conference.  

  ```javascript
  CxEngage.interactions.transferToResource({
    interactionId: '{{uuid}}',
    resourceId: '{{uuid}}',
    transferType: '{{warm or cold}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4022](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-transfer-to-resource-err)
  "
  {:validation ::resource-transfer-params
   :topic-key :customer-transfer-acknowledged}
  [params]
  (let [{:keys [interaction-id resource-id transfer-type topic callback]} params
        transfer-type (if (= transfer-type "warm") "warm-transfer" "cold-transfer")
        interrupt-type "customer-transfer"
        interrupt-body {:transfer-resource-id resource-id
                        :resource-id (state/get-active-user-id)
                        :transfer-type transfer-type}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-transfer-to-resource-err interrupt-body interrupt-response)
                  :callback callback}))))

(s/def ::queue-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/queue-id]
          :opt-un [::specs/transfer-type ::specs/callback]))

(def-sdk-fn transfer-to-queue
  "Transfer an interaction to a queue.
  Warm transfer type will keep the current agent on the conference and put the customer on hold.
  Cold transfer type will immediately remove the current agent from the conference.  

  ```javascript
  CxEngage.interactions.transferToExtension({
    interactionId: '{{uuid}}',
    queueId: '{{uuid}}',
    transferType: '{{warm or cold}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4024](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-transfer-to-queue-err)
  "
  {:validation ::queue-transfer-params
   :topic-key :customer-transfer-acknowledged}
  [params]
  (let [{:keys [interaction-id queue-id transfer-type topic callback]} params
        transfer-type (if (= transfer-type "warm") "warm-transfer" "cold-transfer")
        interrupt-type "customer-transfer"
        interrupt-body {:transfer-queue-id queue-id
                        :resource-id (state/get-active-user-id)
                        :transfer-type transfer-type}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-transfer-to-queue-err interrupt-body interrupt-response)
                  :callback callback}))))

(s/def ::extension-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/transfer-extension]
          :opt-un [::specs/transfer-type ::specs/callback]))

(def-sdk-fn transfer-to-extension
  "Transfer an interaction to a PSTN, SIP, or WebRTC extension.
  Warm transfer type will keep the current agent on the conference and put the customer on hold.
  Cold transfer type will immediately remove the current agent from the conference.  

  ```javascript
  CxEngage.interactions.transferToExtension({
    interactionId: '{{uuid}}',
    transferExtension: {type: '{pstn or sip or webrtc}', value: '+15055555555'},
    transferType: '{{warm or cold}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4026](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-transfer-to-extension-err)
  "
  {:validation ::extension-transfer-params
   :topic-key :customer-transfer-acknowledged}
  [params]
  (let [{:keys [interaction-id transfer-extension transfer-type topic callback]} params
        transfer-type (if (= transfer-type "warm") "warm-transfer" "cold-transfer")
        interrupt-type "customer-transfer"
        interrupt-body {:transfer-extension transfer-extension
                        :resource-id (state/get-active-user-id)
                        :transfer-type transfer-type}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-transfer-to-extension-err interrupt-body interrupt-response)
                  :callback callback}))))

(s/def ::cancel-resource-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/transfer-resource-id]
          :opt-un [::specs/callback]))

(def-sdk-fn cancel-resource-transfer
  "Cancel a resource (user) transfer.
  Note: only warm transfers can be cancelled.

  ```javascript
  CxEngage.interactions.cancelResourceTransfer({
    interactionId: '{{uuid}}',
    transferResourceId: '{{uuid}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4023](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-cancel-resource-transfer-err)
  "
  {:validation ::cancel-resource-transfer-params
   :topic-key :cancel-transfer-acknowledged}
  [params]
  (let [{:keys [interaction-id transfer-resource-id topic callback]} params
        transfer-type "warm-transfer" ; only warm transfers can be cancelled
        interrupt-type "transfer-cancel"
        interrupt-body {:transfer-resource-id transfer-resource-id
                        :resource-id (state/get-active-user-id)
                        :transfer-type transfer-type}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-cancel-resource-transfer-err interrupt-body interrupt-response)
                  :callback callback}))))

(s/def ::cancel-queue-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/transfer-queue-id]
          :opt-un [::specs/callback]))

(def-sdk-fn cancel-queue-transfer
  "Cancel a queue transfer.
  Note: only warm transfers can be cancelled.

  ```javascript
  CxEngage.interactions.cancelQueueTransfer({
    interactionId: '{{uuid}}',
    transferQueueId: '{{uuid}}'
  });
  ```

  Possible Errors:

  - [Interaction: 4025](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-cancel-queue-transfer-err)
  "
  {:validation ::cancel-queue-transfer-params
   :topic-key :cancel-transfer-acknowledged}
  [params]
  (let [{:keys [interaction-id transfer-queue-id topic callback]} params
        transfer-type "warm-transfer" ; only warm transfers can be cancelled
        interrupt-type "transfer-cancel"
        interrupt-body {:transfer-queue-id transfer-queue-id
                        :resource-id (state/get-active-user-id)
                        :transfer-type transfer-type}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-cancel-queue-transfer-err interrupt-body interrupt-response)
                  :callback callback}))))

(s/def ::cancel-extension-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/transfer-extension]
          :opt-un [::specs/callback]))

(def-sdk-fn cancel-extension-transfer
  "Cancel a transfer to a PSTN, SIP, or WebRTC extension.
  Note: only warm transfers can be cancelled.

  ```javascript
  CxEngage.interactions.cancelQueueTransfer({
    interactionId: '{{uuid}}',
    transferExtension: {type: '{pstn or sip or webrtc}', value: '+15055555555'}
  });
  ```

  Possible Errors:

  - [Interaction: 4027](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-cancel-extension-transfer-err)"
  {:validation ::cancel-extension-transfer-params
   :topic-key :cancel-transfer-acknowledged}
  [params]
  (let [{:keys [interaction-id transfer-extension topic callback]} params
        transfer-type "warm-transfer" ; only warm transfers can be cancelled
        interrupt-type "transfer-cancel"
        interrupt-body {:transfer-extension transfer-extension
                        :resource-id (state/get-active-user-id)
                        :transfer-type transfer-type}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response (merge {:interaction-id interaction-id} interrupt-body)
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-cancel-extension-transfer-err interrupt-body interrupt-response)
                  :callback callback}))))

(defn- modify-elements
  "One of two helper functions for prepping the send-script payload. Modifies the keys to be the same as the front-end element's name."
  [elements]
  (let [updated-elements (reduce
                          (fn [acc element]
                            (assoc acc (get element :name) element))
                          {}
                          elements)]
    (clojure.walk/keywordize-keys updated-elements)))

(defn- add-answers-to-elements
  "Second helper function - injects the values for each element into the scriptResponse object solely for Reporting to parse them easier."
  [elements answers]
  (let [updated-elements (reduce-kv
                          (fn [acc element-name element-value]
                            (assoc acc element-name (assoc element-value :value (get-in answers [element-name :value]))))
                          {}
                          elements)]
    updated-elements))

(s/def ::script-params
  (s/keys :req-un [::specs/interaction-id ::specs/answers ::specs/script-id ::specs/dismissed ::specs/script-reporting ::specs/exit-reason]
          :opt-un [::specs/callback]))

(def-sdk-fn send-script
  "The sendSript function is used to send a script either submited by the user, auto-submit itself after a configurable amount of time or automatically when configured on the end of the interaction.

  ```javascript
  CxEngage.interactions.sendScript({
    interactionId: '{{uuid}}',
    scriptId: '{{uuid}}',
    answers: '{{object}}',
    dismissed: '{{bool}}',
    scriptReporting: '{{bool}}',
    exitReason: '{{'user-submitted'||'script-timeout'||'script-auto-dismissed'}}'
  });
  ```
  Possible Errors:

  - [Interaction: 4018](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-send-interaction-script-response-err)
  "
  {:validation ::script-params
   :topic-key :send-script}
  [params]
  (let [{:keys [topic answers script-id interaction-id dismissed script-reporting exit-reason callback]} params
        original-script (state/get-script interaction-id script-id)]
    (if-not original-script
      (p/publish {:topics topic
                  :error (e/unable-to-find-script-err interaction-id script-id)
                  :callback callback})
      (let [{:keys [sub-id script action-id]} original-script
            ;; The send script payload has a somewhat complicated structure due to
            ;; two major services needing overlapping data in different locations.

            ;; First - Flow needs the answers for the script located in the top level
            ;; "update" object, where each field from the script is it's own key and
            ;; contains the value as well as text value (if applicable).

            ;; Second - Reporting parses the scriptResponse object which is the script
            ;; broken down by front-end elements. The value for each field also needs
            ;; to be located within the scriptResponse object under it's corresponding
            ;; element.
            parsed-script (js->clj (js/JSON.parse script) :keywordize-keys true)
            elements (modify-elements (:elements parsed-script))
            updated-answers (reduce-kv
                             (fn [acc input-name input-value]
                               (assoc acc input-name {:value (or input-value nil) :text (get-in elements [input-name :text])}))
                             {}
                             answers)
            final-elements (add-answers-to-elements elements updated-answers)
            script-update {:resource-id (state/get-active-user-id)
                           :dismissed dismissed
                           :script-reporting script-reporting
                           :script-response {(keyword (:name parsed-script)) {:elements final-elements
                                                                              :id (:id parsed-script)
                                                                              :exit-reason exit-reason
                                                                              :name (:name parsed-script)}}}
            script-body {:source "client"
                         :sub-id sub-id
                         :update (merge script-update updated-answers)}
            script-response (a/<! (rest/send-flow-action-request interaction-id action-id script-body))
            {:keys [status api-response]} script-response
            {:keys [result]} api-response]
        (if (or (= status 200) (= status 400))
          (do
            (when (= status 400)
              (log :warn "Send script returned 400. Treating as success since the script no longer exists."))
            (p/publish {:topics topic
                        :response interaction-id
                        :callback callback}))
          (p/publish {:topics topic
                      :error (e/failed-to-send-interaction-script-response-err interaction-id script-response)
                      :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.sendCustomInterrupt({
;;   interactionId: "{{uuid}}",
;;   interruptType: "{{string}}",
;;   interruptBody: "{{object}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::custom-interrupt-params
  (s/keys :req-un [::specs/interaction-id
                   ::specs/interrupt-body
                   ::specs/interrupt-type]
          :opt-un [::specs/callback]))

(def-sdk-fn custom-interrupt
  ""
  {:validation ::custom-interrupt-params
   :topic-key :send-custom-interrupt-acknowledged}
  [params]
  (let [{:keys [callback topic interrupt-body interrupt-type interaction-id]} params
        {:keys [status api-response] :as resp} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-send-custom-interrupt-err interaction-id resp)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; SDK Interactions Module
;; -------------------------------------------------------------------------- ;;

(defrecord InteractionModule []
  pr/SDKModule
  (start [this]
    (let [module-name :interactions]
      (ih/register {:api {module-name {:accept accept
                                       :end end
                                       :reject end
                                       :assign-contact assign
                                       :unassign-contact unassign
                                       :enable-wrapup enable-wrapup
                                       :disable-wrapup disable-wrapup
                                       :end-wrapup end-wrapup
                                       :focus focus
                                       :unfocus unfocus
                                       :create-note create-note
                                       :update-note update-note
                                       :get-note get-note
                                       :get-all-notes get-all-notes
                                       :transfer-to-resource transfer-to-resource
                                       :transfer-to-queue transfer-to-queue
                                       :transfer-to-extension transfer-to-extension
                                       :cancel-resource-transfer cancel-resource-transfer
                                       :cancel-queue-transfer cancel-queue-transfer
                                       :cancel-extension-transfer cancel-extension-transfer
                                       :select-disposition-code select-disposition
                                       :deselect-disposition-code deselect-disposition
                                       :send-script send-script
                                       :send-custom-interrupt custom-interrupt}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
