(ns cxengage-javascript-sdk.modules.zendesk
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]])
  (:require [cljs.core.async :as a]
            [clojure.string :as string]
            [clojure.set :refer [intersection]]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.domain.errors :as error]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.api-utils :as api]
            [cxengage-javascript-sdk.domain.errors :as errors]
            [promesa.core :as prom :refer [promise all then]]
            [cljs.spec.alpha :as s]
            [goog.object :as goog]))

;; -------------------------------------------------------------------------- ;;
;; State Functions
;; -------------------------------------------------------------------------- ;;

(def ^:no-doc zendesk-state (atom {:active-tabs #{}}))

(defn- add-active-tab! [tab-guid]
  (swap! zendesk-state update-in [:active-tabs] conj tab-guid))

(defn- add-interaction! [interaction]
  (let [{:keys [interaction-id]} interaction]
    (swap! zendesk-state assoc-in [:interactions interaction-id] interaction)))

(defn- update-active-tab! [interaction-id active-tab]
  (swap! zendesk-state assoc-in [:interactions interaction-id :active-tab] active-tab))

;; -------------------------------------------------------------------------- ;;
;; Zendesk SDK Functions
;; -------------------------------------------------------------------------- ;;

;; -------------------------------------------------------------------------- ;;
;; CxEngage.zendesk.focusInteraction({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::focus-interaction-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn focus-interaction
  ""
  {:validation ::focus-interaction-params
   :topic-key "cxengage/zendesk/focus-interaction"}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        active-tab (get-in @zendesk-state [:interactions interaction-id :active-tab])
        agent-id (get @zendesk-state :zen-user-id)
        {:keys [result-type ticket-id user-id id]} active-tab]
      (cond
        (or (= result-type "user") user-id) (try
                                              (js/client.request (clj->js {:url (str "/api/v2/channels/voice/agents/" agent-id "/users/" (or id user-id) "/display.json")
                                                                           :type "POST"}))
                                              (catch js/Object e
                                                (ih/publish {:topics topic
                                                             :error (error/failed-to-focus-zendesk-interaction-err interaction-id e)
                                                             :callback callback})))
        (or (= result-type "ticket") ticket-id) (try
                                                  (js/client.request (clj->js {:url (str "/api/v2/channels/voice/agents/" agent-id "/tickets/" (or id ticket-id) "/display.json")
                                                                               :type "POST"}))
                                                  (catch js/Object e
                                                    (ih/publish {:topics topic
                                                                 :error (error/failed-to-focus-zendesk-interaction-err interaction-id e)
                                                                 :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.zendesk.setVisibility({
;;   visibility: "{{boolean}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::set-visibility-params
  (s/keys :req-un [::specs/visibility]
          :opt-un [::specs/callback]))

(def-sdk-fn set-visibility
  ""
  {:validation ::set-visibility-params
   :topic-key "cxengage/zendesk/set-visibility-response"}
  [params]
  (let [{:keys [topic visibility callback]} params]
    (if visibility
      (try
        (js/client.invoke "popover" "show")
        (catch js/Object e
          (ih/publish {:topics topic
                       :error (error/failed-to-set-zendesk-visibility-err e)
                       :callback callback})))
      (try
        (js/client.invoke "popover" "hide")
        (catch js/Object e
          (ih/publish {:topics topic
                       :error (error/failed-to-set-zendesk-visibility-err e)
                       :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.zendesk.setDimensions({
;;   height: "{{number}}",
;;   width: "{{number}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::set-dimensions-params
  (s/keys :req-un [::specs/width]
          :opt-un [::specs/callback ::specs/height]))

(def-sdk-fn set-dimensions
  ""
  {:validation ::set-dimensions-params
   :topic-key "cxengage/zendesk/set-dimensions-response"}
  [params]
  (let [{:keys [topic height width callback]} params]
    (try
      (let [resize (fn [height width]
                     (js/client.invoke "resize" (clj->js {:width width
                                                          :height height})))]
        (if height
          (resize height width)
          (.then (js/client.metadata)
                 (fn [metadata]
                   (-> metadata
                       (aget "settings")
                       (goog/get "height" "800")
                       (js/parseInt)
                       (resize width))))))
      (ih/publish {:topics topic
                   :response "true"
                   :callback callback})
      (catch js/Object e
        (ih/publish {:topics topic
                     :error (error/failed-to-set-zendesk-dimensions-err e)
                     :callback callback})))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.zendesk.assignRelatedTo({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::assign-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback ::specs/active-tab]))

(def-sdk-fn assign-related-to
  ""
  {:validation ::assign-params
   :topic-key "cxengage/zendesk/related-to-assignment-acknowledged"}
  [params]
  (let [{:keys [callback topic active-tab interaction-id]} params
        tenant-id (ih/get-active-tenant-id)
        interrupt-type "interaction-hook-add"
        {:keys [subject id ticket-id] :as ticket} (or active-tab (get @zendesk-state :active-tab))
        related-to (or id ticket-id)
        resource-id (state/get-active-user-id)
        interrupt-body {:hook-by (get @zendesk-state :zen-user-id)
                        :hook-type "zendesk"
                        :hook-sub-type "ticket"
                        :hook-id related-to
                        :hook-name subject
                        :hook-pop (str "/tickets/" related-to)
                        :resource-id resource-id}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (do
        (update-active-tab! interaction-id (or active-tab {:ticket-id related-to}))
        (ih/publish {:topics topic
                     :response (merge {:interaction-id interaction-id} interrupt-body (js->clj ticket :keywordize-keys true))
                     :callback callback}))
      (ih/publish {:topics topic
                   :error (error/failed-to-send-zendesk-assign-err interaction-id interrupt-response)
                   :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.zendesk.unassignRelatedTo({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::assign-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback ::specs/active-tab]))

(def-sdk-fn unassign-related-to
  ""
  {:validation ::assign-params
   :topic-key "cxengage/zendesk/related-to-unassignment-acknowledged"}
  [params]
  (let [{:keys [callback topic active-tab interaction-id]} params
        tenant-id (ih/get-active-tenant-id)
        interrupt-type "interaction-hook-drop"
        {:keys [subject id ticket-id] :as ticket} (or active-tab (get @zendesk-state :active-tab))
        related-to (or id ticket-id)
        resource-id (state/get-active-user-id)
        interrupt-body {:hook-by (get @zendesk-state :zen-user-id)
                        :hook-type "zendesk"
                        :hook-sub-type "ticket"
                        :hook-id related-to
                        :hook-name subject
                        :hook-pop (str "/tickets/" related-to)
                        :resource-id resource-id}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (do
        (update-active-tab! interaction-id (or active-tab {:ticket-id related-to}))
        (ih/publish {:topics topic
                     :response (merge {:interaction-id interaction-id} interrupt-body (js->clj ticket :keywordize-keys true))
                     :callback callback}))
      (ih/publish {:topics topic
                   :error (error/failed-to-send-zendesk-assign-err interaction-id interrupt-response)
                   :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.zendesk.assignContact({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn assign-contact
  ""
  {:validation ::assign-params
   :topic-key "cxengage/zendesk/contact-assignment-acknowledged"}
  [params]
  (let [{:keys [callback topic active-tab interaction-id]} params
        tenant-id (ih/get-active-tenant-id)
        interrupt-type "interaction-hook-add"
        {:keys [name id user-id] :as user} (or active-tab (get @zendesk-state :active-tab))
        contact (or id user-id)
        resource-id (state/get-active-user-id)
        interrupt-body {:hook-by (get @zendesk-state :zen-user-id)
                        :hook-type "zendesk"
                        :hook-sub-type "user"
                        :hook-id contact
                        :hook-name name
                        :hook-pop (str "/users/" contact)
                        :resource-id resource-id}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (do
        (update-active-tab! interaction-id (or active-tab {:user-id contact}))
        (ih/publish {:topics topic
                     :response (merge {:interaction-id interaction-id} interrupt-body (js->clj user :keywordize-keys true))
                     :callback callback}))
      (ih/publish {:topics topic
                   :error (error/failed-to-send-zendesk-assign-err interaction-id interrupt-response)
                   :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.zendesk.unassignContact({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(def-sdk-fn unassign-contact
  ""
  {:validation ::assign-params
   :topic-key "cxengage/zendesk/contact-unassignment-acknowledged"}
  [params]
  (let [{:keys [callback topic active-tab interaction-id]} params
        tenant-id (ih/get-active-tenant-id)
        interrupt-type "interaction-hook-drop"
        {:keys [name id user-id] :as user} (or active-tab (get @zendesk-state :active-tab))
        contact (or id user-id)
        resource-id (state/get-active-user-id)
        interrupt-body {:hook-by (get @zendesk-state :zen-user-id)
                        :hook-type "zendesk"
                        :hook-sub-type "user"
                        :hook-id contact
                        :hook-name name
                        :hook-pop (str "/users/" contact)
                        :resource-id resource-id}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (do
        (update-active-tab! interaction-id (or active-tab {:user-id contact}))
        (ih/publish {:topics topic
                     :response (merge {:interaction-id interaction-id} interrupt-body (js->clj user :keywordize-keys true))
                     :callback callback}))
      (ih/publish {:topics topic
                   :error (error/failed-to-send-zendesk-assign-err interaction-id interrupt-response)
                   :callback callback}))))
;; -------------------------------------------------------------------------- ;;
;; Zendesk Initialization Functions
;; -------------------------------------------------------------------------- ;;

(defn- zendesk-ready? []
  (aget js/window "ZAFClient"))

(defn- zendesk-init
  [integration]
  (let [script (js/document.createElement "script")
        body (.-body js/document)]
    (.setAttribute script "type" "text/javascript")
    (.setAttribute script "src" integration)
    (.appendChild body script)
    (go-loop []
      (if (zendesk-ready?)
        (try
          (aset js/window "client"
            (js/ZAFClient.init
              (fn [context]
                (.then (js/client.get "currentUser")
                       (fn [user]
                         (swap! zendesk-state assoc :zen-user-id (get-in (js->clj user :keywordize-keys true) [:currentUser :id]))))
                (swap! zendesk-state assoc :context context)
                (js/client.trigger "initialised") ;; event for user/ticket widgets to set active tab
                (js/client.on "triggerClickToDial" (fn [data]
                                                    (ih/publish {:topics "cxengage/zendesk/click-to-dial-requested"
                                                                 :response data})))
                (js/client.on "triggerClickToSms" (fn [data]
                                                   (ih/publish {:topics "cxengage/zendesk/click-to-sms-requested"
                                                                :response data})))
                (js/client.on "triggerClickToEmail" (fn [data]
                                                     (ih/publish {:topics "cxengage/zendesk/click-to-email-requested"
                                                                  :response data})))
                (js/client.on "updateUser" (fn [data]
                                            (ih/publish {:topics "cxengage/zendesk/user-update"
                                                         :response data})))
                (js/client.on "updateTicket" (fn [data]
                                              (ih/publish {:topics "cxengage/zendesk/ticket-update"
                                                           :response data})))
                (js/client.on "activeTab" (fn [tab-data]
                                            (let [tab-data (ih/extract-params tab-data)]
                                              (if (:user-id tab-data)
                                                (.then (js/client.request (str "/api/v2/users/" (:user-id tab-data) ".json"))
                                                       (fn [response]
                                                         (aset response "user" "userId" (aget response "user" "id"))
                                                         (aset response "user" "type" "user")
                                                         (swap! zendesk-state assoc :active-tab (:user (js->clj response :keywordize-keys true)))
                                                         (ih/publish {:topics "cxengage/zendesk/active-tab-changed"
                                                                      :response (js->clj response :keywordize-keys true)})))
                                                (.then (js/client.request (str "/api/v2/tickets/" (:ticket-id tab-data) ".json"))
                                                       (fn [response]
                                                         (aset response "ticket" "ticketId" (aget response "ticket" "id"))
                                                         (aset response "ticket" "type" "ticket")
                                                         (swap! zendesk-state assoc :active-tab (:ticket (js->clj response :keywordize-keys true)))
                                                         (ih/publish {:topics "cxengage/zendesk/active-tab-changed"
                                                                      :response (js->clj response :keywordize-keys true)}))))
                                              (.then (js/client.get "instances")
                                                (fn [response]
                                                  (let [app-guid (:app-guid tab-data)
                                                        active-instances (set (map name (keys (:instances (js->clj response :keywordize-keys true)))))]
                                                    ;; Add listener for tab being deactivated (closed or unfocused) if we haven't yet added it.
                                                    (when (not (iu/in? (get @zendesk-state :active-tabs) app-guid))
                                                      (let [guidInstance (js/client.instance app-guid)]
                                                        (.on guidInstance "app.deactivated" (fn []
                                                                                              (ih/publish {:topics "cxengage/zendesk/active-tab-changed"
                                                                                                           :response {}})))
                                                        (add-active-tab! app-guid)))
                                                    ;; Remove any tabs that have been closed from our list
                                                    (swap! zendesk-state assoc :active-tabs (intersection (get @zendesk-state :active-tabs) active-instances))))))))
                (ih/publish {:topics "cxengage/zendesk/zendesk-initialization"
                             :response true}))))
          (catch js/Object e
            (ih/publish {:topics "cxengage/zendesk/zendesk-initialization"
                         :error (error/failed-to-init-zendesk-client-err e)})))
        (do (a/<! (a/timeout 250))
            (recur))))))

(defn- dump-state []
  (js/console.log (clj->js @zendesk-state)))

;; -------------------------------------------------------------------------- ;;
;; Helper Functions
;; -------------------------------------------------------------------------- ;;

(defn- auto-assign-from-search-pop [search-result interaction-id type]
  (let [agent-id (get @zendesk-state :zen-user-id)]
    (if (= type "user")
      (.then
        (js/client.request (clj->js {:url (str "/api/v2/channels/voice/agents/" agent-id "/users/" (:id search-result) "/display.json")
                                     :type "POST"}))
        (fn []
          (assign-contact (clj->js {:interactionId interaction-id :active-tab search-result}))))
      (.then
        (js/client.request (clj->js {:url (str "/api/v2/channels/voice/agents/" agent-id "/tickets/" (:id search-result) "/display.json")
                                     :type "POST"}))
        (fn []
          (assign-related-to (clj->js {:interactionId interaction-id :active-tab search-result})))))))

(defn- pop-search-modal [search-results interaction-id]
  (let [region (state/get-region)
        env (name (state/get-env))]
    (.then (js/client.invoke
            "instances.create"
            (clj->js {:location "modal"
                      :url (str "https://sdk.cxengage.net/zendesk/" region "-" env "/modal.html")}))
           (fn [modal-context]
             (let [modal-guid (-> modal-context
                                   (aget "instances.create")
                                   (first)
                                   (aget "instanceGuid"))
                   modalClient (js/client.instance modal-guid)]
                (js/setTimeout #(.trigger modalClient "displaySearch" (clj->js search-results)) 2000)
                (js/modalClient.on "assign"
                                   (fn [contact]
                                     (let [contact (ih/extract-params contact)
                                           type (:result-type contact)]
                                        (when (= type "user")
                                          (assign-contact {:interaction-id interaction-id :active-tab contact}))
                                        (when (= type "ticket")
                                          (assign-related-to {:interaction-id interaction-id :active-tab contact}))))))))))

;; -------------------------------------------------------------------------- ;;
;; Subscription Handlers
;; -------------------------------------------------------------------------- ;;

;; Helper function for handle-screen-pop strict and fuzzy search results
(defn- handle-search-results [search-results interaction interaction-id]
  (cond
    (= (count search-results) 0) (ih/publish {:topics "cxengage/zendesk/search-and-pop-no-results-received"
                                              :response []})
    (= (count search-results) 1) (if (= (:result_type (first search-results)) "user")
                                  (auto-assign-from-search-pop (first search-results) interaction-id "user")
                                  (auto-assign-from-search-pop (first search-results) interaction-id "ticket"))
    :else (if (or (= (:auto-answer interaction) true)
                  (and (= (:direction interaction) "agent-initiated")
                    (or (= (:channel-type interaction) "email")
                        (= (:channel-type interaction) "sms"))))
              (pop-search-modal search-results interaction-id)
              (ih/subscribe (topics/get-topic :work-accepted-received)
                            (fn [e,t,r,subscription-id]
                              (if (= interaction-id (:interaction-id (ih/extract-params r)))
                                (do
                                  (pop-search-modal search-results interaction-id)
                                  (p/unsubscribe subscription-id))))))))

(defn- handle-work-offer [error topic interaction-details]
  (let [interaction (ih/extract-params interaction-details)
        agent-id (get @zendesk-state :zen-user-id)
        {:keys [contact related-to pop-on-accept interactionId]} interaction]
      (add-interaction! interaction)
      (cond
        pop-on-accept (log :info "Pop On Accept - waiting for work-accepted-received")
        contact (js/client.invoke "routeTo" "user" contact)
        related-to (js/client.invoke "routeTo" "ticket" related-to)
        :else (log :info "No screen pop details on work offer"))))

(defn- handle-screen-pop [error topic interaction-details]
  (let [result (ih/extract-params interaction-details)
        agent-id (get @zendesk-state :zen-user-id)
        {:keys [pop-type pop-uri new-window size search-type filter filter-type terms interaction-id]} result
        interaction (get-in @zendesk-state [:interactions interaction-id])
        formatted-strict-terms (into [] (for [[k v] filter] (str (name k) ":" "\"" v "\"")))]
    (cond
      (= pop-type "url") (do
                            (js/client.request (clj->js {:url (str "/api/v2/channels/voice/agents/" agent-id pop-uri "/display.json")
                                                         :type "POST"}))
                            (.then (js/client.request (str "/api/v2/" pop-uri ".json"))
                                   (fn [response]
                                    (ih/publish {:topics "cxengage/zendesk/internal-pop-received"
                                                 :response (merge {:interaction-id interaction-id} (js->clj response :keywordize-keys true))}))))
      (= pop-type "search-pop") (do
                                  (when (or (= search-type "fuzzy") (and (= search-type "strict") (= filter-type "or")))
                                    (let [all-req-promises (reduce
                                                            (fn [all-promises term]
                                                              (conj all-promises (promise
                                                                                  (fn [resolve reject]
                                                                                    (let [formatted-term (if (= search-type "fuzzy") 
                                                                                                              (if (nil? (string/index-of (string/trim term) " "))
                                                                                                                (str term "*")
                                                                                                                (str "\"" term "\"")))]
                                                                                    (.then (js/client.request (clj->js {:url (if (= search-type "fuzzy") 
                                                                                                                                (str "/api/v2/search.json?query=user:"formatted-term) 
                                                                                                                                (str "/api/v2/search.json?query="term))}))
                                                                                           (fn [data]
                                                                                             (resolve (:results (js->clj data :keywordize-keys true))))))))))
                                                            []
                                                            (if (= search-type "fuzzy") terms formatted-strict-terms))
                                          all-req-promises (all all-req-promises)]
                                      (then all-req-promises
                                        (fn [results]
                                          (let [combined-results (reduce
                                                                  (fn [all-results results]
                                                                    (conj [all-results] results))
                                                                  []
                                                                  results)
                                                search-results (distinct (vec (flatten combined-results)))]
                                            (handle-search-results search-results interaction interaction-id))))))
                                  (when (and (= search-type "strict") (= filter-type "and"))
                                    (let [query (reduce-kv
                                                 (fn [s k v]
                                                   (str s (name k) ":" v " "))
                                                 "/api/v2/search.json?query="
                                                 filter)]
                                       (.then (js/client.request (clj->js {:url query}))
                                              (fn [result]
                                                (let [search-results (:results (js->clj result :keywordize-keys true))]
                                                  (handle-search-results search-results interaction interaction-id))))))))))

;; -------------------------------------------------------------------------- ;;
;; Zendesk Module
;; -------------------------------------------------------------------------- ;;

(defrecord ZendeskModule []
  pr/SDKModule
  (start [this]
    (go-loop []
      (if (ih/core-ready?)
        (let [module-name :zendesk
              zendesk-integration "https://static.zdassets.com/zendesk_app_framework_sdk/2.0/zaf_sdk.min.js"]
            (zendesk-init zendesk-integration)
            (ih/subscribe (topics/get-topic :work-offer-received) handle-work-offer)
            (ih/subscribe (topics/get-topic :generic-screen-pop-received) handle-screen-pop)
            (ih/register (clj->js {:api {:zendesk {:focus-interaction focus-interaction
                                                   :set-dimensions set-dimensions
                                                   :set-visibility set-visibility
                                                   :assign-contact assign-contact
                                                   :assign-related-to assign-related-to
                                                   :unassign-contact unassign-contact
                                                   :unassign-related-to unassign-related-to
                                                   :dump-state dump-state}}
                                   :module-name module-name}))
            (ih/send-core-message {:type :module-registration-status
                                   :status :success
                                   :module-name module-name}))
        (do (a/<! (a/timeout 250))
            (recur)))))
  (stop [this])
  (refresh-integration [this]))
