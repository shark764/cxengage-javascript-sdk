(ns cxengage-javascript-sdk.modules.salesforce-classic
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

(def sfc-state (atom {}))

(defn get-interaction [interaction-id]
  (or (get-in @sfc-state [:interactions interaction-id]) {}))

(defn add-interaction! [interaction]
  (let [{:keys [interactionId]} interaction]
    (swap! sfc-state assoc-in [:interactions interactionId] {:hook {}})))

(defn remove-interaction! [interaction-id]
  (swap! sfc-state iu/dissoc-in [:interactions interaction-id]))

(defn get-active-tab []
  (:active-tab @sfc-state))

(defn set-active-tab! [tab-details]
  (swap! sfc-state assoc-in [:active-tab] tab-details))

(defn get-hook [interaction-id]
  (or (get-in @sfc-state [:interactions interaction-id :hook]) {}))

(defn add-hook! [interaction-id hook-details]
  (swap! sfc-state assoc-in [:interactions interaction-id :hook] hook-details))

(defn remove-hook! [interaction-id]
  (swap! sfc-state assoc-in [:interactions interaction-id :hook] {}))

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
   :topic-key "cxengage/salesforce-classic/set-dimensions-response"}
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
   :topic-key "cxengage/salesforce-classic/is-visible-response"}
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
   :topic-key "cxengage/salesforce-classic/set-visibility-response"}
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
;; CxEngage.salesforceClassic.assign({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::assign-contact-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(defn send-assign-interrupt [tab-details interaction-id callback topic]
  (go
    (let [{:keys [object objectId objectName]} tab-details
          resource-id (state/get-active-user-id)
          interrupt-type "interaction-hook-add"
          interrupt-body {:hook-by resource-id
                          :hook-type "salesforce-classic"
                          :hook-sub-type object
                          :hook-id objectId
                          :hook-name objectName
                          :hook-pop (js/encodeURIComponent (js/JSON.stringify (clj->js tab-details)))
                          :resource-id resource-id}
          {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
      (if (= status 200)
        (let [hook (merge {:interaction-id interaction-id} (dissoc interrupt-body :hook-by :hook-pop :resource-id))]
          (add-hook! interaction-id hook)
          (ih/publish (clj->js {:topics topic
                                :response hook
                                :callback callback})))
        (ih/publish (clj->js {:topics topic
                              :error (e/failed-to-send-salesforce-classic-assign-err interaction-id interrupt-response)
                              :callback callback}))))))

(def-sdk-fn assign
  {:validation ::assign-contact-params
   :topic-key "cxengage/salesforce-classic/contact-assignment-acknowledged"}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        interaction (get-interaction interaction-id)
        hook (get-hook interaction-id)
        tab (get-active-tab)]
    (if-not (empty? interaction)
      (if (empty? hook)
        (if (and (not (empty? (:object tab))) (not (empty? (:objectId tab))))
          (send-assign-interrupt tab interaction-id callback topic)
          (ih/publish (clj->js {:topics topic
                                :error (e/failed-to-assign-blank-salesforce-classic-item-err interaction-id)
                                :callback callback})))
        (ih/publish (clj->js {:topics topic
                              :error (e/failed-to-assign-salesforce-classic-item-to-interaction-err interaction-id)
                              :callback callback})))
      (ih/publish (clj->js {:topics topic
                            :error (e/failed-to-assign-salesforce-classic-item-no-interaction-err interaction-id)
                            :callback callback})))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.salesforceClassic.unassign({
;;   interactionId: "{{uuid}}",
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::unassign-contact-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(defn send-unassign-interrupt [hook interaction-id callback topic]
  (go
    (let [interrupt-type "interaction-hook-drop"
          {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type hook))]
      (if (= status 200)
        (do
          (remove-hook! interaction-id)
          (ih/publish (clj->js {:topics topic
                                :response (merge {:interaction-id interaction-id} hook)
                                :callback callback})))
        (ih/publish (clj->js {:topics topic
                              :error (e/failed-to-send-salesforce-classic-unassign-err interaction-id interrupt-response)
                              :callback callback}))))))

(def-sdk-fn unassign
  {:validation ::unassign-contact-params
   :topic-key "cxengage/salesforce-classic/contact-unassignment-acknowledged"}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        interaction (get-interaction interaction-id)
        hook (get-hook interaction-id)]
    (if-not (empty? interaction)
      (if-not (empty? hook)
        (send-unassign-interrupt hook interaction-id callback topic)
        (ih/publish (clj->js {:topics topic
                              :error (e/failed-to-unassign-salesforce-classic-item-from-interaction-err interaction-id)
                              :callback callback})))
      (ih/publish (clj->js {:topics topic
                            :error (e/failed-to-assign-salesforce-classic-item-no-interaction-err interaction-id)
                            :callback callback})))))

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
   :topic-key "cxengage/salesforce-classic/focus-interaction"}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        hook-id (:hook-id (get-hook interaction-id))]
      (if hook-id
        (try
          (js/sforce.interaction.screenPop hook-id)
          (catch js/Object e
            (ih/publish (clj->js {:topics topic
                                  :error (e/failed-to-focus-salesforce-classic-interaction-err interaction-id)
                                  :callback callback}))))
        (ih/publish (clj->js {:topics topic
                              :error (e/failed-to-focus-salesforce-classic-interaction-err interaction-id)
                              :callback callback})))))

