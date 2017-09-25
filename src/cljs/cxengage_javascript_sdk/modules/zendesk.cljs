(ns cxengage-javascript-sdk.modules.zendesk
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn]])
  (:require [cljs.core.async :as a]
            [clojure.string :as string]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.domain.errors :as error]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.api-utils :as api]
            [cxengage-javascript-sdk.domain.errors :as errors]
            [promesa.core :as prom :refer [promise all then]]
            [cljs.spec.alpha :as s]))

;; -------------------------------------------------------------------------- ;;
;; State Functions
;; -------------------------------------------------------------------------- ;;

(def zendesk-state (atom {}))

(defn add-interaction! [interaction]
  (let [{:keys [interaction-id]} interaction]
    (swap! zendesk-state assoc-in [:interactions interaction-id] interaction)))

(defn update-active-tab! [interaction-id active-tab]
  (swap! zendesk-state assoc-in [:interactions interaction-id :active-tab] active-tab))

(defn get-active-tab [interaction-id]
  (get-in @zendesk-state [:interactions interaction-id :active-tab]))

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
  {:validation ::focus-interaction-params
   :topic-key "cxengage/zendesk/focus-interaction"}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        active-tab (get-in @zendesk-state [:interactions interaction-id :active-tab])
        agent-id (get @zendesk-state :zen-user-id)
        {:keys [ticket-id user-id]} active-tab]
      (when user-id
        (try
          (js/client.request (clj->js {:url (str "/api/v2/channels/voice/agents/" agent-id "/users/" user-id "/display.json")
                                       :type "POST"}))
          (catch js/Object e
            (ih/publish {:topics topic
                         :error (error/failed-to-focus-zendesk-interaction-err interaction-id e)
                         :callback callback}))))
      (when ticket-id
        (try
          (js/client.request (clj->js {:url (str "/api/v2/channels/voice/agents/" agent-id "/tickets/" ticket-id "/display.json")
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
                       :callback callback}))))
      (try
        (js/client.invoke "popover" "hide")
        (catch js/Object e
          (ih/publish {:topics topic
                       :error (error/failed-to-set-zendesk-visibility-err e)
                       :callback callback})))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.zendesk.setDimensions({
;;   height: "{{number}}",
;;   width: "{{number}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::set-dimensions-params
  (s/keys :req-un [::specs/height ::specs/width]
          :opt-un [::specs/callback]))

(def-sdk-fn set-dimensions
  {:validation ::set-dimensions-params
   :topic-key "cxengage/zendesk/set-dimensions-response"}
  [params]
  (let [{:keys [topic height width callback]} params]
      (try
        (js/client.invoke "resize" (clj->js {:width width
                                             :height height}))
        (ih/publish {:topics topic
                     :response "true"
                     :callback callback})
        (catch js/Object e
          (ih/publish {:topics topic
                       :error (error/failed-to-set-zendesk-dimensions-err e)
                       :callback callback})))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.sfc.assignContact({
;;   interactionId: "{{uuid}}"
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::assign-params
  (s/keys :req-un [::specs/interaction-id]
          :opt-un [::specs/callback]))

(def-sdk-fn assign-related-to
  {:validation ::assign-params
   :topic-key "cxengage/zendesk/related-to-assignment-acknowledged"}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        tenant-id (ih/get-active-tenant-id)
        interrupt-type "assign-related-to"
        active-tab (get @zendesk-state :active-tab)
        related-to (:ticket-id active-tab)
        interrupt-body {:external-crm-user (get @zendesk-state :zen-user-id)
                        :external-crm-name "zendesk"
                        :external-crm-related-to related-to
                        :external-crm-related-to-uri (str "/tickets/" related-to)}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (do
        (update-active-tab! interaction-id active-tab)
        (.then (js/client.request (str "/api/v2/tickets/" related-to ".json"))
               (fn [response]
                 (ih/publish {:topics topic
                              :response (merge {:interaction-id interaction-id} interrupt-body (js->clj response :keywordize-keys true))
                              :callback callback}))))
      (ih/publish {:topics topic
                   :error (error/failed-to-send-zendesk-assign-err interaction-id interrupt-response)
                   :callback callback}))))

(def-sdk-fn assign-contact
  {:validation ::assign-params
   :topic-key "cxengage/zendesk/contact-assignment-acknowledged"}
  [params]
  (let [{:keys [callback topic interaction-id]} params
        tenant-id (ih/get-active-tenant-id)
        interrupt-type "assign-contact"
        active-tab (get @zendesk-state :active-tab)
        contact (:user-id active-tab)
        interrupt-body {:external-crm-user (get @zendesk-state :zen-user-id)
                        :external-crm-name "zendesk"
                        :external-crm-contact contact
                        :external-crm-contact-uri (str "/users/" contact)}
        {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (do
        (update-active-tab! interaction-id active-tab)
        (.then (js/client.request (str "/api/v2/users/" contact ".json"))
               (fn [response]
                 (ih/publish {:topics topic
                              :response (merge {:interaction-id interaction-id} interrupt-body (js->clj response :keywordize-keys true))
                              :callback callback}))))
      (ih/publish {:topics topic
                   :error (error/failed-to-send-zendesk-assign-err interaction-id interrupt-response)
                   :callback callback}))))


;; -------------------------------------------------------------------------- ;;
;; Zendesk Initialization Functions
;; -------------------------------------------------------------------------- ;;

(defn zendesk-ready? []
  (aget js/window "ZAFClient"))

(defn ^:private zendesk-init
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
                (js/client.on "triggerClickToDial" (fn [data]
                                                    (ih/publish {:topics "cxengage/zendesk/click-to-dial-requested"
                                                                 :response data})))
                (js/client.on "triggerClickToSms" (fn [data]
                                                   (ih/publish {:topics "cxengage/zendesk/click-to-sms-requested"
                                                                :response data})))
                (js/client.on "triggerClickToEmail" (fn [data]
                                                     (ih/publish {:topics "cxengage/zendesk/click-to-email-requested"
                                                                  :response data})))
                (js/client.on "activeTab" (fn [tab-data]
                                            (swap! zendesk-state assoc :active-tab (ih/extract-params tab-data))
                                            (ih/publish {:topics "cxengage/zendesk/active-tab-changed"
                                                         :response tab-data})))
                (ih/publish {:topics "cxengage/zendesk/zendesk-initialization"
                             :response true}))))
          (catch js/Object e
            (ih/publish {:topics "cxengage/zendesk/zendesk-initialization"
                         :error (error/failed-to-init-zendesk-client-err e)})))
        (do (a/<! (a/timeout 250))
            (recur))))))

