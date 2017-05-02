(ns cxengage-javascript-sdk.modules.sqs
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.state :as state]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [cljsjs.aws-sdk-js]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-cljs-utils.core :as cxu]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.errors :as e]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]))

(defn handle-response*
  [response<]
  (fn [err data]
    (if err
      (log :error err)
      (go
        (>! response< (->> data
                           js->clj
                           (transform-keys ->kebab-case-keyword)))))))

(defn receive-message*
  [sqs queue-url]
  (let [response (a/promise-chan)]
    (if (state/get-session-expired)
      (do (log :error "Session expired, shutting down SQS")
          (go (a/>! response :shutdown))
          response)
      (let [params (clj->js {:QueueUrl queue-url
                             :MaxNumberOfMessages 1
                             :WaitTimeSeconds 20})]
        (.receiveMessage sqs params (handle-response* response))
        response))))

(defn process-message*
  [{:keys [messages]} delete-message-fn]
  (when (seq messages)
    (let [{:keys [receipt-handle body]} (first messages)
          parsed-body (iu/kebabify (js/JSON.parse body))
          session-id (or (get parsed-body :session-id)
                         (get-in parsed-body [:resource :session-id]))
          current-session-id (state/get-session-id)
          msg-type (or (:notification-type parsed-body)
                       (:type parsed-body))]
      (if (not= (state/get-session-id) session-id)
        (do #_(log :warn (str "Received a message from a different session than the current one."
                              "Current session ID: " current-session-id
                              " - Session ID on message received: " session-id
                              " Message type: " msg-type))
            nil)
        (do (delete-message-fn receipt-handle)
            body)))))

(defn delete-message*
  [sqs {:keys [queue-url]} receipt-handle]
  (let [params (clj->js {:QueueUrl queue-url
                         :ReceiptHandle receipt-handle})]
    (.deleteMessage sqs params (fn [err data]
                                 (when err
                                   (log :error err))))))

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
        sqs (aset js/window "serenova" "cxengage" "internal" "SQS" (AWS.SQS. options))]
    (a/put! done-init< {:module-registration-status :success
                        :module (get @(:state module) :module-name)})
    (go-loop []
      (let [sqs-poller (aget js/window "serenova" "cxengage" "internal" "SQS")
            response< (receive-message* sqs-poller queue-url)
            value (a/<! response<)]
        (if (= value :shutdown)
          nil
          (let [message (process-message* value (partial delete-message* sqs queue-url))]
            (when (not= nil (js/JSON.parse message))
              (on-received (js/JSON.parse message)))
            (recur)))))))

(def initial-state
  {:module-name :sqs
   :urls {:config "tenants/tenant-id/users/resource-id/config"}})

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
          (do (sqs-init* this sqs-integration on-msg-fn core-messages<)
              (ih/register {:module-name module-name})
              (ih/send-core-message {:type :module-registration-status
                                     :status :success
                                     :module-name module-name}))))))
  (stop [this])
  (refresh-integration [this]
    (go-loop []
      (let [sqs-token-ttl (get-in (state/get-integration-by-type "sqs") [:credentials :ttl])
            min-ttl (* sqs-token-ttl 500)] ;;refresh at half the ttl
        (a/<! (a/timeout min-ttl))
        (let [api-url (get-in this [:config :api-url])
              module-state @(:state this)
              resource-id (state/get-active-user-id)
              tenant-id (state/get-active-tenant-id)
              topic (p/get-topic :config-response)
              config-url (str api-url (get-in module-state [:urls :config]))
              config-request {:method :get
                              :url (iu/build-api-url-with-params
                                    config-url
                                    {:tenant-id tenant-id
                                     :resource-id resource-id})}
              config-response (a/<! (iu/api-request config-request))
              {:keys [status api-response]} config-response
              {:keys [result]} api-response
              {:keys [integrations]} result
              sqs-integration (peek (filterv #(= (:type %) "sqs") integrations))]
          (state/update-integration "sqs" sqs-integration)
          (if (not= status 200)
            (p/publish {:topics topic
                        :error (e/token-error "SQS")})
            (let [{:keys [queue credentials]} sqs-integration
                  {:keys [access-key secret-key session-token]} credentials
                  queue-url (:url queue)
                  options (clj->js {:accessKeyId access-key
                                    :secretAccessKey secret-key
                                    :sessionToken session-token
                                    :region "us-east-1"
                                    :params {:QueueUrl queue-url}})
                  sqs (aset js/window "serenova" "cxengage" "internal" "SQS" (AWS.SQS. options))])))
        (recur)))))
