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

(def sdk-topics {;; Misc Topics
                 :voice-enabled "cxengage/capabilities/voice-available"
                 :messaging-enabled "cxengage/capabilities/messaging-available"

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

                 ;; CRUD topics
                 :contact-response "cxengage/entities/contacts-response"
                 :get-queue-response "cxengage/entities/get-queue-response"
                 :get-queues-response "cxengage/entities/get-queues-response"
                 :get-transfer-list-response "cxengage/entities/get-transfer-list-response"
                 :get-transfer-lists-response "cxengage/entities/get-transfer-lists-response"
                 :get-user-response "cxengage/entities/get-user-response"
                 :get-users-response "cxengage/entities/get-users-response"
                 :update-user-response "cxengage/entities/update-user-response"

                 ;; Reporting
                 :get-capacity-response "cxengage/reporting/get-capacity-response"
                 :get-available-stats-response "cxengage/reporting/get-available-stats-response"
                 :get-contact-history-response "cxengage/reporting/get-contact-interaction-history-response"
                 :get-contact-interaction-response "cxengage/reporting/get-contact-interactions-response"
                 :batch-response "cxengage/reporting/batch-response"

                 ;; Logging
                 :logs-dumped "cxengage/logging/logs-dumped"
                 :log-level-set "cxengage/logging/log-level-set"
                 :logs-saved "cxengage/logging/logs-saved"


                 ;; Generic Interaction Topics
                 :work-offer-received "cxengage/interactions/work-offer-received"
                 :screen-pop-received "cxengage/interactions/url-pop-received"
                 :work-initiated-received "cxengage/interactions/work-initiated"
                 :disposition-codes-received "cxengage/interactions/disposition-codes-received"
                 :custom-fields-received "cxengage/interactions/custom-fields-received"
                 :work-accepted-received "cxengage/interactions/work-accepted-received"
                 :work-rejected-received "cxengage/interactions/work-rejected-received"
                 :work-ended-received "cxengage/interactions/work-ended-received"
                 :interaction-end-acknowledged "cxengage/interactions/end-acknowledged"
                 :interaction-accept-acknowledged "cxengage/interactions/accept-acknowledged"
                 :interaction-focus-acknowledged "cxengage/interactions/focus-acknowledged"
                 :interaction-unfocus-acknowledged "cxengage/interactions/unfocus-acknowledged"
                 :contact-assignment-acknowledged "cxengage/interactions/contact-assign-acknowledged"
                 :contact-unassignment-acknowledged "cxengage/interactions/contact-unassign-acknowledged"
                 :script-received "cxengage/interactions/script-received"
                 :wrapup-details-received "cxengage/interactions/wrapup-details-received"
                 :enable-wrapup-acknowledged "cxengage/interactions/enable-wrapup-acknowledged"
                 :disable-wrapup-acknowledged "cxengage/interactions/disable-wrapup-acknowledged"
                 :end-wrapup-acknowledged "cxengage/interactions/end-wrapup-acknowledged"
                 :wrapup-started "cxengage/interactions/wrapup-started"

                 ;; Voice Interaction Topics
                 :hold-acknowledged "cxengage/interactions/voice/hold-acknowledged"
                 :resume-acknowledged "cxengage/interactions/voice/resume-acknowledged"
                 :mute-acknowledged "cxengage/interactions/voice/mute-acknowledged"
                 :unmute-acknowledged "cxengage/interactions/voice/unmute-acknowledged"
                 :recording-start-acknowledged "cxengage/interactions/voice/start-recording-acknowledged"
                 :recording-stop-acknowledged "cxengage/interactions/voice/stop-recording-acknowledged"
                 :customer-hold-received "cxengage/interactions/voice/customer-hold-received"
                 :customer-resume-received "cxengage/interactions/voice/customer-resume-received"
                 :customer-transfer-acknowledged "cxengage/interactions/voice/customer-transfer-acknowledged"
                 :cancel-transfer-acknowledged "cxengage/interactions/voice/cancel-transfer-acknowledged"
                 :customer-hold "cxengage/interactions/voice/customer-hold-received"
                 :customer-resume "cxengage/interactions/voice/customer-resume-received"
                 :resource-muted "cxengage/interactions/voice/resource-mute-received"
                 :resource-unmuted "cxengage/interactions/voice/resource-unmute-received"
                 :recording-started "cxengage/interactions/voice/recording-start-received"
                 :recording-ended "cxengage/interactions/voice/recording-end-received"
                 :dial-send-acknowledged "cxengage/interactions/voice/dial-send-acknowledged"
                 :send-digits-acknowledged "cxengage/interactions/voice/send-digits-acknowledged"
                 :transfer-connected "cxengage/interactions/voice/transfer-connected"

                 ;; Messaging Interaction Topics
                 :messaging-history-received "cxengage/interactions/messaging/history-received"
                 :send-message-acknowledged "cxengage/interactions/messaging/send-message-acknowledged"
                 :new-message-received "cxengage/interactions/messaging/new-message-received"
                 })

(defn get-topic [k]
  (if-let [topic (get sdk-topics k)]
    topic
    (log :error "NO TOPIC!!!!!!!!!!!!!!!!" k)))

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
          (log :error "(" topic ") is not a valid subscription topic.")
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
      (log :error "Subscription ID not found")
      (log :info "Successfully unsubscribed"))))

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
