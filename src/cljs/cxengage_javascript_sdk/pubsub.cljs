(ns cxengage-javascript-sdk.pubsub
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [cljs.spec.alpha :as s]
            [lumbajack.core]
            [clojure.string :as string]
            [expound.alpha :as e]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.topics :as topics]))

(def sdk-subscriptions (atom {}))

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
      (-> @sdk-subscriptions
          (get matched-topic)
          (vals)))))

(defn publish
  "Publishes a value (or error) to a specific topic, optionally calling the callback provided and optionally leaving the casing of the response unaltered."
  [publish-details]
  (let [{:keys [topics response error callback preserve-casing?]} (js->clj publish-details :keywordize-keys true)
        topics (if (string? topics) (conj #{} topics) topics)
        all-topics (all-topics)
        topics (ih/camelify topics)
        error (ih/camelify error)
        response (if preserve-casing?
                   (clj->js response)
                   (ih/camelify response))
        relevant-subscribers (->> topics
                                  (map get-topic-permutations)
                                  (flatten)
                                  (distinct)
                                  (map get-subscribers-by-topic)
                                  (filter (complement nil?))
                                  (flatten))]
    (when (not-empty relevant-subscribers)
      (doseq [cb relevant-subscribers]
        (doseq [t topics]
          (cb error t response))))
    (when (and (fn? callback) callback) (callback error topics response)))
  nil)
