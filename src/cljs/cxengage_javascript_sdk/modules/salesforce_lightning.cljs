(ns cxengage-javascript-sdk.modules.salesforce-lightning
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn]])
  (:require [cljs.core.async :as a]
            [clojure.string :as string]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.api-utils :as api]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.domain.errors :as errors]
            [cljs.spec.alpha :as s]))

;; -------------------------------------------------------------------------- ;;
;; State Functions
;; -------------------------------------------------------------------------- ;;

(def sfl-state (atom {}))

(defn set-current-salesforce-user-id! [user-id]
  (swap! sfl-state assoc :user-id user-id))

(defn get-current-salesforce-user-id []
  (or (:user-id @sfl-state) ""))

(defn get-interaction [interaction-id]
  (or (get-in @sfl-state [:interactions interaction-id]) {}))

(defn add-interaction! [interaction]
  (let [{:keys [interactionId]} interaction]
    (swap! sfl-state assoc-in [:interactions interactionId] {:hook {}})))

(defn remove-interaction! [interaction-id]
  (swap! sfl-state iu/dissoc-in [:interactions interaction-id]))

(defn get-active-tab []
  (:active-tab @sfl-state))

(defn set-active-tab! [tab-details]
  (swap! sfl-state assoc-in [:active-tab] tab-details))

(defn get-hook [interaction-id]
  (or (get-in @sfl-state [:interactions interaction-id :hook]) {}))

(defn add-hook! [interaction-id hook-details]
  (swap! sfl-state assoc-in [:interactions interaction-id :hook] hook-details))

(defn remove-hook! [interaction-id]
  (swap! sfl-state assoc-in [:interactions interaction-id :hook] {}))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.salesforceLightning.setDimensions({
;;   height: "{{number}}",
;;   width: "{{number}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::set-dimensions-params
  (s/keys :req-un [::specs/width]
          :opt-un [::specs/height ::specs/callback]))

(def-sdk-fn set-dimensions
  {:validation ::set-dimensions-params
   :topic-key "cxengage/salesforce-lightning/set-dimensions-response"}
  [params]
  (let [{:keys [topic height width callback] } params]
    (try
      (if height
        (js/sforce.opencti.setSoftphonePanelHeight (clj->js {:heightPX height}))
        (js/sforce.opencti.getCallCenterSettings
         (clj->js {:callback  (fn [response]
                                (let [sfHeight (aget response "returnValue" "/reqGeneralInfo/reqSoftphoneHeight")]
                                  (js/sforce.opencti.setSoftphonePanelHeight (clj->js {:heightPX sfHeight}))))})))
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
                                (let [result (aget response "returnValue" "visible")]
                                  (ih/publish (clj->js {:topics topic
                                                        :response result
                                                        :callback callback}))))}))
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
;; CxEngage.salesforceLightning.assign({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::assign-contact-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))
(defn send-assign-interrupt [new-hook interaction-id callback topic]
  (go
    (let [{:keys [hookId hookSubType hookName]} new-hook
          resource-id (state/get-active-user-id)
          interrupt-type "interaction-hook-add"
          interrupt-body (merge new-hook {:hook-by (get-current-salesforce-user-id)
                                          :hook-name hookName
                                          :hook-id hookId
                                          :hook-sub-type hookSubType
                                          :hook-type "salesforce-lightning"
                                          :hook-pop (js/encodeURIComponent (js/JSON.stringify (clj->js new-hook)))
                                          :resource-id resource-id})
          {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
       (if (= status 200)
         (do
           (add-hook! interaction-id (js->clj (ih/camelify new-hook) :keywordize-keys true))
           (ih/publish (clj->js {:topics topic
                                 :response (merge {:interaction-id interaction-id} new-hook)
                                 :callback callback})))
         (ih/publish (clj->js {:topics topic
                               :error (e/failed-to-send-salesforce-lightning-assign-err interaction-id interrupt-response)
                               :callback callback}))))))

(def-sdk-fn assign
  {:validation ::assign-contact-params
   :topic-key "cxengage/salesforce-lightning/contact-assignment-acknowledged"}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        interaction (get-interaction interaction-id)
        hook (get-hook interaction-id)
        tab (get-active-tab)]
    (if-not (empty? interaction)
      (if (empty? hook)
        (if (and (not (empty? (:hookSubType tab))) (not (empty? (:hookId tab))))
          (send-assign-interrupt tab interaction-id callback topic)
          (ih/publish (clj->js {:topics topic
                                :error (e/failed-to-assign-blank-salesforce-lightning-item-err interaction-id)
                                :callback callback})))
        (ih/publish (clj->js {:topics topic
                              :error (e/failed-to-assign-salesforce-lightning-item-to-interaction-err interaction-id)
                              :callback callback})))
      (ih/publish (clj->js {:topics topic
                            :error (e/failed-to-assign-salesforce-lightning-item-no-interaction-err interaction-id)
                            :callback callback})))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.salesforceLightning.unassign({
;;   interactionId: "{{uuid}}",
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::unassign-contact-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(defn send-unassign-interrupt [hook interaction-id callback topic]
  (go
    (let [interrupt-type "interaction-hook-drop"
          agent-id (state/get-active-user-id)
          agent-hook (assoc hook :resourceId agent-id :hookBy (get-current-salesforce-user-id))
          {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type agent-hook))]
      (if (= status 200)
        (do
          (remove-hook! interaction-id)
          (ih/publish (clj->js {:topics topic
                                :response (merge {:interaction-id interaction-id} agent-hook)
                                :callback callback})))
        (ih/publish (clj->js {:topics topic
                              :error (e/failed-to-send-salesforce-lightning-unassign-err interaction-id interrupt-response)
                              :callback callback}))))))

(def-sdk-fn unassign
  {:validation ::unassign-contact-params
   :topic-key "cxengage/salesforce-lightning/contact-unassignment-acknowledged"}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        interaction (get-interaction interaction-id)
        hook (get-hook interaction-id)]
    (if-not (empty? interaction)
      (if-not (empty? hook)
        (send-unassign-interrupt hook interaction-id callback topic)
        (ih/publish (clj->js {:topics topic
                              :error (e/failed-to-unassign-salesforce-lightning-item-from-interaction-err interaction-id)
                              :callback callback})))
      (ih/publish (clj->js {:topics topic
                            :error (e/failed-to-assign-salesforce-lightning-item-no-interaction-err interaction-id)
                            :callback callback})))))

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
   :topic-key "cxengage/salesforce-lightning/focus-interaction"}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        hook-id (:hookId (get-hook interaction-id))]
    (if hook-id
      (try
        (js/sforce.opencti.screenPop (clj->js {:type "sobject" :params {:recordId hook-id}}))
        (catch js/Object e
          (ih/publish (clj->js {:topics topic
                                :error e
                                :callback callback}))))
      (ih/publish (clj->js {:topics topic
                            :error (e/failed-to-focus-salesforce-lightning-interaction-err interaction-id)
                            :callback callback})))))

