(ns client-sdk.pubsub
  (:require [cljs.core.async :as a]
            [client-sdk-utils.core :as u]
            [lumbajack.core :refer [log]]
            [clojure.string :as s]
            [cljs-uuid-utils.core :as id]
            [client-sdk.api.helpers :as h]))

(def module-state (atom {}))

(def topics [[:reporting [:poll-response]]
             [:flow [:interrupts]]
             [:auth [:login-response]]])

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

(defn publish! [message]
  (let [{:keys [topic]} message]
    (if-not (valid-topic? topic)
      (log :error "That is not a valid subscription topic.")
      (let [all-topics (get-topic-permutations topic)]
        (doseq [topic all-topics]
          (let [subscribers (vals (get @subscriptions topic))]
            (doseq [subscription-handler subscribers]
              (subscription-handler (h/format-response message)))))))))

(defn api []
  {:subscribe subscribe})

(defn init [env]
  (swap! module-state assoc :env env)
  (let [module-inputs< (a/chan 1024)]
    (u/start-simple-consumer! module-inputs< publish!)
    module-inputs<))
