(ns cxengage-javascript-sdk.pubsub
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [clojure.string :as string]
            [cljs-uuid-utils.core :as id]
            [cxengage-cljs-utils.core :as u]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.errors :as err]))

(def module-state (atom {}))

(def topics [[:features [:voice-enabled]
                        [:messaging-enabled]]
             [:authentication [:login]]
             [:interactions [:work-offer
                             :work-accepted
                             :work-ended
                             :work-initiated
                             :accept-response
                             :end-response
                             :work-rejected
                             :contact-unassigned
                             :contact-assigned]]
             [:messaging [:send-message-response
                          :new-message-received
                          :history]]
             [:voice [:hold-started
                      :hold-ended
                      :mute-started
                      :mute-ended
                      :recording-started
                      :recording-ended
                      :phone-controls-response]]
             [:session [:active-tenant-set
                        :started
                        :state-changed]]
             [:contacts [:get-response
                         :search-response
                         :create-response
                         :update-response
                         :delete-response
                         :list-attributes-response
                         :get-layout-response
                         :list-layouts-response]]])

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
  (s/keys :req-un [:specs/topic :specs/callback]
          :opt-un []))

(defn subscribe
  ([topic callback]
   (let [params {:topic topic :callback callback}]
     (subscribe params)))
  ([params]
   (if-not (s/valid? ::subscribe-params (js->clj params :keywordize-keys true))
     (iu/format-response (err/invalid-params-err))
     (let [{:keys [topic callback]} params]
       (if-not (valid-topic? topic)
         (do (log :error (str "That is not a valid subscription topic (" topic ")."))
             (iu/format-response (err/subscription-topic-not-recognized-err)))
         (let [subscription-id (id/make-random-uuid)]
           (swap! subscriptions assoc-in [topic subscription-id] callback)
           nil))))))

(defn publish! [original-topic error response]
  (if-not (valid-topic? original-topic)
    (do (log :error (str "That is not a valid subscription topic (" original-topic ")."))
        nil)
    (let [all-topics (get-topic-permutations original-topic)]
      (doseq [topic all-topics]
        (when-let [subscribers (vals (get @subscriptions topic))]
          (doseq [subscription-handler subscribers]
            (subscription-handler error original-topic response)))))))

(defn send-response [topic message callback]
  (let [{:keys [error response]} message
        error (iu/format-response error)
        response (iu/format-response response)]
    (when callback (callback error topic response))
    (publish! topic error response)))

(defn sdk-error-response
  ([topic error]
   (sdk-error-response topic error nil))
  ([topic error callback]
   (let [error-msg {:error error
                    :response nil}]
     (send-response topic error-msg callback))))

(defn sdk-response
  ([topic response]
   (sdk-response topic response nil))
  ([topic response callback]
   (let [success-msg {:error nil
                      :response response}]
     (send-response topic success-msg callback))))

(defn module-shutdown-handler [message]
  (log :info "Received shutdown message from Core - PubSub Module shutting down...."))

(defn init [env]
  (log :debug "Initializing SDK module: Pub/Sub")
  (swap! module-state assoc :env env)
  (let [module-shutdown< (a/chan 1024)]
    (u/start-simple-consumer! module-shutdown< module-shutdown-handler)
    {:shutdown module-shutdown<}))