(defn dump-state []
  (js/console.log (clj->js @zendesk-state)))

;; -------------------------------------------------------------------------- ;;
;; Helper Functions
;; -------------------------------------------------------------------------- ;;

(defn auto-assign-from-search-pop [search-result interaction-id type]
  (let [agent-id (get @zendesk-state :zen-user-id)]
    (if (= type "user")
      (.then
        (js/client.request (clj->js {:url (str "/api/v2/channels/voice/agents/" agent-id "/users/" (:id search-result) "/display.json")
                                     :type "POST"}))
        (fn []
          (js/setTimeout #(assign-contact (clj->js {:interactionId interaction-id})) 2500)))
      (.then
        (js/client.request (clj->js {:url (str "/api/v2/channels/voice/agents/" agent-id "/tickets/" (:id search-result) "/display.json")
                                     :type "POST"}))
        (fn []
          (js/setTimeout #(assign-related-to (clj->js {:interactionId interaction-id})) 2500))))))

(defn pop-search-modal [search-results]
  (.then (js/client.invoke
          "instances.create"
          (clj->js {:location "modal"
                    :url "assets/modal.html"}))
         (fn [modal-context]
           (let [modal-guid (-> modal-context
                                 (aget "instances.create")
                                 (first)
                                 (aget "instanceGuid"))
                 modalClient (js/client.instance modal-guid)]
              (js/setTimeout #(.trigger modalClient "displaySearch" (clj->js search-results)) 2000)
              (js/modalClient.on "assignContact"
                                 (fn [contact]
                                   (ih/publish {:topics "cxengage/zendesk/assign-request"
                                                :response contact})))))))

;; -------------------------------------------------------------------------- ;;
;; Subscription Handlers
;; -------------------------------------------------------------------------- ;;

(defn handle-work-offer [error topic interaction-details]
  (let [interaction (ih/extract-params interaction-details)
        agent-id (get @zendesk-state :zen-user-id)
        {:keys [contact related-to pop-on-accept interactionId]} interaction]
      (add-interaction! interaction)
      (cond
        pop-on-accept (log :info "Pop On Accept - waiting for work-accepted-received")
        contact (js/client.invoke "routeTo" "user" contact)
        related-to (js/client.invoke "routeTo" "ticket" related-to)
        :else (log :info "No screen pop details on work offer"))))

(defn handle-screen-pop [error topic interaction-details]
  (let [result (ih/extract-params interaction-details)
        agent-id (get @zendesk-state :zen-user-id)
        {:keys [pop-type pop-uri new-window size search-type filter filter-type terms interaction-id]} result]
    (cond
      (= pop-type "url") (js/client.request (clj->js {:url (str "/api/v2/channels/voice/agents/" agent-id pop-uri "/display.json")
                                                     :type "POST"}))
      (= pop-type "external-url") (if (= new-window "true")
                                  (js/window.open pop-uri "targetWindow" (str "width=" (:width size) ",height=" (:height size)))
                                  (js/window.open pop-uri))
      (= pop-type "search-pop") (do
                                  (when (= search-type "fuzzy")
                                    (let [all-req-promises (reduce
                                                            (fn [all-promises term]
                                                              (conj all-promises (promise
                                                                                  (fn [resolve reject]
                                                                                    (.then (js/client.request (clj->js {:url (str "/api/v2/search.json?query=" term)}))
                                                                                           (fn [data]
                                                                                             (resolve (:results (js->clj data :keywordize-keys true)))))))))
                                                            []
                                                            terms)
                                          all-req-promises (all all-req-promises)]
                                      (then all-req-promises
                                        (fn [results]
                                          (let [combined-results (reduce
                                                                  (fn [all-results single-result]
                                                                    (conj all-results single-result))
                                                                  []
                                                                  results)
                                                search-results (vec (flatten combined-results))]
                                            (cond
                                              (= (count search-results) 0) (ih/publish {:topics "cxengage/zendesk/search-and-pop-no-results-received"
                                                                                        :response []})
                                              (= (count search-results) 1) (if (= (:result_type (first search-results)) "user")
                                                                            (auto-assign-from-search-pop (first search-results) interaction-id "user")
                                                                            (auto-assign-from-search-pop (first search-results) interaction-id "ticket"))
                                              :else (pop-search-modal search-results)))))))
                                  (when (= search-type "strict")
                                    (let [query (reduce-kv
                                                 (fn [s k v]
                                                   (str s (name k) ":" v " "))
                                                 "/api/v2/search.json?query="
                                                 filter)]
                                       (.then (js/client.request (clj->js {:url query
                                                                           :type "POST"}))
                                              (fn [result]
                                                (let [search-results (:results (ih/extract-params result ))]
                                                  (cond
                                                    (= (count search-results) 0) (ih/publish {:topics "cxengage/zendesk/search-and-pop-no-results-received"
                                                                                              :response []})
                                                    (= (count search-results) 1) (if (= (:result_type (first search-results)) "user")
                                                                                  (auto-assign-from-search-pop (first search-results) interaction-id "user")
                                                                                  (auto-assign-from-search-pop (first search-results) interaction-id "ticket"))
                                                    :else (pop-search-modal search-results)))))))))))

;; -------------------------------------------------------------------------- ;;
;; Zendesk Module
;; -------------------------------------------------------------------------- ;;

(defrecord ZendeskModule []
  pr/SDKModule
  (start [this]
    (go-loop []
      (if (ih/core-ready?)
        (let [module-name :zendesk
              zendesk-integration "https://assets.zendesk.com/apps/sdk/2.0/zaf_sdk.js"]
            (zendesk-init zendesk-integration)
            (ih/subscribe (topics/get-topic :work-offer-received) handle-work-offer)
            (ih/subscribe (topics/get-topic :generic-screen-pop-received) handle-screen-pop)
            (ih/register (clj->js {:api {:zendesk {:focus-interaction focus-interaction
                                                   :set-dimensions set-dimensions
                                                   :set-visibility set-visibility
                                                   :assign-contact assign-contact
                                                   :assign-related-to assign-related-to
                                                   :dump-state dump-state}}
                                   :module-name module-name}))
            (ih/send-core-message {:type :module-registration-status
                                   :status :success
                                   :module-name module-name}))
        (do (a/<! (a/timeout 250))
            (recur)))))
  (stop [this])
  (refresh-integration [this]))
