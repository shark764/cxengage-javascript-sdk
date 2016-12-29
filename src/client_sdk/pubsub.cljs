(ns client-sdk.pubsub
  (:require [cljs.core.async :as a]
            [client-sdk-utils.core :as u]
            [lumbajack.core :refer [log]]
            [clojure.string :as s]
            [client-sdk.state :as state]
            [cljs-uuid-utils.core :as id]
            [client-sdk.api.helpers :as h]
            [client-sdk.api.interactions :as int]))

(def module-state (atom {}))

(def topics [[:authentication [:login-successful
                               :login-failed
                               :config-received]]
             [:interactions [:work-offer-received]]
             [:session [:started
                        :ended
                        :timed-out
                        :state-changed]]])

(def topic-strings
  (let [prefix "cxengage"]
    (->> topics
         (map
          (fn [[module actions]]
            (map
             (fn [action]
               [(str prefix "/" (name module) "/" (name action))
                (str prefix "/" (name module))])
             actions)))
         (flatten)
         (into #{prefix}))))

(def subscriptions
  (atom
   (reduce
    #(assoc %1 %2 {})
    {}
    topic-strings)))

(defn get-topic-permutations [topic]
  (let [parts (s/split topic #"/")]
    (-> (reduce (fn [{:keys [x permutations]} part]
                  (let [permutation (s/join "/" (take x parts))]
                    {:x (inc x) :permutations (conj permutations permutation)}))
                {:x 1 :permutations []}
                parts)
        (:permutations))))

(defn valid-topic? [topic]
  (contains? topic-strings topic))

(defn subscribe [topic handler]
  (if-not (valid-topic? topic)
    (log :error "That is not a valid subscription topic.")
    (let [subscription-id (id/make-random-uuid)]
      (swap! subscriptions assoc-in [topic subscription-id] handler)
      nil)))

(defn publish! [topic message]
  (if-not (valid-topic? topic)
    (log :error "That is not a valid subscription topic.")
    (let [all-topics (get-topic-permutations topic)]
      (doseq [topic all-topics]
        (if-let [subscribers (vals (get @subscriptions topic))]
          (doseq [subscription-handler subscribers]
            (subscription-handler (h/format-response message)))
          (log :warn (str "No subscribers found for topic `" topic "`, sending to no one.")))))))

(defn handle-work-offer [message]
  (state/add-interaction! :pending message)
  (publish! "cxengage/interactions/work-offer-received" message))

(defn handle-work-initiated [msg]
  (log :warn "Handling work initiated"))

(defn handle-work-rejected [message]
  (log :warn "Handling work rejected")
  (state/remove-interaction! :pending message))

(defn handle-custom-fields [message]
  (log :warn "Handling custom fields")
  (state/remove-interaction! :pending message))

(defn handle-disposition-codes [message]
  (log :warn "Handling disposition codes")
  (state/remove-interaction! :pending message))

(defn msg-router [message]
  (let [handling-fn (case (:msg-type message)
                      :INTERACTIONS/WORK_OFFER_RECEIVED handle-work-offer
                      :INTERACTIONS/WORK_REJECTED_RECEIVED handle-work-rejected
                      :INTERACTIONS/WORK_INITIATED_RECEIVED handle-work-initiated
                      :INTERACTIONS/CUSTOM_FIELDS_RECEIVED handle-custom-fields
                      :INTERACTIONS/DISPOSITION_CODES_RECEIVED handle-disposition-codes
                      nil)]
    (when (and (get message :actionId)
               (not= (get message :interactionId) "00000000-0000-0000-0000-000000000000"))
      (let [ack-msg (-> message (select-keys [:actionId :subId :resourceId :tenantId :interactionId]))]
        (int/acknowledge-flow-action-handler (state/get-module-chan :interactions) (a/promise-chan) ack-msg)))
    (if handling-fn
      (handling-fn message)
      (log :debug (str "Temporarily ignoring msg 'cuz handler isnt written yet: " (:msg-type message))))))

(defn infer-notification-type [message]
  (let [{:keys [notificationType]} message]
    (if-let [inferred-notification-type (case notificationType
                                          "work-rejected" (merge {:msg-type :INTERACTIONS/WORK_REJECTED_RECEIVED} message)
                                          "work-initiated" (merge {:msg-type :INTERACTIONS/WORK_INITIATED_RECEIVED} message)
                                          "disposition-codes" (merge {:msg-type :INTERACTIONS/DISPOSITION_CODES_RECEIVED} message)
                                          "custom-fields" (merge {:msg-type :INTERACTIONS/CUSTOM_FIELDS_RECEIVED} message)
                                          "interaction-timeout" (merge {:msg-type :INTERACTIONS/INTERACTION_TIMEOUT} message)
                                          nil)]
      inferred-notification-type
      (log :warn "Unable to infer agent notification msg type - no matching clause" message))))

(defn sqs-msg-router [message]
  (let [cljsd-msg (js->clj (js/JSON.parse message) :keywordize-keys true)
        inferred-msg (case (:type cljsd-msg)
                       "resource-state-change" (merge {:msg-type :SESSION/CHANGE_STATE_RESPONSE} cljsd-msg)
                       "work-offer" (merge {:msg-type :INTERACTIONS/WORK_OFFER_RECEIVED} cljsd-msg)
                       "agent-notification" (infer-notification-type cljsd-msg)
                       nil)]
    (if inferred-msg
      (msg-router inferred-msg)
      (log :error "Unable to infer msg type from sqs"))))

(defn mqtt-msg-router [message]
  (log :debug "message in mqtt msg router" message))

(defn module-router [message]
  (if (:msg-type message)
    (msg-router message)
    (sqs-msg-router message)))

(defn api []
  {:subscribe subscribe})

(defn init [env]
  (swap! module-state assoc :env env)
  (let [module-inputs< (a/chan 1024)]
    (u/start-simple-consumer! module-inputs< module-router)
    module-inputs<))
