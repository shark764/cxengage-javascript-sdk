(ns client-sdk.pubsub
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [client-sdk-utils.core :as u]
            [clojure.string :as string]
            [client-sdk.state :as state]
            [cljs-uuid-utils.core :as id]
            [client-sdk.api.helpers :as h]
            [cljs.spec :as s]
            [client-sdk.domain.specs :as specs]
            [client-sdk.domain.errors :as err]))

(def module-state (atom {}))

(def topics [[:authentication [:login-response
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
  (let [parts (string/split topic #"/")]
    (:permutations
     (reduce
      (fn [{:keys [x permutations]} part]
        (let [permutation (string/join "/" (take x parts))]
          {:x (inc x), :permutations (conj permutations permutation)}))
      {:x 1, :permutations []}
      parts))))

(defn valid-topic? [topic]
  (contains? topic-strings topic))

(s/def ::subscribe-params
    (s/keys :req-un [::specs/topic ::specs/callback]
            :opt-un []))

(defn subscribe
  ([topic callback]
   (let [params {:topic topic :callback callback}]
     (subscribe params)))
  ([params]
   (if-not (s/valid? ::subscribe-params (js->clj params :keywordize-keys true))
      (err/invalid-params-err)
      (let [{:keys [topic callback]} params]
        (if-not (valid-topic? topic)
          (log :error "That is not a valid subscription topic.")
          (let [subscription-id (id/make-random-uuid)]
            (swap! subscriptions assoc-in [topic subscription-id] callback)
            nil))))))

(defn publish! [topic message]
  (if-not (valid-topic? topic)
    (log :error "That is not a valid subscription topic.")
    (let [all-topics (get-topic-permutations topic)]
      (doseq [topic all-topics]
        (when-let [subscribers (vals (get @subscriptions topic))]
          (doseq [subscription-handler subscribers]
            (subscription-handler (h/format-response message)))
          #_(log :warn (str "No subscribers found for topic `" topic "`, sending to no one.")))))))

(defn module-shutdown-handler [message]
  (log :info "Received shutdown message from Core - PubSub Module shutting down...."))

(defn init [env]
  (log :info "Initializing SDK module: Pub/Sub")
  (swap! module-state assoc :env env)
  (let [module-shutdown< (a/chan 1024)]
    (u/start-simple-consumer! module-shutdown< module-shutdown-handler)
    {:shutdown module-shutdown<}))
