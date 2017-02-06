(ns cxengage-javascript-sdk.modules.sqs
  (:require-macros [cljs.core.async.macros :refer [go-loop go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljsjs.aws-sdk-js]
            [cxengage-cljs-utils.core :as u]
            [cxengage-javascript-sdk.state :as state]
            [cljs.core.async :as a]
            [camel-snake-kebab.core :refer [->kebab-case-keyword]]
            [camel-snake-kebab.extras :refer [transform-keys]]))

(def module-state (atom {}))

(defn ^:private handle-response
  [response<]
  (fn [err data]
    (if err
      (log :error err)
      (go
        (>! response< (->> data
                           js->clj
                           (transform-keys ->kebab-case-keyword)))))))

(defn ^:private receive-message
  [sqs queue-url]
  (let [params (clj->js {:QueueUrl queue-url
                         :MaxNumberOfMessages 1
                         :WaitTimeSeconds 20})
        response (a/promise-chan)]
    (.receiveMessage sqs params (handle-response response))
    response))

(defn ^:private process-message
  [{:keys [messages]} delete-message-fn]
  (when (seq messages)
    (let [{:keys [receipt-handle body]} (first messages)]
      (delete-message-fn receipt-handle)
      body)))

(defn ^:private delete-message
  [sqs {:keys [queue-url]} receipt-handle]
  (let [params (clj->js {:QueueUrl queue-url
                         :ReceiptHandle receipt-handle})]
    (.deleteMessage sqs params (fn [err data]
                                 (when err
                                   (log :error err))))))

(defn ^:private sqs-init
  [config on-received done-init<]
  (let [{:keys [queue credentials]} config
        {:keys [accessKey secretKey sessionToken]} credentials
        queue-url (:url queue)
        options (clj->js {:accessKeyId accessKey
                          :secretAccessKey secretKey
                          :sessionToken sessionToken
                          :region "us-east-1" ;;TODO: get from config
                          :params {:QueueUrl queue-url}})
        sqs (AWS.SQS. options)
        shutdown< (a/chan)]
    (a/put! done-init< {:status :ok})
    (go-loop []
      (let [response< (receive-message sqs queue-url)
            [v c] (alts! [response< shutdown<])]
        (if-not (= v :shutdown)
          (let [message (process-message v (partial delete-message sqs queue-url))]
            (when (not= nil (js/JSON.parse message))
              (on-received (js/JSON.parse message)))
            (recur))
          (log :info "Shut down SQS"))))
    shutdown<))

(defn module-router [message]
  (let [handling-fn (case (:type message)
                      :SQS nil
                      nil)]
    (if handling-fn
      (handling-fn message)
      (log :error "No appropriate handler found in SQS SDK module." (:type message)))))

(defn module-shutdown-handler [message]
  (log :info "Received shutdown message from Core - SQS Module shutting down...."))

(defn init
  [env done-init< config on-msg-fn]
  (log :debug "Initializing SDK module: SQS")
  (swap! module-state assoc :env env)
  (let [module-inputs< (a/chan 1024)
        module-shutdown< (a/chan 1024)
        sqs-config (first (filter #(= (:type %) "sqs") (:integrations config)))]
    (if-not sqs-config
      (a/put! done-init< {:status :failure})
      (do (u/start-simple-consumer! module-inputs< module-router)
          (u/start-simple-consumer! module-shutdown< module-shutdown-handler)
          (sqs-init sqs-config on-msg-fn done-init<)
          {:messages module-inputs<
           :shutdown module-shutdown<}))))
