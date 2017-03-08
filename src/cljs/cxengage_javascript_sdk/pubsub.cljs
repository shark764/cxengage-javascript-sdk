(ns cxengage-javascript-sdk.pubsub
  (:require-macros [lumbajack.macros :refer [log]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [clojure.string :as string]
            [clojure.set :as set]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.state :as st]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cljs-uuid-utils.core :as id]
            [cxengage-javascript-sdk.domain.specs :as specs]))

(def sdk-subscriptions (atom {}))

(def sdk-topics {
                 ;; Authentication Topics
                 :login-response "cxengage/authentication/login-response"

                 ;; Session Topics
                 :active-tenant-set "cxengage/session/set-active-tenant-response"
                 :config-response "cxengage/session/config-details"
                 :presence-state-changed "cxengage/session/state-change-response"
                 :presence-state-change-request-acknowledged "cxengage/session/state-change-request-acknowledged"
                 :presence-heartbeats-response "cxengage/session/session-heartbeat-response"
                 :session-started "cxengage/session/started"
                 :set-direction-response "cxengage/session/set-direction-response"
                 :extension-list "cxengage/session/extension-list"
                 :tenant-list "cxengage/session/tenant-list"
                 :session-ended "cxengage/session/ended"

                 ;; Generic Interaction Topics

                 ;; Voice Interaction Topics

                 ;; Messaging Interaction Topics
                 })

(defn get-topic [k]
  (if-let [topic (get sdk-topics k)]
    topic
    (js/console.error "NO TOPIC!!!!!!!!!!!!!!!!" k)))

(defn get-topic-permutations [topic]
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
          (vals sdk-topics)))

(defn valid-topic? [topic]
  (let [valid-topics (all-topics)]
    (if (some #(= topic %) valid-topics)
      topic)))

(s/def ::subscribe-params
  (s/keys :req-un [::specs/topic ::specs/callback]
          :opt-un []))

(defn subscribe [topic callback]
  (let [params {:topic topic :callback callback}]
    (if-not (s/valid? ::subscribe-params params)
      (e/invalid-args-error (s/explain-data ::subscribe-params params))
      (let [subscription-id (str (id/make-random-uuid))]
        (if-not (valid-topic? topic)
          (js/console.error "(" topic ") is not a valid subscription topic.")
          (do (swap! sdk-subscriptions assoc-in [topic subscription-id] callback)
              subscription-id))))))

(s/def ::unsubscribe-params
  (s/keys :req-un [::specs/subscription-id]
          :opt-un []))

(defn unsubscribe [subscription-id]
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
      (js/console.error "Subscription ID not found")
      (js/console.info "Successfully unsubscribed"))))

(defn publish
  ([publish-details]
   (let [{:keys [topics response error callback]} publish-details
         topics (if (string? topics) (conj #{} topics) topics)
         all-topics (all-topics)
         subscriptions-to-publish (reduce-kv
                                   (fn [subs-to-pub topic subscriptions]
                                     (if (some #(= topic %) all-topics)
                                       (merge subs-to-pub subscriptions)))
                                   {}
                                   @sdk-subscriptions)
         subscription-callbacks (vals subscriptions-to-publish)
         topics (iu/camelify topics)
         error (iu/camelify error)
         response (iu/camelify response)]
     (doseq [cb subscription-callbacks]
       (doseq [t topics]
         (cb error t response)))
     (when callback (callback error topics response)))))
