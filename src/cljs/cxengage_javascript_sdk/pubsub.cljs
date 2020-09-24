(ns cxengage-javascript-sdk.pubsub
  (:require-macros [cxengage-javascript-sdk.domain.macros :refer [log]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :as a]
            [cljs.spec.alpha :as s]
            [clojure.string :as string]
            [expound.alpha :as e]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cljs-uuid-utils.core :as uuid]
            [cxengage-javascript-sdk.domain.errors :as error]
            [cxengage-javascript-sdk.state :as state]))

(def sdk-subscriptions (atom {}))

(defn destroy-subscriptions []
  (reset! sdk-subscriptions {}))

(defn get-topic-permutations
  "Given an SDK consumer topic string, returns a list of all possible permutatations of that topic string. I.E. 'cxengage/authentication' returns 'cxengage' & 'cxengage/authentication'."
  [topic]
  (let [parts (string/split topic #"/")]
    (:permutations 
     (reduce
      (fn [{:keys [x permutations]} part]
        (let [permutation (string/join "/" (take x parts))]
          {:x (inc x), :permutations (conj permutations permutation)}))
      {:x 1, :permutations []}
      parts))))

(defn all-topics []
   (reduce (fn [acc v] (into acc (get-topic-permutations v)))
           #{}
           (vals topics/sdk-topics)))      

(defn valid-topic?
  "Determines if a topic exists in the list of valid SDK topics."
  [topic]
  (let [valid-topics (all-topics)]
    (if (some #(= topic %) valid-topics)
      topic)))

(s/def ::subscribe-params
  (s/keys :req-un [::specs/topic ::specs/callback]))

(defn subscribe
  "Adds a subscription callback associated with a specific topic. Any time that topic is published to all subscription callbacks for that topic will be fired. Returns a subscription ID which can be later unsubscribed with."
  [topic callback]
  (let [params {:topic topic :callback callback}]
    (if-not (s/valid? ::subscribe-params params)
      (s/explain-data ::subscribe-params params)
      (let [subscription-id (str (id/make-random-uuid))]
        (if-not (valid-topic? topic)
          (log :error (str "(" topic ") is not a valid subscription topic."))
          (do (swap! sdk-subscriptions assoc-in [topic subscription-id] callback)
              subscription-id))))))

(s/def ::unsubscribe-params
  (s/keys :req-un [::specs/subscription-id]))

(defn unsubscribe
  "Removes a subscription callback from the SDK subscribers list."
  [subscription-id]
  (let [original-subs @sdk-subscriptions
        new-sub-list (reduce-kv
                      (fn [updated-subscriptions topic subscribers]
                        (assoc updated-subscriptions
                               topic
                               (dissoc subscribers subscription-id)))
                      {}
                      @sdk-subscriptions)]
    (reset! sdk-subscriptions new-sub-list)
    (if (= new-sub-list original-subs)
      (log :error "Subscription ID not found")
      (do (log :info "Successfully unsubscribed")
          true))))

(defn get-subscribers-by-topic
  "Given a topic, finds all subscriber callbacks for any topics that match the topic provided."
  [topic]
  (let [all-topics (keys @sdk-subscriptions)]
    (when-let [matched-topic (first (filter #(= topic %) all-topics))]
      (get @sdk-subscriptions matched-topic))))


(defn format-request-logs
  [log]
  (let [{:keys [log-level level data code message]} log
        date-time (js/Date.)
        error-message "Exception when trying to format request logs; unable to format request logs for publishing. Likely passing a circularly dependent javascript object to a CxEngage logging statement."
        log-level (if log-level
                      log-level
                      (if (or (= "session-fatal" level) (= "interaction-fatal"))
                          "error"
                          level))]
    (assoc {}
          :level log-level
          :message (js/JSON.stringify (clj->js {:data (try
                                                        (js/JSON.stringify (clj->js data))
                                                        (catch js/Object e
                                                          (log :error error-message)
                                                          {:error error-message}))
                                                :original-client-log-level (name level)
                                                :code code 
                                                :message message}))
          :timestamp (.toISOString date-time))))

(defn save-logs
  [error]
  (go (let [log (format-request-logs error)
            logs-body {:logs [log]
                       :context {:client-url js/window.location.href}
                       :device {:user-agent js/window.navigator.userAgent}
                       :app-id (str (uuid/make-random-squuid))
                       :app-name "CxEngage SDK"}
            {:keys [status api-response] :as log-response} (a/<! (rest/save-logs-request logs-body))]
        (if (= status 200)
          (log :info "Successfully logged to CxEngage.")
          (do
            (log :warn "Unable to upload logs to CxEngage.")
            (publish {:topics (topics/get-topic :logs-saved)
                      :error (error/failed-to-save-logs-err
                              (update-in logs-body [:logs 0 :message]
                                (fn [message] (js->clj (js/JSON.parse message) {:keywordize-keys true})))
                              log-response)}))))))


(defn publish
  "Publishes a value (or error) to a specific topic, optionally calling the callback provided and optionally leaving the casing of the response unaltered."
  [publish-details]
  (let [{:keys [topics response error callback preserve-casing?]} (ih/extract-params publish-details true)
        error (ih/camelify error)
        response (if preserve-casing?
                      (clj->js response)
                      (ih/camelify response))
        topic-permutations (get-topic-permutations topics)

        ; relevant-subscribers is a list of maps: 1 map per topic (keys=subscription ids, values=callbacks)
        relevant-subscribers (filter (complement nil?) (map get-subscribers-by-topic topic-permutations))]
    ; Ignoring logs-saved topic when it errors, when there's no active user or its session has expired
    (when (and error (not= topics (topics/get-topic :logs-saved)) (state/get-active-user-id) (state/get-active-tenant-id))
      (save-logs (ih/kebabify error)))
    (when (not-empty relevant-subscribers)
      ; Iterate through each map (representing each topic-permutation)
      (doseq [topic-permutation relevant-subscribers]
        ; Iterate through each key (representing each subscriber to this topic)
        (doseq [subscription-id (keys topic-permutation)]
          (let [cb (get topic-permutation subscription-id)]
            (if-not (nil? cb)
              ; we are passing subscription id as the fourth parameter as a
              ; temporary fix for a specific bug -- we do not recommend using this
              ; as we hope to remove it later and add subscription ID into the response
              ; instead.
              ;
              ; Tech Debt - response should become standardized so that it is:
              ;       - Always a map
              ;       - Reliably contains system-generated information such as
              ;            status and subscription ID
              ;       - Custom formatted data should be stored in some standardized
              ;            key in this map (e.g. :data))

              (cb error topics response subscription-id))))))
    (when (and (fn? callback) callback) (callback error topics response)))
  nil)