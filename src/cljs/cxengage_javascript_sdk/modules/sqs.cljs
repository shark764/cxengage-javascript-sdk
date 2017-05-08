(ns cxengage-javascript-sdk.modules.sqs
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.state :as state]
            [cljs.core.async :as a]
            [cljsjs.aws-sdk-js]
            [cxengage-javascript-sdk.interaction-management :as int]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.errors :as e]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]))

(defn handle-response*
  [response<]
  (fn [err data]
    (if err
      (js/console.error err)
      (go
        (>! response< (->> data
                           js->clj
                           (transform-keys ->kebab-case-keyword)))))))

(defn receive-message*
  [sqs queue-url]
  (let [response (a/promise-chan)]
    (if (state/get-session-expired)
      (do (js/console.error "Session expired, shutting down SQS")
          (go (a/>! response :shutdown))
          response)
      (let [params (clj->js {:QueueUrl queue-url
                             :MaxNumberOfMessages 1
                             :WaitTimeSeconds 2})]
        (.receiveMessage sqs params (handle-response* response))
        response))))

(defn process-message*
  [response delete-message-fn]
  (let [{:keys [messages]} response]
    (when (seq messages)
      (let [{:keys [receipt-handle body]} (first messages)
            parsed-body (ih/kebabify (js/JSON.parse body))
            session-id (or (get parsed-body :session-id)
                           (get-in parsed-body [:resource :session-id]))
            current-session-id (state/get-session-id)
            msg-type (or (:notification-type parsed-body)
                         (:type parsed-body))]
        (if (not= (state/get-session-id) session-id)
          (do #_(js/console.warn (str "Received a message from a different session than the current one."
                                      "Current session ID: " current-session-id
                                      " - Session ID on message received: " session-id
                                      " Message type: " msg-type))
              nil)
          (do (delete-message-fn receipt-handle)
              body))))))

(defn delete-message*
  [sqs {:keys [queue-url]} receipt-handle]
  (let [params (clj->js {:QueueUrl queue-url
                         :ReceiptHandle receipt-handle})]
    (.deleteMessage sqs params (fn [err data]
                                 (when err
                                   (js/console.error err))))))

(defn sqs-init*
  [module integration on-received done-init<]
  (let [{:keys [queue credentials]} integration
        {:keys [access-key secret-key session-token]} credentials
        queue-url (:url queue)
        options (clj->js {:accessKeyId access-key
                          :secretAccessKey secret-key
                          :sessionToken session-token
                          :region "us-east-1" ;;TODO: get from integration
                          :params {:QueueUrl queue-url}})
        sqs (aset js/window "CxEngage" "internal" "SQS" (AWS.SQS. options))]
    (a/put! done-init< {:module-registration-status :success
                        :module (get @(:state module) :module-name)})))

(def initial-state
  {:module-name :sqs
   :urls {:config "tenants/tenant-id/users/resource-id/config"}})

(defn pull-message [sqs-queue queue-url on-received]
  (go (let [response (receive-message* sqs-queue queue-url)
            value (a/<! response)]
        (let [message (process-message* value (partial delete-message* sqs-queue queue-url))]
          (when (not= nil (js/JSON.parse message))
            (on-received (js/JSON.parse message)))))))

(defn init-sqs []
  (go (js/clearInterval (ih/get-sqs-poller-interval))
      (let [on-received int/sqs-msg-router
            resource-id (state/get-active-user-id)
            tenant-id (state/get-active-tenant-id)
            topic (p/get-topic :config-response)
            config-request {:method :get
                            :url (iu/build-api-url-with-params
                                  (str (state/get-base-api-url) "tenants/tenant-id/users/resource-id/config")
                                  {:tenant-id tenant-id
                                   :resource-id resource-id})}
            {:keys [status api-response]} (a/<! (iu/api-request config-request))
            user-config (:result api-response)]
        (state/set-config! user-config)
        (let [sqs-integration (state/get-integration-by-type "sqs")
              {:keys [access-key secret-key session-token]} (:credentials sqs-integration)
              {:keys [url]} (:queue sqs-integration)
              region "us-east-1" ;; TODO: get from config?
              halfTtl (-> (get-in sqs-integration [:credentials :ttl])
                          (* 1000)
                          (/ 2))
              credentials-obj (clj->js {"accessKeyId" access-key
                                        "secretAccessKey" secret-key
                                        "sessionToken" session-token
                                        "expiryWindow" 15})
              params-obj (clj->js {"QueueUrl" url})
              credentials (AWS.Credentials. credentials-obj)
              queue (AWS.SQS. (clj->js {"credentials" credentials
                                        "region" region
                                        "params" params-obj}))]
          (js/setTimeout init-sqs halfTtl)
          (js/console.info "Starting new interval...")
          (pull-message queue url on-received)
          (ih/set-sqs-poller-interval (js/setInterval (partial pull-message queue url on-received) 3000))))))

(defrecord SQSModule [config state core-messages< on-msg-fn]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [module-name (get @(:state this) :module-name)]
      (let [sqs-integration (state/get-integration-by-type "sqs")]
        (if-not sqs-integration
          (ih/send-core-message {:type :module-registration-status
                                 :status :failure
                                 :module-name module-name})
          (do (init-sqs)
              (ih/register {:module-name module-name})
              (ih/send-core-message {:type :module-registration-status
                                     :status :success
                                     :module-name module-name}))))))
  (stop [this])
  (refresh-integration [this]))