;; -------------------------------------------------------------------------- ;;
;; Helper Functions
;; -------------------------------------------------------------------------- ;;

(defn auto-assign-from-search-pop [response interaction-id]
  (let [result (:result (ih/extract-params response))
        parsed-result (ih/extract-params (js/JSON.parse result) true)]
      (if (= 1 (count (vals parsed-result)))
        (let [_ (log :info "Only one search result. Assigning:" (clj->js parsed-result))
              object-id (name (first (keys parsed-result)))
              object-map (first (vals parsed-result))
              object (get object-map :object)
              object-name (get object-map :Name)
              tab-details {:object object
                           :objectId object-id
                           :objectName object-name}]
          (send-assign-interrupt tab-details interaction-id nil "cxengage/salesforce-classic/contact-assignment-acknowledged"))
        (log :info "More than one result - skipping auto-assign"))))

(defn dump-state []
  (js/console.log (clj->js @sfc-state)))

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

(defn handle-focus-change [response]
  (let [result (-> response
                   (aget "result")
                   (js/JSON.parse)
                   (js->clj :keywordize-keys true))]
    (do
      (set-active-tab! result)
      (ih/publish {:topics "cxengage/salesforce-classic/active-tab-changed"
                   :response (js->clj result :keywordize-keys true)}))))

(defn handle-click-to-dial [dial-details]
  (let [result (:result (js->clj dial-details :keywordize-keys true))
        parsed-result (js/JSON.parse result)]
    (ih/publish {:topics "cxengage/salesforce-classic/on-click-to-interaction"
                 :response parsed-result})))

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
                              (let [tab-details (js->clj (js/JSON.parse (js/decodeURIComponent popUri)) :keywordize-keys true)
                                    object-id (get tab-details :objectId)
                                    hook {:interaction-id interactionId
                                          :hook-id object-id
                                          :hook-sub-type (get tab-details :object)
                                          :hook-name (get tab-details :objectName)
                                          :hook-type "salesforce-classic"}]
                                (log :info "Popping URI:" (clj->js tab-details))
                                (js/sforce.interaction.screenPop (str "/" object-id) true)
                                (add-hook! interactionId hook)
                                (ih/publish (clj->js {:topics "cxengage/salesforce-classic/contact-assignment-acknowledged"
                                                      :response hook})))
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
                                                                   :error e}))))))
           :else (log :error "Invalid pop type" popType))
        (log :debug "Ignoring non-v2 screen pop"))))

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
        (js/sforce.interaction.onFocus handle-focus-change)
        (js/sforce.interaction.cti.disableClickToDial)
        (try
          (js/sforce.interaction.cti.onClickToDial handle-click-to-dial)
          (ih/publish (clj->js {:topics "cxengage/salesforce-classic/initialize-complete"
                                :response true}))
          (js/sforce.interaction.getPageInfo handle-focus-change)
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
              sfc-integration "https://login.salesforce.com/support/console/41.0/integration.js"
              sfc-interaction "https://login.salesforce.com/support/api/28.0/interaction.js"]
          (sfc-init sfc-integration sfc-interaction)
          (ih/register {:api {:salesforce-classic {:set-dimensions set-dimensions
                                                   :is-visible is-visible?
                                                   :set-visibility set-visibility
                                                   :focus-interaction focus-interaction
                                                   :assign assign
                                                   :unassign unassign
                                                   :dump-state dump-state}}
                        :module-name module-name})
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
