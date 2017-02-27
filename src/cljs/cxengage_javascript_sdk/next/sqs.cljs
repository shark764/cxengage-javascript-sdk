(ns cxengage-javascript-sdk.next.sqs
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cxengage-javascript-sdk.next.protocols :as pr]
            [cxengage-javascript-sdk.state :as state]
            [cljs.core.async :as a]
            [cljsjs.aws-sdk-js]
            [cxengage-cljs-utils.core :as u]
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
  (let [params (clj->js {:QueueUrl queue-url
                         :MaxNumberOfMessages 1
                         :WaitTimeSeconds 20})
        response (a/promise-chan)]
    (.receiveMessage sqs params (handle-response* response))
    response))

(defn process-message*
  [{:keys [messages]} delete-message-fn]
  (when (seq messages)
    (let [{:keys [receipt-handle body]} (first messages)]
      (delete-message-fn receipt-handle)
      body)))

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
        sqs (AWS.SQS. options)
        shutdown< (a/chan)]
    (a/put! done-init< {:module-registration-status :success
                        :module (get @(:state module) :module-name)})
    (go-loop []
      (let [response< (receive-message* sqs queue-url)
            [v c] (alts! [response< shutdown<])]
        (if-not (= v :shutdown)
          (let [message (process-message* v (partial delete-message* sqs queue-url))]
            (when (not= nil (js/JSON.parse message))
              (on-received (js/JSON.parse message)))
            (recur)))))))

(def initial-state
  {:module-name :sqs
   :urls {}})

(defrecord SQSModule [config state core-messages< on-msg-fn]
  pr/SDKModule
  (start [this]
    (reset! (:state this) initial-state)
    (let [register (aget js/window "serenova" "cxengage" "modules" "register")
          module-name (get @(:state this) :module-name)]
      (let [sqs-integration (state/get-integration-by-type "sqs")]
        (if-not sqs-integration
          (a/put! core-messages< {:module-registration-status :failure :module module-name})
          (do (sqs-init* this sqs-integration on-msg-fn core-messages<)
              (js/console.info "<----- Started " module-name " module! ----->")
              (register {:module-name module-name}))))))
  (stop [this]))
