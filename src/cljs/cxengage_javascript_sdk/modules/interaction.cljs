(ns cxengage-javascript-sdk.modules.interaction
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.helpers :refer [log]]
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
  ([module type] (e/wrong-number-of-args-error))
  ([module type client-params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (send-interrupt module type (merge (iu/extract-params client-params) {:callback (first others)}))))
  ([module type client-params]
   (let [client-params (iu/extract-params client-params)
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
                   :error (e/invalid-args-error (s/explain-data (:validation interrupt-params) client-params))
                   :callback callback})
       (iu/send-interrupt* module (assoc interrupt-params
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
  ([module action] (e/wrong-number-of-args-error))
  ([module action client-params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (note-action module action (merge (iu/extract-params client-params) {:callback (first others)}))))
  ([module action client-params]
   (let [params (iu/extract-params client-params)
         module-state @(:state module)
         api-url (get-in module [:config :api-url])
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
         note-url (-> api-url
                      (str (get-in module-state [:urls :note]))
                      (iu/build-api-url-with-params (select-keys params [:tenant-id :interaction-id :note-id])))
         notes-url (-> api-url
                       (str (get-in module-state [:urls :notes]))
                       (iu/build-api-url-with-params (select-keys params [:tenant-id :interaction-id])))
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
       (e/invalid-args-error (s/explain-data validation params))
       (do (go (let [note-response (a/<! (iu/api-request contact-note-request))
                     {:keys [status api-response]} note-response]
                 (if (not= status 200)
                   (p/publish {:topics topic
                               :response (e/api-error "api returned error")
                               :callback callback})
                   (p/publish {:topics topic
                               :response api-response
                               :callback callback}))))
           nil)))))

(s/def ::disposition-code-params
  (s/keys :req-un [::specs/interaction-id ::specs/disposition-id]
          :opt-un [::specs/callback]))

(defn select-disposition-code
  ([module]
   (e/wrong-number-of-args-error))
  ([module params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (select-disposition-code module (merge (iu/extract-params params) {:callback (first others)}))))
  ([module params]
   (let [{:keys [interaction-id disposition-id callback] :as params} (iu/extract-params params)
         dispositions (state/get-interaction-disposition-codes interaction-id)
         dv (filterv #(= (:disposition-id %1) disposition-id) dispositions)
         topic (p/get-topic :disposition-code-changed)]
     (if-not (s/valid? ::disposition-code-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error (s/explain-data ::disposition-code-params params))
                   :callback callback})
       (if (empty? dv)
         (p/publish {:topics topic
                     :error (e/incorrect-disposition-selected)
                     :callback callback})
         (let [disposition (first dv)
               interrupt-disposition (merge disposition {:selected true})
               params (merge params {:disposition interrupt-disposition})]
           (send-interrupt module :select-disposition-code params)))))))

(defn modify-elements [elements]
  (let [updated-elements (reduce
                          (fn [acc element]
                            (assoc acc (get element :name) element))
                          {}
                          elements)]
    (clojure.walk/keywordize-keys updated-elements)))

(defn add-answers-to-elements [elements answers]
  (let [updated-elements (reduce-kv
                          (fn [acc element-name element-value]
                            (assoc acc element-name (assoc element-value :value (get-in answers [element-name :value]))))
                          {}
                          elements)]
    updated-elements))

(s/def ::script-params
  (s/keys :req-un [::specs/interaction-id ::specs/answers ::specs/script-id]
          :opt-un [::specs/callback]))

(defn send-script
  ([module] (e/wrong-number-of-args-error))
  ([module client-params & others]
   (if-not (fn? (js->clj (first others)))
     (e/wrong-number-of-args-error)
     (send-script module (merge (iu/extract-params client-params) {:callback (first others)}))))
  ([module client-params]
   (let [params (iu/extract-params client-params)
         module-state @(:state module)
         {:keys [answers script-id interaction-id callback]} params
         original-script (state/get-script interaction-id script-id)
         {:keys [sub-id script action-id]} original-script
         parsed-script (js->clj (js/JSON.parse script) :keywordize-keys true)
         elements (modify-elements (:elements parsed-script))
         topic (p/get-topic :send-script)
         api-url (get-in module [:config :api-url])
         script-url (str api-url (get-in module-state [:urls :send-script]))
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
         script-request {:method :post
                         :url (iu/build-api-url-with-params
                               script-url
                               {:tenant-id (state/get-active-tenant-id)
                                :interaction-id interaction-id
                                :action-id action-id})
                         :body script-body}]
     (if-not (s/valid? ::script-params params)
       (p/publish {:topics topic
                   :error (e/invalid-args-error "Invalid Arguments")
                   :callback callback})
       (do (go (let [script-response (a/<! (iu/api-request script-request))
                     {:keys [status api-response]} script-response
                     {:keys [result]} api-response]
                 (if (not= status 200)
                   (p/publish {:topics topic
                               :error (e/api-error "api error")
                               :callback callback})
                   (p/publish {:topics topic
                               :response interaction-id
                               :callback callback}))))
           nil)))))

(def initial-state
  {:module-name :interactions
   :urls {:note "tenants/tenant-id/interactions/interaction-id/notes/note-id"
          :notes "tenants/tenant-id/interactions/interaction-id/notes?contents=true"
          :artifact-file "tenants/tenant-id/interactions/interaction-id/artifacts/artifact-id"
          :send-script "tenants/tenant-id/interactions/interaction-id/actions/action-id"}})

(defrecord InteractionModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [module-name (get @(:state this) :module-name)]
      (ih/register {:api {module-name {:accept (partial send-interrupt this :accept)
                                       :end (partial send-interrupt this :end)
                                       :reject (partial send-interrupt this :end)
                                       :assignContact (partial send-interrupt this :assign)
                                       :unassignContact (partial send-interrupt this :unassign)
                                       :enableWrapup (partial send-interrupt this :enable-wrapup)
                                       :disableWrapup (partial send-interrupt this :disable-wrapup)
                                       :endWrapup (partial send-interrupt this :end-wrapup)
                                       :focus (partial send-interrupt this :focus)
                                       :unfocus (partial send-interrupt this :unfocus)
                                       :createNote (partial note-action this :create)
                                       :updateNote (partial note-action this :update)
                                       :getNote (partial note-action this :get-one)
                                       :getAllNotes (partial note-action this :get-all)
                                       :selectDispositionCode (partial select-disposition-code this)
                                       :deselectDispositionCode (partial send-interrupt this :deselect-disposition)
                                       :sendScript (partial send-script this)}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
