(ns cxengage-javascript-sdk.next-modules.sqs
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.state :as state]
            [cljs.core.async :as a]
            [cljsjs.aws-sdk-js]
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
      (do (js/console.log :error "Session expired, shutting down SQS")
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
          parsed-body (ih/kebabify (js/JSON.parse body))
          session-id (or (get parsed-body :session-id)
                         (get-in parsed-body [:resource :session-id]))
          current-session-id (state/get-session-id)
          msg-type (or (:notification-type parsed-body)
                       (:type parsed-body))]
      (if (not= (state/get-session-id) session-id)
        (do #_(js/console.log :warn (str "Received a message from a different session than the current one."
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
                                   (js/console.log :error err))))))

(defn sqs-init*
  [integration on-received]
  (let [{:keys [queue credentials]} integration
        {:keys [access-key secret-key session-token ttl]} credentials
        most-of-ttl (-> ttl
                        (* 1000)
                        (* 0.75))
        original-sqs-queue-url (:url queue)
        options (clj->js {:accessKeyId access-key
                          :secretAccessKey secret-key
                          :sessionToken session-token
                          :region "us-east-1" ;;TODO: get from integration
                          :params {:QueueUrl original-sqs-queue-url}})
        original-sqs-queue (AWS.SQS. options)
        original-sqs-needs-refresh-time (+ (.getTime (js/Date.)) most-of-ttl)]
    (ih/send-core-message {:type :module-registration-status
                           :status :success
                           :module-name "sqs"})
    ;; Start the loop to poll SQS, initially with the first SQS Queue object + url that we instantiated
    (go-loop [sqs-queue original-sqs-queue
              sqs-queue-url original-sqs-queue-url
              sqs-needs-refresh-time original-sqs-needs-refresh-time]
      (if (> (.getTime (js/Date.)) sqs-needs-refresh-time)
        ;; 1/2 of the TTL has passed using this SQS Queue object, we need to create a new one for subsequent SQS polls
        (do (js/console.info "Refreshing SQS integration")
            (let [tenant-id (state/get-active-tenant-id)
                  resource-id (state/get-active-user-id)
                  config-request {:method :get
                                  :url (iu/build-api-url-with-params
                                        (str (state/get-base-api-url) "tenants/tenant-id/users/resource-id/config")
                                        {:tenant-id tenant-id
                                         :resource-id resource-id})}
                  {:keys [status api-response]} (a/<! (iu/api-request config-request))
                  user-config (:result api-response)]
              (if (not= status 200)
                (p/publish {:topics (p/get-topic :failed-to-refresh-sqs-integration)
                            :error (e/failed-to-refresh-sqs-integration-err)})
                (do
                  (state/set-config! user-config)
                  (let [sqs-integration (state/get-integration-by-type "sqs")]
                    (if-not sqs-integration
                      (p/publish {:topics (p/get-topic :failed-to-refresh-sqs-integration)
                                  :error (e/failed-to-refresh-sqs-integration-err)})
                      (let [{:keys [access-key secret-key session-token ttl]} (:credentials sqs-integration)
                            {:keys [url]} (:queue sqs-integration)
                            new-sqs-queue-url url
                            new-region "us-east-1" ;; TODO: get from integration
                            new-most-of-ttl (-> ttl
                                                (* 1000)
                                                (* 0.75))
                            new-sqs-needs-refresh-time (+ (.getTime (js/Date.)) new-most-of-ttl)
                            new-options (clj->js {:accessKeyId access-key
                                                  :secretAccessKey secret-key
                                                  :sessionToken session-token
                                                  :region "us-east-1" ;;TODO: get from integration
                                                  :params {:QueueUrl new-sqs-queue-url}})
                            new-sqs-queue (AWS.SQS. new-options)
                            response< (receive-message* new-sqs-queue new-sqs-queue-url)
                            value (a/<! response<)]
                        (if (= value :shutdown)
                          nil
                          (let [message (process-message* value (partial delete-message* sqs-queue sqs-queue-url))]
                            (when-let [msg (js/JSON.parse message)]
                              (on-received msg))
                            (recur new-sqs-queue
                                   new-sqs-queue-url
                                   new-sqs-needs-refresh-time))))))))))
        ;; It is still less than 1/2 of the TTL for the previously instantiated SQS Queue object, continue using the same one
        (let [response< (receive-message* sqs-queue sqs-queue-url)
              value (a/<! response<)]
          (if (= value :shutdown)
            nil
            (let [message (process-message* value (partial delete-message* sqs-queue sqs-queue-url))]
              (when-let [msg (js/JSON.parse message)]
                (on-received msg))
              (recur sqs-queue
                     sqs-queue-url
                     sqs-needs-refresh-time))))))))

;; -------------------------------------------------------------------------- ;;
;; SDK SQS Module
;; -------------------------------------------------------------------------- ;;

(defrecord SQSModule [on-msg-fn]
  pr/SDKModule
  (start [this]
    (let [module-name :sqs]
      (let [sqs-integration (state/get-integration-by-type "sqs")]
        (if-not sqs-integration
          (ih/send-core-message {:type :module-registration-status
                                 :status :failure
                                 :module-name module-name})
          (do (sqs-init* sqs-integration on-msg-fn)
              (ih/register {:module-name module-name}))))))
  (stop [this])
  (refresh-integration [this]))
