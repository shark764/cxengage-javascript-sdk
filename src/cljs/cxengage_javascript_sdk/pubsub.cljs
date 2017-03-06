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

(def sdk-topics {:voice-enabled "cxengage/features/voice-enabled"
                 :messaging-enabled "cxengage/features/messaging-enabled"
                 :login "cxengage/authentication/login"
                 :logout "cxengage/authentication/logout"
                 :work-offer "cxengage/interactions/work-offer"
                 :work-accepted "cxengage/interactions/work-accepted"
                 :work-ended "cxengage/interactions/work-ended"
                 :work-initiated "cxengage/interactions/work-initiated"
                 :accept-response "cxengage/interactions/accept-response"
                 :end-response "cxengage/interactions/end-response"
                 :work-rejected "cxengage/interactions/work-rejected"
                 :contact-unassigned "cxengage/interactions/contact-unassigned"
                 :contact-assigned "cxengage/interactions/contact-assigned"
                 :wrapup-details "cxengage/interactions/wrapup-details"
                 :wrapup-started "cxengage/interactions/wrapup-started"
                 :wrapup-on "cxengage/interactions/wrapup-on"
                 :wrapup-off "cxengage/interactions/wrapup-off"
                 :wrapup-end "cxengage/interactions/wrapup-end"
                 :screen-pop-uri "cxengage/interactions/screen-pop/uri"
                 :send-message-response "cxengage/messaging/send-message-response"
                 :new-message-received "cxengage/messaging/new-message-received"
                 :history "cxengage/messaging/history"
                 :hold-started "cxengage/voice/hold-started"
                 :hold-ended "cxengage/voice/hold-ended"
                 :mute-started "cxengage/voice/mute-started"
                 :mute-ended "cxengage/voice/mute-ended"
                 :recording-started "cxengage/voice/recording-started"
                 :recording-ended "cxengage/voice/recording-ended"
                 :phone-controls-response "cxengage/voice/phone-controls-response"
                 :transfer-response "cxengage/voice/transfer-response"
                 :cancel-transfer-response "cxengage/voice/cancel-transfer-response"
                 :transfer-connected "cxengage/voice/transfer-connected"
                 :dial-response "cxengage/voice/dial-response"
                 :extension-set "cxengage/voice/extension-set"
                 :extensions-response "cxengage/voice/extensions-response"
                 :get-user-response "cxengage/entities/get-user-response"
                 :get-users-response "cxengage/entities/get-users-response"
                 :get-queue-response "cxengage/entities/get-queue-response"
                 :get-queues-response "cxengage/entities/get-queues-response"
                 :get-transfer-list-response "cxengage/entities/get-transfer-list-response"
                 :get-transfer-lists-response "cxengage/entities/get-transfer-lists-response"
                 :active-tenant-set "cxengage/session/active-tenant-set"
                 :started "cxengage/session/started"
                 :state-changed "cxengage/session/state-changed"
                 :direction-changed "cxengage/session/direction-changed"
                 :get-response "cxengage/get-response"
                 :search-response "cxengage/contacts/search-response"
                 :create-response "cxengage/contacts/create-response"
                 :update-response "cxengage/contacts/update-response"
                 :delete-response "cxengage/contacts/delete-response"
                 :list-attributes-response "cxengage/contacts/list-attributes-response"
                 :get-layout-response "cxengage/contacts/get-layout-response"
                 :list-layouts-response "cxengage/contacts/list-layouts-response"
                 :polling-response "cxengage/reporting/polling-response"
                 :available-stats-response "cxengage/reporting/available-stats-response"
                 :check-capacity-response "cxengage/reporting/check-capacity-response"
                 :fatal-error "cxengage/errors/fatal"
                 :error-error "cxengage/errors/error"})

(defn get-topic [k]
  (get sdk-topics k))

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
