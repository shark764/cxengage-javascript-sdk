(ns client-sdk.sqs
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require cljsjs.aws-sdk-js
            [lumbajack.core :refer [log]]
            [client-sdk-utils.core :as u]
            [client-sdk.state :as state]
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
      (log :debug "Received message" body)
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
    (go-loop []
      (let [response< (receive-message sqs queue-url)
            [v c] (alts! [response< shutdown<])]
        (if-not (= v :shutdown)
          (let [message (process-message v (partial delete-message sqs queue-url))]
            (when message (on-received message))
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

(defn init
  [env done-init< config]
  (swap! module-state assoc :env env)
  (let [module-inputs< (a/chan 1024)
        sqs-config (first (filter #(= (:type %) "sqs") (:integrations config)))]
    (u/start-simple-consumer! module-inputs< module-router)
    (sqs-init sqs-config #(log :debug %) done-init<)
    module-inputs<))
