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
  (let [{:keys [interactionId]} interaction]
    (swap! zendesk-state assoc-in [:interactions interactionId] interaction)))

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
        interaction (get-in @zendesk-state [:interactions interaction-id])
        agent-id (get @zendesk-state :zen-user-id)
        {:keys [contact relatedTo]} interaction]
      (when contact
        (try
          (js/client.request (clj->js {:url (str "/api/v2/channels/voice/agents/" agent-id "/users/" contact "/display.json")
                                       :type "POST"}))
          (catch js/Object e
            (ih/publish {:topics topic
                         :error (error/failed-to-focus-zendesk-interaction-err interaction-id e)
                         :callback callback}))))
      (when relatedTo
        (try
          (js/client.request (clj->js {:url (str "/api/v2/channels/voice/agents/" agent-id "/users/" relatedTo "/display.json")
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
                related-to (get @zendesk-state :active-tab)
                interrupt-body {:external-crm-user (get @zendesk-state :zen-user-id)
                                :external-crm-name "zendesk"
                                :external-crm-related-to related-to
                                :external-crm-related-to-uri related-to}
                {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (ih/publish {:topics topic
                   :response (merge {:interaction-id interaction-id} interrupt-body)
                   :callback callback})
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
                contact (get @zendesk-state :active-tab)
                interrupt-body {:external-crm-user (get @zendesk-state :zen-user-id)
                                :external-crm-name "zendesk"
                                :external-crm-contact contact
                                :external-crm-contact-uri contact}
                {:keys [status] :as interrupt-response} (a/<! (rest/send-interrupt-request interaction-id interrupt-type interrupt-body))]
    (if (= status 200)
      (ih/publish {:topics topic
                   :response (merge {:interaction-id interaction-id} interrupt-body)
                   :callback callback})
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
                (swap! zendesk-state assoc :zen-user-id (get-in
                                                          (ih/extract-params context)
                                                          [:currentUser :id]))
                (js/client.on "assignUser" (fn [user]
                                            (ih/publish {:topics "cxengage/zendesk/assign-request"
                                                         :response user})))
                (js/client.on "activeTab" (fn [tab-data]
                                            (swap! zendesk-state assoc :active-tab (ih/extract-params tab-data))
                                            (ih/publish {:topics "cxengage/zendesk/active-tab-changed"
                                                         :response tab-data})))
                (ih/publish {:topics "cxengage/zendesk/zendesk-initialization"
                             :response true}))))
          (catch js/Object e
            (ih/publish {:topics "cxengage/zendesk/zendesk-initialization"
                         :error (error/failed-to-init-zendesk-client-err e)})))))))
        (do (a/<! (a/timeout 250))
            (recur))))))

(defn dump-state []
  (js/console.log (clj->js @zendesk-state)))

;; -------------------------------------------------------------------------- ;;
;; Helper Functions
;; -------------------------------------------------------------------------- ;;

(defn auto-assign-from-search-pop [response interaction-id]
  (let [result (:result (ih/extract-params response))
        parsed-result (ih/extract-params (js/JSON.parse result))]
      (if (= 1 (count (vals parsed-result)))
        (assign-contact (clj->js {:interaction interaction-id}))
        (log :info "More than one result - skipping auto-assign"))))

(defn pop-search-modal [search-results]
  (.then (js/client.invoke
          "instances.create"
          (clj->js {:location "modal"
                    :url "assets/modal.html"}))
         (fn [modal-context]
           (let [modal-client (-> modal-context
                                  (aget "instances.create")
                                  (first)
                                  (aget "instanceGuid")
                                  (js/client.instance))]
              (js/modal-client.on "assignContact"
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
  (let [result (ih/extract-params interaction-details )
        agent-id (get @zendesk-state :zen-user-id)
        {:keys [popType popUrl newWindow size searchType filter filterType terms interactionId]} result]
    (cond
      (= popType "internal") (js/client.request (clj->js {:url (str "/api/v2/channels/voice/agents/" agent-id popUrl "/display.json")
                                                          :type "POST"}))
      (= popType "external") (if (= newWindow "true")
                              (js/window.open popUrl "targetWindow" (str "width=" (:width size) ",height=" (:height size)))
                              (js/window.open popUrl))
      (= popType "search") (do
                              (when (= searchType "strict")
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
                                                (= (count search-results) 1) (auto-assign-from-search-pop search-results interactionId)
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
            (ih/register (clj->js {:api {:zendesk {:focus-interaction focus-interaction
                                                   :set-dimensions set-dimensions
                                                   :set-visibility set-visibility
                                                   :assign-contact assign-contact
                                                   :assign-related-to assign-related-to}}
                                   :module-name module-name}))
            (ih/send-core-message {:type :module-registration-status
                                   :status :success
                                   :module-name module-name}))))
        (do (a/<! (a/timeout 250))
            (recur)))
  (stop [this])
  (refresh-integration [this]))
