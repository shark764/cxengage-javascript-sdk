(ns cxengage-javascript-sdk.modules.salesforce-lightning
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn]])
  (:require [cljs.core.async :as a]
            [clojure.string :as string]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.errors :as errors]
            [cxengage-javascript-sdk.domain.api-utils :as api]
            [cljs.spec.alpha :as s]))

;; -------------------------------------------------------------------------- ;;
;; State Functions
;; -------------------------------------------------------------------------- ;;

(def sfl-state (atom {}))

(defn add-interaction! [interaction]
  (let [{:keys [interactionId]} interaction]
    (swap! sfl-state assoc-in [:interactions interactionId] interaction)))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.salesforceLightning.setDimensions({
;;   height: "{{number}}",
;;   width: "{{number}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::set-dimensions-params
  (s/keys :req-un [::specs/height ::specs/width]
          :opt-un [::specs/callback]))

(def-sdk-fn set-dimensions
  {:validation ::set-dimensions-params
   :topic-key "cxengage/salesforce-lightning/set-dimensions-response"}
  [params]
  (let [{:keys [topic height width callback]} params]
      (try
        (js/sforce.opencti.setSoftphonePanelHeight (clj->js {:heightPX height}))
        (js/sforce.opencti.setSoftphonePanelWidth (clj->js {:widthPX width}))
        (ih/publish (clj->js {:topics topic
                              :response true
                              :callback callback}))
        (catch js/Object e
          (ih/publish (clj->js {:topics topic
                                :error e
                                :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.salesforceLightning.isVisible();
;; -------------------------------------------------------------------------- ;;

(s/def ::is-visible-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn is-visible?
  {:validation ::is-visible-params
   :topic-key "cxengage/salesforce-lightning/is-visible-response"}
  [params]
  (let [{:keys [topic callback]} params]
    (try (js/sforce.opencti.isSoftphonePanelVisible
          (clj->js {:callback (fn [response]
                               (ih/publish (clj->js {:topics topic
                                                     :response true
                                                     :callback callback})))}))
         (catch js/Object e
           (ih/publish (clj->js {:topics topic
                                 :error e
                                 :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.salesforceLightning.setVisibility({
;;   visibility: "{{boolean}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::set-visibility-params
  (s/keys :req-un [::specs/visibility]
          :opt-un [::specs/callback]))

(def-sdk-fn set-visibility
  {:validation ::set-visibility-params
   :topic-key "cxengage/salesforce-lightning/set-visibility-response"}
  [params]
  (let [{:keys [topic visibility callback]} params]
    (try (js/sforce.opencti.setSoftphonePanelVisibility
          (clj->js {:visible visibility
                    :callback (fn [response]
                                (ih/publish (clj->js {:topics topic
                                                      :response true
                                                      :callback callback})))}))
         (catch js/Object e
           (ih/publish (clj->js {:topics topic
                                 :error e
                                 :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.salesforceLightning.assignContact({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::assign-contact-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(defn send-assign-interrupt [page-info interaction-id callback topic]
  (let [parsed-result (ih/extract-params page-info)
        {:keys [objectType recordId]} (:returnValue parsed-result)
        tenant-id (ih/get-active-tenant-id)
        interrupt-type "assign-contact"
        interrupt-body (if (or (= objectType "Contact") (= objectType "Lead"))
                        {:external-crm-user (get @sfl-state :resource-id)
                         :external-crm-name "salesforce-lightning"
                         :external-crm-contact recordId
                         :external-crm-contact-uri recordId}
                        {:external-crm-user (get @sfl-state :resource-id)
                         :external-crm-name "salesforce-lightning"
                         :external-crm-related-to recordId
                         :external-crm-related-to-uri recordId})
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
     (if (= status 200)
       (ih/publish (clj->js {:topics topic
                             :response (merge {:interaction-id interaction-id} interrupt-body)
                             :callback callback}))
       (ih/publish (clj->js {:topics topic
                             :error (e/failed-to-send-salesforce-lightning-assign-err interaction-id interrupt-response)
                             :callback callback})))))

(def-sdk-fn assign-contact
  {:validation ::assign-contact-params
   :topic-key "cxengage/salesforce-lightning/contact-assignment-acknowledged"}
  [params]
  (let [{:keys [callback topic interaction-id]} params]
    (try
      (js/sforce.opencti.getAppViewInfo
        (clj->js {:callback (fn [response]
                              (send-assign-interrupt response interaction-id callback topic))}))
      (catch js/Object e
        (ih/publish (clj->js {:topics topic
                              :error e
                              :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.salesforceLightning.focusInteraction({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::focus-interaction-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn focus-interaction
  {:validation ::focus-interaction-params
   :topic-key "cxengage/salesforce-lightning/focus-interaction-response"}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        interaction (get-in @sfl-state [:interactions interaction-id])
        {:keys [tab-id contactId relatedTo recordId]} interaction]
      (try
        (js/sforce.opencti.screenPop
          (clj->js {:type "sobject" :params {:recordId (or tab-id recordId relatedTo contactId)}}))
        (catch js/Object e
          (ih/publish (clj->js {:topics topic
                                :error e
                                :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; Helper Functions
;; -------------------------------------------------------------------------- ;;

(defn update-interaction-tab-id [response interaction]
  (try
    (js/sforce.opencti.getAppViewInfo
      (clj->js {:callback (fn [result]
                            (let [result (ih/extract-params result)
                                  interaction-id (:interactionId interaction)]
                              (if (= (:success result) true)
                                (swap! sfl-state assoc-in [:interactions interaction-id :tab-id] (:recordId result))
                                (ih/publish (clj->js {:topics "cxengage/salesforce-lightning/errors/error/failed-to-update-interaction-tab-id"
                                                      :error (e/failed-to-update-salesforce-lightning-interaction-tab-id-err interaction-id)})))))}))
    (catch js/Object e
      (ih/publish (clj->js {:topics "cxengage/salesforce-lightning/errors/error/failed-to-update-interaction-tab-id"
                            :error e})))))

(defn auto-assign-from-search-pop [response interaction-id]
  (let [result (:result (ih/extract-params response))
        parsed-result (ih/extract-params (js/JSON.parse result))]
      (if (= 1 (count (vals parsed-result)))
        (assign-contact (clj->js {:interaction interaction-id}))
        (log :info "More than one result - skipping auto-assign"))))

(defn dump-state []
  (js/console.log (clj->js @sfl-state)))

;; -------------------------------------------------------------------------- ;;
;; Subscription Handlers
;; -------------------------------------------------------------------------- ;;

(defn handle-state-change [error topic response]
  (if error
    (ih/publish {:topics topic
                 :error error})
    (let [state (:state (ih/extract-params response))]
      (when (= state "notready")
        (try
          (js/sforce.opencti.disableClickToDial)
          (catch js/Object e
            (ih/publish (clj->js {:topics topic
                                  :error e})))))
      (when (= state "ready")
        (try
          (js/sforce.opencti.enableClickToDial)
          (catch js/Object e
            (ih/publish (clj->js {:topics topic
                                  :error e}))))))))

(defn handle-click-to-dial [dial-details]
  (ih/publish {:topics "cxengage/salesforce-lightning/on-click-to-interaction"
               :response dial-details}))

(defn handle-work-offer [error topic interaction-details]
  (let [interaction (ih/extract-params interaction-details)
        {:keys [contact related-to pop-on-accept interactionId]} interaction
        pop-callback (fn [response]
                       (update-interaction-tab-id response interaction))]
      (add-interaction! interaction)
      (cond
        pop-on-accept (log :debug "Pop On Accept - waiting for work-accepted-received")
        related-to (try
                     (js/sforce.opencti.screenPop (clj->js {:type "sobject" :params {:recordId related-to}, :callback pop-callback}))
                     (catch js/Object e
                       (ih/publish (clj->js {:topics topic
                                             :error e}))))
        contact (try
                  (js/sforce.opencti.screenPop (clj->js {:type "sobject" :params {:recordId contact}, :callback pop-callback}))
                  (catch js/Object e
                    (ih/publish (clj->js {:topics topic
                                          :error e}))))
        :else (log :debug "No screen pop details on work offer"))))

(defn handle-work-accepted [error topic interaction-details]
  (let [result (ih/extract-params interaction-details)
        interaction (get-in @sfl-state [:interactions (:interactionId result)])
        {:keys [related-to contact pop-on-accept]} interaction
        pop-callback (fn [response]
                       (update-interaction-tab-id response interaction))]
    (when pop-on-accept
      (cond
        related-to (try
                     (js/sforce.opencti.screenPop (clj->js {:type "sobject" :params {:recordId related-to}, :callback pop-callback}))
                     (catch js/Object e
                       (ih/publish (clj->js {:topics topic
                                             :error e}))))
        contact (try
                  (js/sforce.opencti.screenPop (clj->js {:type "sobject" :params {:recordId contact}, :callback pop-callback}))
                  (catch js/Object e
                    (ih/publish (clj->js {:topics topic
                                          :error e}))))
        :else (log :info "No screen pop details on work accepted")))))

(defn handle-work-ended [error topic interaction-details]
  (let [interaction-id (:interactionId (ih/extract-params interaction-details))
        tab-id (get-in @sfl-state [:interactions interaction-id :tab-id])]
    (try
      (js/sforce.console.closeTab tab-id)
      (catch js/Object e
        (ih/publish (clj->js {:topics topic
                              :error e}))))))

(defn handle-screen-pop [error topic interaction-details]
  (let [result (ih/extract-params interaction-details)
        {:keys [popType popUrl newWindow size searchType filter filterType terms interactionId]} result
        pop-callback (fn [response]
                        (auto-assign-from-search-pop response interactionId))]
    (cond
      (= popType "internal") (try
                               (js/sforce.opencti.screenPop (clj->js {:type "sobject" :params {:recordId popUrl}, :callback pop-callback}))
                               (catch js/Object e
                                 (ih/publish (clj->js {:topics topic
                                                       :error e}))))
      (= popType "external") (if (= newWindow "true")
                              (js/window.open popUrl "targetWindow" (str "width=" (:width size) ",height=" (:height size)))
                              (js/window.open popUrl))
      (= popType "search") (do
                              (when (= searchType "fuzzy")
                                (try
                                  (js/sforce.opencti.searchAndScreenPop (clj->js {:searchParams (string/join " or " terms)
                                                                                  :queryParams ""
                                                                                  :callType "inbound"
                                                                                  :callback pop-callback}))
                                  (catch js/Object e
                                    (ih/publish (clj->js {:topics topic
                                                          :error e})))))
                              (when (= searchType "strict")
                                (try
                                  (js/sforce.opencti.searchAndScreenPop (clj->js {:searchParams (string/join (str " " filterType " ") (vals filter))
                                                                                  :queryParams ""
                                                                                  :callType "inbound"
                                                                                  :callback pop-callback}))
                                  (catch js/Object e
                                    (ih/publish (clj->js {:topics topic
                                                          :error e})))))))))

;; -------------------------------------------------------------------------- ;;
;; Salesforce Initialization Functions
;; -------------------------------------------------------------------------- ;;

(defn sfl-ready? []
  (and (aget js/window "sforce")
       (aget js/window "sforce" "opencti")))

(defn ^:private sfl-init
  [interaction]
  (doseq [url [interaction]]
    (let [script (js/document.createElement "script")
          body (.-body js/document)]
      (.setAttribute script "type" "text/javascript")
      (.setAttribute script "src" url)
      (.appendChild body script)))
  (go-loop []
    (if (sfl-ready?)
      (do
        (swap! sfl-state assoc :resource-id (ih/get-active-user-id))
        (try
          (js/sforce.opencti.onClickToDial (clj->js {:listener handle-click-to-dial}))
          (ih/publish (clj->js {:topics "cxengage/salesforce-lightning/initialize-complete"
                                :response true}))
          (catch js/Object e
            (ih/publish (clj->js {:topics "cxengage/salesforce-lightning/error/failed-to-create-click-to-dial-handler"
                                  :error e})))))
      (do (a/<! (a/timeout 250))
          (recur)))))

;; -------------------------------------------------------------------------- ;;
;; Salesforce Lightning Module
;; -------------------------------------------------------------------------- ;;

(defrecord SFLModule []
  pr/SDKModule
  (start [this]
    (go-loop []
      (if (ih/core-ready?)
        (let [module-name :salesforce-lightning
              sfl-interaction "https://login.salesforce.com/support/api/41.0/lightning/opencti_min.js"]
          (sfl-init sfl-interaction)
          (ih/register (clj->js {:api {:salesforce-lightning {:set-dimensions set-dimensions
                                                              :is-visible is-visible?
                                                              :set-visibility set-visibility
                                                              :focus-interaction focus-interaction
                                                              :assign-contact assign-contact
                                                              :dump-state dump-state}}
                                 :module-name module-name}))
          (ih/subscribe (topics/get-topic :presence-state-change-request-acknowledged) handle-state-change)
          (ih/subscribe (topics/get-topic :work-offer-received) handle-work-offer)
          (ih/subscribe (topics/get-topic :work-accepted-received) handle-work-accepted)
          (ih/subscribe (topics/get-topic :work-ended-received) handle-work-ended)
          (ih/subscribe (topics/get-topic :generic-screen-pop-received) handle-screen-pop)
          (ih/send-core-message {:type :module-registration-status
                                 :status :success
                                 :module-name module-name}))
        (do (a/<! (a/timeout 250))
            (recur)))))
  (stop [this])
  (refresh-integration [this]))
