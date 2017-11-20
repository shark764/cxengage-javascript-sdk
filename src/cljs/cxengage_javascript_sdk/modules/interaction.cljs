(ns cxengage-javascript-sdk.modules.interaction
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn]])
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

(defn build-detailed-interrupt-body
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

;; -------------------------------------------------------------------------- ;;
;; //End interaction
;; CxEngage.interactions.end({
;;   interactionId: "{{uuid}}"
;; });
;;
;; //Reject interaction
;; CxEngage.interactions.reject({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn end
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.accept({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn accept
  {:validation ::generic-interaction-fn-params
   :topic-key :interaction-accept-acknowledged}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        tenant-id (state/get-active-tenant-id)
        interrupt-body {:resource-id (state/get-active-user-id)}
        interrupt-type "offer-accept"
        {:keys [timeout timeout-end channel-type]} (state/get-interaction interaction-id)
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
            (do (when (and (= channel-type "voice")
                           (= (:provider (state/get-active-extension)) "twilio"))
                  (let [connection (state/get-twilio-connection)]
                    (go-loop [t (a/timeout 3000)
                              attempts 1]
                      (if (= attempts 3)
                        (p/publish {:topics topic
                                    :error (e/failed-to-find-twilio-connection-object interaction-id)
                                    :callback callback})
                        (if (and connection
                                 (.-accept connection))
                          (.accept connection)
                          (do (a/<! t)
                              (recur (a/timeout 3000)
                                     (inc attempts))))))))
                (when (or (= channel-type "sms")
                          (= channel-type "messaging"))
                  (int/get-messaging-history interaction-id))))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.focus({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn focus
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.unfocus({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn unfocus
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.assignContact({
;;   interactionId: "{{uuid}}",
;;   contactId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn assign
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.unassignContact({
;;   interactionId: "{{uuid}}",
;;   contactId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn unassign
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.enableWrapup({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn enable-wrapup
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.disableWrapup({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn disable-wrapup
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.endWrapup({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn end-wrapup
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.deselectdispositioncode({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn deselect-disposition
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.selectDispositionCode({
;;   interactionId: "{{uuid}}",
;;   dispositionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn select-disposition
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.transferToResource({
;;   interactionId: "{{uuid}}",
;;   resourceId: "{{uuid}}",
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;
(s/def ::resource-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/resource-id]
          :opt-un [::specs/transfer-type ::specs/callback]))

(def-sdk-fn transfer-to-resource
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

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.transferToQueue({
;;   interactionId: "{{uuid}}",
;;   queueId: "{{uuid}}",
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;
(s/def ::queue-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/queue-id]
          :opt-un [::specs/transfer-type ::specs/callback]))

(def-sdk-fn transfer-to-queue
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


;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.transferToExtension({
;;   interactionId: "{{uuid}}",
;;   transferExtension: {type: "pstn", value: "+15055555555"},
;;   transferType: "{{warm or cold}}"
;; });
;; -------------------------------------------------------------------------- ;;
(s/def ::extension-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/transfer-extension]
          :opt-un [::specs/transfer-type ::specs/callback]))

(def-sdk-fn transfer-to-extension
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


;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.cancelResourceTransfer({
;;   interactionId: "{{uuid}}",
;;   transferResourceId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;
(s/def ::cancel-resource-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/transfer-resource-id]
          :opt-un [::specs/callback]))

(def-sdk-fn cancel-resource-transfer
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


;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.cancelQueueTransfer({
;;   interactionId: "{{uuid}}",
;;   transferQueueId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;
(s/def ::cancel-queue-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/transfer-queue-id]
          :opt-un [::specs/callback]))

(def-sdk-fn cancel-queue-transfer
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


;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.cancelExtensionTransfer({
;;   interactionId: "{{uuid}}",
;;   transferExtension: {type: "pstn", value: "+15055555555"}
;; });
;; -------------------------------------------------------------------------- ;;
(s/def ::cancel-extension-transfer-params
  (s/keys :req-un [::specs/interaction-id ::specs/transfer-extension]
          :opt-un [::specs/callback]))

(def-sdk-fn cancel-extension-transfer
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


;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.sendScript({
;;   interactionId: "{{uuid}}",
;;   scriptId: "{{uuid}}",
;;   answers: "{{object}}"
;; });
;; -------------------------------------------------------------------------- ;;

(defn modify-elements
  "One of two helper functions for prepping the send-script payload. Modifies the keys to be the same as the front-end element's name."
  [elements]
  (let [updated-elements (reduce
                          (fn [acc element]
                            (assoc acc (get element :name) element))
                          {}
                          elements)]
    (clojure.walk/keywordize-keys updated-elements)))

(defn add-answers-to-elements
  "Second helper function - injects the values for each element into the scriptResponse object solely for Reporting to parse them easier."
  [elements answers]
  (let [updated-elements (reduce-kv
                          (fn [acc element-name element-value]
                            (assoc acc element-name (assoc element-value :value (get-in answers [element-name :value]))))
                          {}
                          elements)]
    updated-elements))

(s/def ::script-params
  (s/keys :req-un [::specs/interaction-id ::specs/answers ::specs/script-id]
          :opt-un [::specs/callback]))

(def-sdk-fn send-script
  {:validation ::script-params
   :topic-key :send-script}
  [params]
  (let [{:keys [topic answers script-id interaction-id callback]} params
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
                           :script-response {(keyword (:name parsed-script)) {:elements final-elements
                                                                              :id (:id parsed-script)
                                                                              :name (:name parsed-script)}}}
            script-body {:source "client"
                         :sub-id sub-id
                         :update (merge script-update updated-answers)}
            script-response (a/<! (rest/send-flow-action-request interaction-id action-id script-body))
            {:keys [status api-response]} script-response
            {:keys [result]} api-response]
        (if (= status 200)
          (p/publish {:topics topic
                      :response interaction-id
                      :callback callback})
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