;; -------------------------------------------------------------------------- ;;
;; Helper Functions
;; -------------------------------------------------------------------------- ;;

(defn auto-assign-from-search-pop [response interaction-id]
  (let [result (:returnValue (ih/extract-params response true))]
    (if (= 1 (count (vals result)))
      (let [_ (log :info "Only one search result. Assigning:" (clj->js result))
            record (first (vals result))
            hook-id (.slice (:Id record) 0 -3)
            ;; Refer to the handle-focus-change comment block for a description
            ;; of why we are slicin here. TL;DR Salesforce Gateway adherence
            hook {:hookId hook-id
                  :hookSubType (:RecordType record)
                  :hookName (:Name record)}]
        (send-assign-interrupt hook interaction-id nil "cxengage/salesforce-lightning/contact-assignment-acknowledged"))
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

(defn handle-focus-change [response]
  (let [active-tab (get-active-tab)
        result (js->clj response :keywordize-keys true)
        ;; So.. the Salesforce Classic SDK provides us with the salesforce object
        ;; id with the last 3 characters removed. Salesforce Gateway accounts for
        ;; this by also removing the 3 characters on the end of the fully qualified
        ;; object id. However - in the salesforce lightning SDK, it returns us
        ;; the fully qualified id, so in order to work properly with salesforce
        ;; gateway, we need to preemtively remove the trailing 3 characters.
        ;; ¯\_(ツ)_/¯ ¯\_(ツ)_/¯ ¯\_(ツ)_/¯ ¯\_(ツ)_/¯ ¯\_(ツ)_/¯ ¯\_(ツ)_/¯
        hook-id (if (:recordId result)
                  (.slice (:recordId result) 0 -3)
                  nil)
        hook {:hookId hook-id
              :hookSubType (:objectType result)
              :hookName (:recordName result)}]
    (if-not (and (= (:hookId active-tab) (:hookId hook))
                 (= (:hookSubType active-tab) (:hookSubType hook)))
      (do
        (set-active-tab! hook)
        (ih/publish {:topics "cxengage/salesforce-lightning/active-tab-changed"
                     :response hook}))
      (log :debug "Active tab is the same. Not publishing change."))))

(defn handle-get-app-view-info [response]
  (let [result (js->clj response :keywordize-keys true)
        focus-response (clj->js (:returnValue result))]
    (handle-focus-change focus-response)))

(defn handle-click-to-dial [dial-details]
  (ih/publish {:topics "cxengage/salesforce-lightning/on-click-to-interaction"
               :response dial-details}))

(defn handle-work-offer [error topic interaction-details]
  (let [interaction (js->clj interaction-details :keywordize-keys true)]
    (add-interaction! interaction)))

(defn handle-work-ended [error topic interaction-details]
  (let [interaction-id (:interactionId (js->clj interaction-details :keywordize-keys true))]
    (remove-interaction! interaction-id)))

(defn handle-screen-pop [error topic interaction-details]
  (let [_ (log :debug "Handling screen pop:" interaction-details)
        result (js->clj interaction-details :keywordize-keys true)
        {:keys [version popType popUrl popUri newWindow size searchType filter filterType terms interactionId]} result
        pop-callback (fn [response]
                        (auto-assign-from-search-pop response interactionId))]
    (if (= "v2" version)
      (cond
        (= popType "url") (try
                            ;; Check if the popUri is JSON. If it is, that is one we assigned and are getting transferred.
                            ;; If not, it is a work item from flow. It will be formatted like: "<case-number>/<object-id>"
                            (if (try (js/JSON.parse (js/decodeURIComponent popUri))
                                  true
                                  (catch js/Object e
                                    false))
                              (let [tab-details (js->clj (js/JSON.parse (js/decodeURIComponent popUri)) :keywordize-keys true)
                                    object-id (or (get tab-details :objectId) (get tab-details :hookId))
                                    hook {:interaction-id interactionId
                                          :hook-id object-id
                                          :hook-sub-type (or (get tab-details :object) (get tab-details :hookSubType))
                                          :hook-name (or (get tab-details :objectName) (get tab-details :hookName))
                                          :hook-type "salesforce-lightning"}]
                                (log :info "Popping transferred URI:" (clj->js tab-details))
                                (js/sforce.opencti.screenPop (clj->js {:type "sobject" :params {:recordId object-id}}))
                                (send-assign-interrupt (js->clj (ih/camelify tab-details) :keywordize-keys true)
                                                        interactionId
                                                        nil
                                                        "cxengage/salesforce-lightning/contact-assignment-acknowledged"))
                              (let [uri-params (string/split popUri #"/")
                                    object-name (first uri-params)
                                    object-id (second uri-params)
                                    tab-details {:hook-sub-type "Case"
                                                 :hook-id object-id
                                                 :hook-name object-name}]
                                (log :info "Popping work item URI:" object-id object-name)
                                (js/sforce.opencti.screenPop (clj->js {:type "sobject" :params {:recordId object-id}}))
                                (send-assign-interrupt (js->clj (ih/camelify tab-details) :keywordize-keys true)
                                                       interactionId
                                                       nil
                                                       "cxengage/salesforce-lightning/contact-assignment-acknowledged")))
                            (catch js/Object e
                              (ih/publish (clj->js {:topics topic
                                                    :error e}))))
        (= popType "external") (if (= newWindow "true")
                                 (js/window.open popUrl "targetWindow" (str "width=" (:width size) ",height=" (:height size)))
                                 (js/window.open popUrl))
        (= popType "search-pop") (if (or (= searchType "fuzzy")
                                         (= searchType "strict"))
                                   (let [search-params (if (= searchType "fuzzy")
                                                        (string/join " or " terms)
                                                        (string/join (str " " filterType " ") (vals filter)))]
                                     (try
                                       (js/sforce.opencti.searchAndScreenPop (clj->js {:searchParams search-params
                                                                                       :queryParams ""
                                                                                       :callType "inbound"
                                                                                       :callback pop-callback}))
                                       (catch js/Object e
                                         (ih/publish (clj->js {:topics topic
                                                               :error e})))))
                                  (log :error "Invalid search type" searchType))
        :else (log :error "Invalid pop type" popType))
      (log :debug "Ignoring non-v2 screen pop"))))

(defn handle-get-current-user-id [js-response]
  (let [response (js->clj js-response :keywordize-keys true)
        result (get-in response [:returnValue :runApex])
        error (:errors response)]
    (if-not error
      (set-current-salesforce-user-id! result)
      (ih/publish (clj->js {:topics "cxengage/salesforce-lightning/failed-to-get-current-user-id"
                            :error (e/failed-to-get-current-salesforce-lightning-user-id-err error)})))))

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
        (js/sforce.opencti.onNavigationChange (clj->js {:listener handle-focus-change}))
        (js/sforce.opencti.runApex (clj->js {:apexClass "net_cxengage.CxLookup"
                                             :methodName "getCurrentUserId"
                                             :methodParams ""
                                             :callback handle-get-current-user-id}))
        (try
          (js/sforce.opencti.onClickToDial (clj->js {:listener handle-click-to-dial}))
          (ih/publish (clj->js {:topics "cxengage/salesforce-lightning/initialize-complete"
                                :response true}))
          (js/sforce.opencti.getAppViewInfo (clj->js {:callback handle-get-app-view-info}))
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
                                                              :assign assign
                                                              :unassign unassign
                                                              :dump-state dump-state}}
                                 :module-name module-name}))
          (ih/subscribe (topics/get-topic :presence-state-change-request-acknowledged) handle-state-change)
          (ih/subscribe (topics/get-topic :work-offer-received) handle-work-offer)
          (ih/subscribe (topics/get-topic :work-ended-received) handle-work-ended)
          (ih/subscribe (topics/get-topic :generic-screen-pop-received) handle-screen-pop)
          (ih/send-core-message {:type :module-registration-status
                                 :status :success
                                 :module-name module-name}))
        (do (a/<! (a/timeout 250))
            (recur)))))
  (stop [this])
  (refresh-integration [this]))
