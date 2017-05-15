(ns cxengage-javascript-sdk.modules.interaction
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cxengage-javascript-sdk.macros :refer [def-sdk-fn]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-cljs-utils.core :as cxu]
            [cxengage-javascript-sdk.domain.errors :as err]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.interaction-management :as int]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.interop-helpers :as ih]))

(s/def ::generic-interaction-fn-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(s/def ::contact-operation-params
  (s/keys :req-un [::specs/interaction-id ::specs/contact-id]
          :opt-un [::specs/callback]))

(defn send-interrupt
  ([type] (e/wrong-number-of-sdk-fn-args-err))
  ([type client-params & others]
   (if-not (fn? (js->clj (first others)))
     (e/callback-isnt-a-function-err)
     (send-interrupt type (merge (ih/extract-params client-params) {:callback (first others)}))))
  ([type client-params]
   (let [client-params (ih/extract-params client-params)
         {:keys [callback interaction-id contact-id disposition]} client-params
         {:keys [sub-id action-id channel-type resource-id tenant-id resource direction channel-type timeout timeout-end]} (state/get-interaction interaction-id)
         {:keys [extension role-id session-id work-offer-id]} resource
         basic-interrupt-body {:resource-id (state/get-active-user-id)}
         detailed-interaction-interrupt-body {:tenant-id tenant-id
                                              :interaction-id interaction-id
                                              :sub-id sub-id
                                              :action-id action-id
                                              :work-offer-id work-offer-id
                                              :session-id session-id
                                              :resource-id resource-id
                                              :direction direction
                                              :channel-type channel-type}
         interrupt-params (case type
                            :end {:validation ::generic-interaction-fn-params
                                  :interrupt-type "resource-disconnect"
                                  :topic (p/get-topic :interaction-end-acknowledged)
                                  :interrupt-body basic-interrupt-body}
                            :accept {:validation ::generic-interaction-fn-params
                                     :interrupt-type "offer-accept"
                                     :topic (p/get-topic :interaction-accept-acknowledged)
                                     :interrupt-body basic-interrupt-body
                                     :on-confirm-fn (fn []
                                                      (when-not (<= (js/Date.parse (or timeout timeout-end)) (iu/get-now))
                                                        (when (= channel-type "voice")
                                                          (let [connection (state/get-twilio-connection)]
                                                            (.accept connection)))
                                                        (when (or (= channel-type "sms")
                                                                  (= channel-type "messaging"))
                                                          (int/get-messaging-history tenant-id interaction-id))))}
                            :focus {:validation ::generic-interaction-fn-params
                                    :interrupt-type "interaction-focused"
                                    :topic (p/get-topic :interaction-focus-acknowledged)
                                    :interrupt-body detailed-interaction-interrupt-body}
                            :unfocus {:validation ::generic-interaction-fn-params
                                      :interrupt-type "interaction-unfocused"
                                      :topic (p/get-topic :interaction-unfocus-acknowledged)
                                      :interrupt-body detailed-interaction-interrupt-body}
                            :assign {:validation ::contact-operation-params
                                     :interrupt-type "interaction-contact-selected"
                                     :topic (p/get-topic :contact-assignment-acknowledged)
                                     :interrupt-body (assoc detailed-interaction-interrupt-body :contact-id contact-id)}
                            :unassign {:validation ::contact-operation-params
                                       :interrupt-type "interaction-contact-deselected"
                                       :topic (p/get-topic :contact-unassignment-acknowledged)
                                       :interrupt-body (assoc detailed-interaction-interrupt-body :contact-id contact-id)}
                            :enable-wrapup {:validation ::generic-interaction-fn-params
                                            :interrupt-type "wrapup-on"
                                            :topic (p/get-topic :enable-wrapup-acknowledged)
                                            :interrupt-body basic-interrupt-body}
                            :deselect-disposition {:validation ::generic-interaction-fn-params
                                                   :interrupt-type "disposition-select"
                                                   :topic (p/get-topic :disposition-code-changed)
                                                   :interrupt-body basic-interrupt-body}
                            :disable-wrapup {:validation ::generic-interaction-fn-params
                                             :interrupt-type "wrapup-off"
                                             :topic (p/get-topic :disable-wrapup-acknowledged)
                                             :interrupt-body basic-interrupt-body}
                            :end-wrapup {:validation ::generic-interaction-fn-params
                                         :interrupt-type "wrapup-end"
                                         :topic (p/get-topic :end-wrapup-acknowledged)
                                         :interrupt-body basic-interrupt-body}
                            :select-disposition-code {:validation ::generic-interaction-fn-params
                                                      :interrupt-type "disposition-select"
                                                      :topic (p/get-topic :disposition-code-changed)
                                                      :interrupt-body {:disposition disposition
                                                                       :resource-id resource-id}})]
     (if-not (s/valid? (:validation interrupt-params) client-params)
       (p/publish {:topics (:topic interrupt-params)
                   :error (e/args-failed-spec-err)
                   :callback callback})
       (iu/send-interrupt* (assoc interrupt-params
                                  :interaction-id interaction-id
                                  :callback callback))))))

(s/def ::get-one-note-params
  (s/keys :req-un [::specs/interaction-id ::specs/note-id]
          :opt-un [::specs/callback]))

(s/def ::get-all-notes-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(s/def ::update-note-params
  (s/keys :req-un [::specs/interaction-id ::specs/note-id]
          :opt-un [::specs/callback ::specs/title ::specs/body ::specs/contact-id]))

(s/def ::create-note-params
  (s/keys :req-un [::specs/interaction-id ::specs/title ::specs/body]
          :opt-un [::specs/callback ::specs/contact-id ::specs/tenant-id ::specs/resource-id]))

(defn note-action
  ([module action] (e/wrong-number-of-sdk-fn-args-err))
  ([module action client-params & others]
   (if-not (fn? (js->clj (first others)))
     (e/callback-isnt-a-function-err)
     (note-action module action (merge (ih/extract-params client-params) {:callback (first others)}))))
  ([module action client-params]
   (let [params (ih/extract-params client-params)
         module-state @(:state module)
         {:keys [callback interaction-id note-id]} params
         params (assoc params :resource-id (state/get-active-user-id))
         params (assoc params :tenant-id (state/get-active-tenant-id))
         validation (case action
                      :get-one ::get-one-note-params
                      :get-all ::get-all-notes-params
                      :update ::update-note-params
                      :create ::create-note-params)
         body (case action
                :get-one nil
                :get-all nil
                :update (select-keys params [:title :body :contact-id])
                :create (select-keys params [:title :body :contact-id]))
         note-url (iu/api-url
                   "tenants/tenant-id/interactions/interaction-id/notes/note-id"
                   (select-keys params [:tenant-id :interaction-id :note-id]))
         notes-url (iu/api-url
                    "tenants/tenant-id/interactions/interaction-id/notes?contents=true"
                    (select-keys params [:tenant-id :interaction-id]))
         url (case action
               :get-one note-url
               :get-all notes-url
               :update note-url
               :create notes-url)
         method (case action
                  :get-one :get
                  :get-all :get
                  :update :put
                  :create :post)
         topic (case action
                 :get-one "cxengage/interactions/get-note-response"
                 :get-all "cxengage/interactions/get-notes-response"
                 :update "cxengage/interactions/update-note-response"
                 :create "cxengage/interactions/create-note-response")
         contact-note-request {:method method
                               :url url}
         contact-note-request (if body
                                (assoc contact-note-request :body body)
                                contact-note-request)]
     (if-not (s/valid? validation params)
       (p/publish {:topics topic
                   :error (e/args-failed-spec-err)
                   :callback callback})
       (do (go (let [note-response (a/<! (iu/api-request contact-note-request))
                     {:keys [status api-response]} note-response]
                 (when (= status 200)
                   (p/publish {:topics topic
                               :response api-response
                               :callback callback}))))
           nil)))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.interactions.selectDispositionCode({
;;   interactionId: "{{uuid}}",
;;   dispositionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::disposition-code-params
  (s/keys :req-un [::specs/interaction-id ::specs/disposition-id]
          :opt-un [::specs/callback]))

(def-sdk-fn select-disposition-code
  ::disposition-code-params
  (p/get-topic :disposition-code-changed)
  [params]
  (let [{:keys [topic interaction-id disposition-id callback]} params
        dispositions (state/get-interaction-disposition-codes interaction-id)
        dv (filterv #(= (:disposition-id %1) disposition-id) dispositions)]
    (if (empty? dv)
      (p/publish {:topics topic
                  :error (e/args-failed-spec-err)
                  :callback callback})
      (let [disposition (first dv)
            interrupt-disposition (merge disposition {:selected true})
            params (merge params {:disposition interrupt-disposition})]
        (send-interrupt :select-disposition-code params)))))

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
  ::script-params
  (p/get-topic :send-script)
  [params]
  (let [{:keys [topic answers script-id interaction-id callback]} params
        original-script (state/get-script interaction-id script-id)
        {:keys [sub-id script action-id]} original-script
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
                     :sub-id (:sub-id original-script)
                     :update (merge script-update updated-answers)}
        ;; The send script payload has a somewhat complicated structure due to
        ;; two major services needing overlapping data in different locations.

        ;; First - Flow needs the answers for the script located in the top level
        ;; "update" object, where each field from the script is it's own key and
        ;; contains the value as well as text value (if applicable).

        ;; Second - Reporting parses the scriptResponse object which is the script
        ;; broken down by front-end elements. The value for each field also needs
        ;; to be located within the scriptResponse object under it's corresponding
        ;; element.
        script-request {:method :post
                        :url (iu/api-url
                              "tenants/tenant-id/interactions/interaction-id/actions/action-id"
                              {:tenant-id (state/get-active-tenant-id)
                               :interaction-id interaction-id
                               :action-id action-id})
                        :body script-body}]
    (do (go (let [script-response (a/<! (iu/api-request script-request))
                  {:keys [status api-response]} script-response
                  {:keys [result]} api-response]
              (when (= status 200)
                (p/publish {:topics topic
                            :response interaction-id
                            :callback callback})))))))

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
  ::custom-interrupt-params
  (p/get-topic :send-custom-interrupt-acknowledged)
  [params]
  (let [{:keys [callback topic interrupt-body interrupt-type interaction-id]} params
        tenant-id (state/get-active-tenant-id)
        interrupt-request {:method :post
                           :body {:source "Client"
                                  :interrupt-type interrupt-type
                                  :interrupt interrupt-body}
                           :url (str (iu/api-url
                                      "tenants/tenant-id/interactions/interaction-id/interrupts"
                                      {:tenant-id tenant-id
                                       :interaction-id interaction-id}))}
        {:keys [status api-response]} (a/<! (iu/api-request interrupt-request))]
    (when (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; SDK Interactions Module
;; -------------------------------------------------------------------------- ;;

(defrecord InteractionModule []
  pr/SDKModule
  (start [this]
    (let [module-name :interactions]
      (ih/register {:api {module-name {:accept (partial send-interrupt :accept)
                                       :end (partial send-interrupt :end)
                                       :reject (partial send-interrupt :end)
                                       :assignContact (partial send-interrupt :assign)
                                       :unassignContact (partial send-interrupt :unassign)
                                       :enableWrapup (partial send-interrupt :enable-wrapup)
                                       :disableWrapup (partial send-interrupt :disable-wrapup)
                                       :endWrapup (partial send-interrupt :end-wrapup)
                                       :focus (partial send-interrupt :focus)
                                       :unfocus (partial send-interrupt :unfocus)
                                       :createNote (partial note-action this :create)
                                       :updateNote (partial note-action this :update)
                                       :getNote (partial note-action this :get-one)
                                       :getAllNotes (partial note-action this :get-all)
                                       :selectDispositionCode select-disposition-code
                                       :deselectDispositionCode (partial send-interrupt :deselect-disposition)
                                       :sendScript send-script
                                       :sendCustomInterrupt custom-interrupt}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
