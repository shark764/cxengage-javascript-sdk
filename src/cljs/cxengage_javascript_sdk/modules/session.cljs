(ns cxengage-javascript-sdk.modules.session
  (:require-macros [cljs-sdk-utils.macros :refer [def-sdk-fn]]
                   [lumbajack.macros :refer [log]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.spec.alpha :as s]
            [cljs.core.async :as a]
            [cljs-sdk-utils.protocols :as pr]
            [cljs-sdk-utils.errors :as e]
            [cljs-sdk-utils.topics :as topics]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cljs-sdk-utils.interop-helpers :as ih]
            [cljs-sdk-utils.specs :as specs]))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.session.goNotReady({
;;   reasonInfo: {
;;     reason: "{{string}}",
;;     reasonId: "{{uuid}}",
;;     reasonListId: "{{uuid}}"
;;   }
;; });
;; -------------------------------------------------------------------------- ;;

(s/def ::go-not-ready-spec
  (s/keys :req-un []
          :opt-un [::specs/callback ::specs/reason-info]))

(def-sdk-fn go-not-ready
  {:validation ::go-not-ready-spec
   :topic-key :presence-state-change-request-acknowledged}
  [params]
  (let [{:keys [callback topic reason-info]} params
        {:keys [reason reason-id reason-list-id]} reason-info
        session-id (state/get-session-id)]
    (if (and
         (or reason reason-id reason-list-id)
         (not (state/valid-reason-codes? reason reason-id reason-list-id)))
      (p/publish {:topics topic
                  :error (e/invalid-reason-info-err (state/get-all-reason-lists))
                  :callback callback})
      (let [change-state-body (cond-> {:session-id session-id
                                       :state "notready"}
                                reason-info (assoc :reason reason
                                                   :reason-id reason-id
                                                   :reason-list-id reason-list-id))
            resp (a/<! (rest/change-state-request change-state-body))
            {:keys [status api-response]} resp
            new-state-data (:result api-response)]
        (if (= status 200)
          (p/publish {:topics topic
                      :response new-state-data
                      :callback callback})
          (p/publish {:topics topic
                      :error (e/failed-to-change-state-err resp)
                      :callback callback}))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.session.setActiveTenant({ tenantId: "{{uuid}}" });
;; -------------------------------------------------------------------------- ;;

(defn- start-heartbeats* []
  (log :debug "Sending heartbeats...")
  (go-loop []
    (if (= "offline" (state/get-user-session-state))
      (do (log :info "Session is now offline; ceasing future heartbeats.")
          (state/set-session-expired! true)
          nil)
      (let [topic (topics/get-topic :presence-heartbeats-response)
            resp (a/<! (rest/heartbeat-request))
            {:keys [api-response status]} resp
            {:keys [result]} api-response
            next-heartbeat-delay (* 1000 (or (:heartbeatDelay result) 30))]
        (if (= status 200)
          (do (log :debug "Heartbeat sent!")
              (p/publish {:topics topic
                          :response result})
              (a/<! (a/timeout next-heartbeat-delay))
              (recur))
          (do (log :error "Heartbeat failed; ceasing future heartbeats.")
              (state/set-session-expired! true)
              (p/publish {:topics topic
                          :error (e/session-heartbeats-failed-err resp)})
              nil))))))

(defn- start-session* []
  (go (let [resp (a/<! (rest/start-session-request))
            {:keys [status api-response]} resp
            topic (topics/get-topic :session-started)
            session-details (assoc (:result api-response) :resource-id (state/get-active-user-id))]
        (if (= status 200)
          (do (state/set-session-details! (assoc session-details :expired? false))
              (p/publish {:topics topic
                          :response session-details})
              (go-not-ready)
              (state/set-session-expired! false)
              (start-heartbeats*))
          (p/publish {:topics topic
                      :error (e/failed-to-start-agent-session-err resp)}))
        nil)))

(defn- get-config* []
  (go (let [topic (topics/get-topic :config-response)
            config-response (a/<! (rest/get-config-request))
            {:keys [api-response status]} config-response
            user-config (:result api-response)
            extensions (get user-config :extensions)]
        (if (= status 200)
          (do (state/set-config! user-config)
              (ih/send-core-message {:type :config-ready})
              (p/publish {:topics topic
                          :response user-config})
              (p/publish {:topics (topics/get-topic :extension-list)
                          :response (select-keys user-config [:active-extension :extensions])})
              (start-session*))
          (p/publish {:topics topic
                      :error (e/failed-to-get-session-config-err config-response)}))))
  nil)

(s/def ::set-active-tenant-spec
  (s/keys :req-un [::specs/tenant-id]
          :opt-un [::specs/callback]))

(def-sdk-fn set-active-tenant
  {:validation ::set-active-tenant-spec
   :topic-key :active-tenant-set}
  [params]
  (let [{:keys [callback topic tenant-id]} params
        tenant-permissions (state/get-tenant-permissions tenant-id)
        {:keys [status api-response] :as resp} (a/<! (rest/get-tenant-request tenant-id))
        region-id (get-in api-response [:result :region-id])]
    (if (not= status 200)
      (p/publish {:topics topic
                  :error (e/failed-to-get-tenant-err api-response)})
      (do
        (state/set-tenant-data! (:result api-response))
        (state/set-active-tenant! tenant-id)
        (let [{:keys [status api-response] :as resp} (a/<! (rest/get-region-request region-id))]
          (if (not= status 200)
            (p/publish {:topics topic
                        :error (e/failed-to-get-region-err api-response)})
            (do
              (state/set-region! (get-in api-response [:result :name]))
              (p/publish {:topics topic
                          :response {:tenant-id tenant-id}
                          :callback callback})
              (get-config*))))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.session.setDirection({ direction: "{{inbound/outbound}}" });
;; -------------------------------------------------------------------------- ;;

(s/def ::set-direction-spec
  (s/keys :req-un [::specs/direction]
          :opt-un [::specs/callback]))

(def-sdk-fn set-direction
  {:validation ::set-direction-spec
   :topic-key :set-direction-response}
  [params]
  (let [{:keys [callback topic direction]} params
        resp (a/<! (rest/set-direction-request direction))
        {:keys [status api-response]} resp
        direction-details {:direction direction
                           :session-id (get-in api-response [:result :session-id])}]
    (if (= status 200)
      (p/publish {:topics topic
                  :response direction-details
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-set-direction-err resp)
                  :callback callback}))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.session.goReady({ extensionValue: "{{uuid/extension}}" });
;; -------------------------------------------------------------------------- ;;

(defn- go-ready* [topic callback]
  (go (let [session-id (state/get-session-id)
            resp (a/<! (rest/change-state-request {:session-id session-id
                                                   :state "ready"}))
            {:keys [status api-response]} resp
            new-state-data (:result api-response)]
        (if (= status 200)
          (p/publish {:topics topic
                      :response new-state-data
                      :callback callback})
          (p/publish {:topics topic
                      :error (e/failed-to-change-state-err resp)
                      :callback callback})))))

(s/def ::go-ready-spec
  (s/keys :req-un [::specs/extension-value]
          :opt-un [::specs/callback]))

(def-sdk-fn go-ready
  {:validation ::go-ready-spec
   :topic-key :presence-state-change-request-acknowledged}
  [params]
  (let [{:keys [callback topic extension-value]} params
        resp (a/<! (rest/get-user-request))
        {:keys [status api-response]} resp
        {:keys [result]} api-response
        extensions (get-in api-response [:result :extensions])]
    (if-not (= status 200)
      (p/publish {:topics topic
                  :error (e/failed-to-get-user-extensions-err resp)
                  :callback callback})
      (do (state/set-extensions! extensions)
          (let [new-extension (state/get-extension-by-value extension-value)
                active-extension (state/get-active-extension)]
            (if-not new-extension
              (p/publish {:topics topic
                          :error (e/invalid-extension-provided-err (state/get-all-extensions))
                          :callback callback})
              (if-not (= active-extension new-extension)
                ;; Active extension was either nil (this user has never had an
                ;; active extension, E.G. they're a new user), or they *do* have an
                ;; active extension, but it didn't match the one they passed for the
                ;; session they're starting. Update their user prior to changing
                ;; state, so they go ready with the correct extension.
                (let [resp (a/<! (rest/update-user-request {:activeExtension new-extension}))
                      {:keys [status api-response]} resp]
                  (if-not (= status 200)
                    (p/publish {:topics topic
                                :error (e/failed-to-update-extension-err resp)
                                :callback callback}))
                  (let [resp (a/<! (rest/get-config-request))
                        {:keys [status api-response]} resp
                        user-config (:result api-response)]
                    (if (not (= status 200))
                      (p/publish {:topics topic
                                  :error (e/failed-to-get-session-config-err resp)
                                  :callback callback})
                      (do (state/set-config! user-config)
                          (p/publish {:topics (topics/get-topic :extension-list)
                                      :response (select-keys user-config [:active-extension :extensions])})
                          (ih/send-core-message {:type :config-ready})
                          (p/publish {:topics (topics/get-topic :config-response)
                                      :response user-config
                                      :callback callback})
                          (go-ready* topic callback)))))

                ;; Their active extension and the extension they passed in are the
                ;; same, no user update request is necessary, simply go ready.
                (go-ready* topic callback))))))))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.session.getActiveUserId();
;; -------------------------------------------------------------------------- ;;

(defn get-active-user-id [& params]
  (let [callback (first params)
        callback (if (fn? callback) callback nil)
        user-id (state/get-active-user-id)]
    (p/publish {:topic (topics/get-topic :get-active-user-id-response)
                :response user-id
                :callback callback})
    user-id))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.session.getActiveTenantId();
;; -------------------------------------------------------------------------- ;;

(defn get-active-tenant-id [& params]
  (let [callback (first params)
        callback (if (fn? callback) callback nil)
        tenant-id (state/get-active-tenant-id)]
    (p/publish {:topic (topics/get-topic :get-active-tenant-id-response)
                :response tenant-id
                :callback callback})
    tenant-id))

;; -------------------------------------------------------------------------- ;;
;; CxEngage.session.getToken();
;; -------------------------------------------------------------------------- ;;

(defn get-token [& params]
  (let [callback (first params)
        callback (if (fn? callback) callback nil)
        token (state/get-token)]
    (p/publish {:topic (topics/get-topic :get-token-response)
                :response token
                :callback callback})
    token))

;; -------------------------------------------------------------------------- ;;
;; SDK Presence Session Module
;; -------------------------------------------------------------------------- ;;

(defrecord SessionModule []
  pr/SDKModule
  (start [this]
    (let [module-name :session]
      (ih/register {:api {module-name {:set-active-tenant set-active-tenant
                                       :go-ready go-ready
                                       :go-not-ready go-not-ready
                                       :set-direction set-direction
                                       :get-active-user-id get-active-user-id
                                       :get-active-tenant-id get-active-tenant-id
                                       :get-token get-token}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))
