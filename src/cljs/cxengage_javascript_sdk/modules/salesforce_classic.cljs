(ns cxengage-javascript-sdk.modules.salesforce-classic
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
            [cxengage-javascript-sdk.domain.api-utils :as api]
            [cxengage-javascript-sdk.domain.errors :as errors]
            [cljs.spec.alpha :as s]))

;; -------------------------------------------------------------------------- ;;
;; State Functions
;; -------------------------------------------------------------------------- ;;

(def sfc-state (atom {}))

(defn add-interaction! [interaction]
  (let [{:keys [interactionId]} interaction]
    (swap! sfc-state assoc-in [:interactions interactionId] interaction)))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.salesforceClassic.setDimensions({
;;   height: "{{number}}",
;;   width: "{{number}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::set-dimensions-params
  (s/keys :req-un [::specs/height ::specs/width]
          :opt-un [::specs/callback]))

(def-sdk-fn set-dimensions
  {:validation ::set-dimensions-params
   :topic-key "cxengage/sfc/set-dimensions-response"}
  [params]
  (let [{:keys [topic height width callback]} params]
      (try
        (js/sforce.interaction.cti.setSoftphoneHeight height)
        (catch js/Object e
          (ih/publish (clj->js {:topics topic
                                :error e
                                :callback callback}))))
      (try
        (js/sforce.interaction.cti.setSoftphoneWidth width)
        (catch js/Object e
          (ih/publish (clj->js {:topics topic
                                :error e
                                :callback callback}))))
      (ih/publish (clj->js {:topics topic
                            :response "true"
                            :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.salesforceClassic.isVisible();
;; -------------------------------------------------------------------------- ;;

(s/def ::is-visible-params
  (s/keys :req-un []
          :opt-un [::specs/callback]))

(def-sdk-fn is-visible?
  {:validation ::is-visible-params
   :topic-key "cxengage/sfc/is-visible-response"}
  [params]
  (let [{:keys [topic callback]} params]
    (try (js/sforce.interaction.isVisible
          (fn [response]
            (ih/publish (clj->js {:topics topic
                                  :response (:result response)
                                  :callback callback}))))
         (catch js/Object e
           (ih/publish (clj->js {:topics topic
                                 :error e
                                 :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.salesforceClassic.setVisibility({
;;   visibility: "{{boolean}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::set-visibility-params
  (s/keys :req-un [::specs/visibility]
          :opt-un [::specs/callback]))

(def-sdk-fn set-visibility
  {:validation ::set-visibility-params
   :topic-key "cxengage/sfc/set-visibility-response"}
  [params]
  (let [{:keys [topic visibility callback]} params]
    (try (js/sforce.interaction.setVisible visibility
          (fn [response]
            (ih/publish (clj->js {:topics topic
                                  :response (:result response)
                                  :callback callback}))))
         (catch js/Object e
           (ih/publish (clj->js {:topics topic
                                 :error e
                                 :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.salesforceClassic.assignContact({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::assign-contact-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(defn send-assign-interrupt [page-info interaction-id callback topic]
  (let [parsed-result (ih/extract-params page-info)
        {:keys [object objectId]} parsed-result
        interrupt-type "assign-contact"
        interrupt-body (if (or (= object "Contact") (= object "Lead"))
                        {:external-crm-user (get @sfc-state :resource-id)
                         :external-crm-name "salesforce-classic"
                         :external-crm-contact objectId
                         :external-crm-contact-uri objectId}
                        {:external-crm-user (get @sfc-state :resource-id)
                         :external-crm-name "salesforce-classic"
                         :external-crm-related-to objectId
                         :external-crm-related-to-uri objectId})
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (ih/publish (clj->js {:topics topic
                            :response (merge {:interaction-id interaction-id} interrupt-body)
                            :callback callback}))
      (ih/publish (clj->js {:topics topic
                            :error (e/failed-to-send-salesforce-classic-assign-err interaction-id interrupt-response)
                            :callback callback})))))

(def-sdk-fn assign-contact
  {:validation ::assign-contact-params
   :topic-key "cxengage/sfc/contact-assignment-acknowledged"}
  [params]
  (let [{:keys [callback topic interaction-id]} params]
    (try (js/sforce.interaction.getPageInfo
          (fn [response]
           (send-assign-interrupt response interaction-id callback topic)))
         (catch js/Object e
           (ih/publish (clj->js {:topics topic
                                 :error e
                                 :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.salesforceClassic.focusInteraction({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::focus-interaction-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn focus-interaction
  {:validation ::focus-interaction-params
   :topic-key "cxengage/sfc/focus-interaction"}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        interaction (get-in @sfc-state [:interactions interaction-id])
        {:keys [tab-id]} interaction]
      (when tab-id
        (try
          (js/sforce.console.focusPrimaryTabById tab-id)
          (catch js/Object e
            (ih/publish (clj->js {:topics topic
                                  :error e
                                  :callback callback})))))))

;; -------------------------------------------------------------------------- ;;
;; Helper Functions
;; -------------------------------------------------------------------------- ;;

(defn update-interaction-tab-id [response interaction]
  (try (js/sforce.console.getFocusedPrimaryTabId
        (fn [result]
          (let [result (js->clj result :keywordize-keys true)
                interaction-id (:interactionId interaction)]
            (if (= (:success result) true)
              (swap! sfc-state assoc-in [:interactions interaction-id :tab-id] (:id result))
              (ih/publish (clj->js {:topics "cxengage/salesforce-classic/errors/error/failed-to-update-interaction-tab-id"
                                    :error (e/failed-to-update-salesforce-classic-interaction-tab-id-err interaction-id)}))))))
       (catch js/Object e
         (ih/publish (clj->js {:topics "cxengage/salesforce-classic/errors/error/failed-to-update-interaction-tab-id"
                               :error e})))))

(defn auto-assign-from-search-pop [response interaction-id]
  (let [result (:result (ih/extract-params response))
        parsed-result (ih/extract-params result)]
      (if (= 1 (count (vals parsed-result)))
        (assign-contact (clj->js {:interaction interaction-id}))
        (log :info "More than one result - skipping auto-assign"))))

(defn dump-state []
  (js/console.log @sfc-state))

;; -------------------------------------------------------------------------- ;;
;; Subscription Handlers
;; -------------------------------------------------------------------------- ;;

(defn handle-state-change [error topic response]
  (if error
    (ih/publish {:topics topic
                 :error error})
    (let [state (:state (js->clj response :keywordize-keys true))]
      (when (= state "notready")
        (try
          (js/sforce.interaction.cti.disableClickToDial)
          (catch js/Object e
            (ih/publish (clj->js {:topics topic
                                  :error e})))))
      (when (= state "ready")
        (try
          (js/sforce.interaction.cti.enableClickToDial)
          (catch js/Object e
            (ih/publish (clj->js {:topics topic
                                  :error e}))))))))

(defn handle-click-to-dial [dial-details]
  (let [result (:result (js->clj dial-details :keywordize-keys true))
        parsed-result (js/JSON.parse result)]
    (ih/publish {:topics "cxengage/salesforce-classic/on-click-to-interaction"
                 :response parsed-result})))

(defn handle-work-offer [error topic interaction-details]
  (let [interaction (js->clj interaction-details :keywordize-keys true)
        {:keys [contact related-to pop-on-accept interactionId]} interaction
        pop-callback (fn [response]
                       (update-interaction-tab-id response interaction))]
      (add-interaction! interaction)
      (cond
        pop-on-accept (log :info "Pop On Accept - waiting for work-accepted-received")
        related-to (try
                    (js/sforce.interaction.screenPop related-to true pop-callback)
                    (catch js/Object e
                      (ih/publish (clj->js {:topics topic
                                            :error e}))))
        contact (try
                 (js/sforce.interaction.screenPop contact true pop-callback)
                 (catch js/Object e
                   (ih/publish (clj->js {:topics topic
                                         :error e})))))))
        :else (log :info "No screen pop details on work offer")

(defn handle-work-accepted [error topic interaction-details]
  (let [result (js->clj interaction-details :keywordize-keys true)
        interaction (get-in @sfc-state [:interactions (:interactionId result)])
        {:keys [related-to contact pop-on-accept]} interaction
        pop-callback (fn [response]
                       (update-interaction-tab-id response interaction))]
    (when pop-on-accept
      (cond
        related-to (try
                    (js/sforce.interaction.screenPop related-to true pop-callback)
                    (catch js/Object e
                      (ih/publish (clj->js {:topics topic
                                            :error e}))))
        contact (try
                  (js/sforce.interaction.screenPop contact true pop-callback)
                  (catch js/Object e
                    (ih/publish (clj->js {:topics topic
                                          :error e}))))
        :else (log :info "No screen pop details on work accepted")))))

(defn handle-work-ended [error topic interaction-details]
  (let [interaction-id (:interactionId (js->clj interaction-details :keywordize-keys true))
        tab-id (get-in @sfc-state [:interactions interaction-id :tab-id])]
    (try
      (js/sforce.console.closeTab tab-id)
      (catch js/Object e
        (ih/publish (clj->js {:topics topic
                              :error e}))))))

(defn handle-screen-pop [error topic interaction-details]
  (let [result (js->clj interaction-details :keywordize-keys true)
        {:keys [popType popUrl newWindow size searchType filter filterType terms interactionId]} result
        pop-callback (fn [response]
                        (auto-assign-from-search-pop response interactionId))]
    (cond
      (= popType "url") (try
                              (js/sforce.interaction.screenPop popUrl true)
                              (catch js/Object e
                                (ih/publish (clj->js {:topics topic
                                                      :error e}))))
      (= popType "external-url") (if (= newWindow "true")
                              (js/window.open popUrl "targetWindow" ()(:width size) (:height size))
                              (js/window.open popUrl (:width size) (:height size)))
      (= popType "search-pop") (do
                              (when (= searchType "fuzzy")
                                (try
                                  (js/sforce.interaction.searchAndScreenPop
                                    (string/join " or " terms)
                                    ""
                                    "inbound"
                                    pop-callback)
                                  (catch js/Object e
                                    (ih/publish (clj->js {:topics topic
                                                          :error e})))))
                              (when (= searchType "strict")
                                (try (js/sforce.interaction.searchAndScreenPop
                                       (string/join (str " " filterType " ") (vals filter))
                                       ""
                                       "inbound"
                                       pop-callback)
                                     (catch js/Object e
                                      (ih/publish (clj->js {:topics topic
                                                            :error e})))))))))

;; -------------------------------------------------------------------------- ;;
;; Salesforce Initialization Functions
;; -------------------------------------------------------------------------- ;;

(defn sfc-ready? []
  (and (aget js/window "sforce")
       (aget js/window "sforce" "console")
       (aget js/window "sforce" "interaction")))

(defn ^:private sfc-init
  [integration interaction]
  (doseq [url [integration interaction]]
    (let [script (js/document.createElement "script")
          body (.-body js/document)]
      (.setAttribute script "type" "text/javascript")
      (.setAttribute script "src" url)
      (.appendChild body script)))
  (go-loop []
    (if (sfc-ready?)
      (do
        (swap! sfc-state assoc :resource-id (ih/get-active-user-id))
        (try
          (js/sforce.interaction.cti.onClickToDial handle-click-to-dial)
          (ih/publish (clj->js {:topics "cxengage/salesforce-classic/initialize-complete"
                                :response true}))
          (catch js/Object e
            (ih/publish (clj->js {:topics "cxengage/salesforce-classic/error/failed-to-create-click-to-dial-handler"
                                  :error e})))))
      (do (a/<! (a/timeout 250))
          (recur)))))

;; -------------------------------------------------------------------------- ;;
;; Salesforce Classic Module
;; -------------------------------------------------------------------------- ;;

(defrecord SFCModule []
  pr/SDKModule
  (start [this]
    (go-loop []
      (if (ih/core-ready?)
        (let [module-name :salesforce-classic
              sfc-integration "https://login.salesforce.com/support/console/39.0/integration.js"
              sfc-interaction "https://login.salesforce.com/support/api/28.0/interaction.js"]
          (if-not sfc-integration
            (js/console.log "<----- SFC integration not found, not starting module ----->")
            (do (sfc-init sfc-integration sfc-interaction)
                (ih/register {:api {:salesforce-classic {:set-dimensions set-dimensions
                                                         :is-visible is-visible?
                                                         :set-visibility set-visibility
                                                         :focus-interaction focus-interaction
                                                         :assign-contact assign-contact}}
                              :module-name module-name})
                (ih/subscribe (topics/get-topic :presence-state-change-request-acknowledged) handle-state-change)
                (ih/subscribe (topics/get-topic :work-offer-received) handle-work-offer)
                (ih/subscribe (topics/get-topic :work-accepted-received) handle-work-accepted)
                (ih/subscribe (topics/get-topic :work-ended-received) handle-work-ended)
                (ih/subscribe (topics/get-topic :generic-screen-pop-received) handle-screen-pop)
                (ih/send-core-message {:type :module-registration-status
                                       :status :success
                                       :module-name module-name}))))
        (do (a/<! (a/timeout 250))
            (recur)))))
  (stop [this])
  (refresh-integration [this]))
