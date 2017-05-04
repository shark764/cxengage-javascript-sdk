(ns cxengage-javascript-sdk.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [camel-snake-kebab.core :as k]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [lumbajack.core :as l]
            [cxengage-cljs-utils.core :as cxu]

            [cxengage-javascript-sdk.interaction-management :as int]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.pubsub :as pu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.helpers :refer [log]]

            [cxengage-javascript-sdk.next-modules.authentication :as authentication]
            [cxengage-javascript-sdk.next-modules.session :as session]

            [cxengage-javascript-sdk.modules.messaging :as messaging]
            [cxengage-javascript-sdk.modules.entities :as entities]
            [cxengage-javascript-sdk.modules.reporting :as reporting]
            [cxengage-javascript-sdk.modules.contacts :as contacts]
            [cxengage-javascript-sdk.modules.interaction :as interaction]
            [cxengage-javascript-sdk.modules.sqs :as sqs]
            [cxengage-javascript-sdk.modules.voice :as voice]
            [cxengage-javascript-sdk.modules.logging :as logging]
            [cxengage-javascript-sdk.modules.email :as email]
            [cxengage-javascript-sdk.domain.errors :as e]))

(def *SDK-VERSION* "5.0.0-SNAPSHOT")

(defn register-module [module]
  (let [{:keys [api module-name]} module
        old-api (iu/kebabify (aget js/window "CxEngage"))
        new-api (iu/deep-merge old-api api)]
    (when api
      (aset js/window "CxEngage" (->> new-api (transform-keys k/->camelCase) (clj->js))))))

(defn start-external-module [module]
  (.start module (clj->js (state/get-config))))

(defn start-internal-module [module]
  (pr/start module)
  (pr/refresh-integration module))

(defn gen-new-initial-module-config [comm<]
  {:config (state/get-config)
   :state (atom {})
   :core-messages< comm<})

(defn start-base-modules [comm<]
  (let [auth-module (authentication/map->AuthenticationModule. (gen-new-initial-module-config comm<))
        session-module (session/map->SessionModule. (gen-new-initial-module-config comm<))
        interaction-module (interaction/map->InteractionModule. (gen-new-initial-module-config comm<))
        entities-module (entities/map->EntitiesModule. (gen-new-initial-module-config comm<))
        contacts-module (contacts/map->ContactsModule. (gen-new-initial-module-config comm<))
        logging-module (logging/map->LoggingModule. (gen-new-initial-module-config comm<))]
    (doseq [module [auth-module session-module interaction-module entities-module contacts-module logging-module]]
      (start-internal-module module))))

(defn start-session-dependant-modules [comm<]
  (let [sqs-module (sqs/map->SQSModule. (assoc (gen-new-initial-module-config comm<) :on-msg-fn int/sqs-msg-router))
        messaging-module (messaging/map->MessagingModule (assoc (gen-new-initial-module-config comm<) :on-msg-fn int/messaging-msg-router))
        voice-module (voice/map->VoiceModule. (gen-new-initial-module-config comm<))
        email-module (email/map->EmailModule. (gen-new-initial-module-config comm<))
        reporting-module (reporting/map->ReportingModule. (gen-new-initial-module-config comm<))]
    (doseq [module [sqs-module messaging-module voice-module email-module reporting-module]]
      (start-internal-module module))))

(defn handle-module-registration-status [m]
  (let [{:keys [status module-name]} m]
    (if (= status :failure)
      (log :fatal (clj->js (e/required-module-failed-to-start-err)))
      (log :info (str "<----- Started " (name module-name) " module! ----->")))))

(defn route-module-message [comm< m]
  (case (:type m)
    :config-ready (start-session-dependant-modules comm<)
    :module-registration-status (handle-module-registration-status m)
    nil))

(s/def ::base-url string?)
(s/def ::type #{:js :cljs})
(s/def ::environment #{:dev :qe :staging :prod})
(s/def ::log-level #{:debug :info :warn :error :fatal :off})
(s/def ::blast-sqs-output boolean?)
(s/def ::initialize-options
  (s/keys :req-un []
          :opt-un [::consumer-type ::log-level ::environment ::base-url ::blast-sqs-output ::reporting-refresh-rate]))

(defn initialize
  ([& options]
   (if (> 1 (count (flatten (iu/kebabify options))))
     (do (js/console.error (clj->js (e/wrong-number-sdk-opts-err))
                           nil))
     (let [opts (first (flatten (iu/kebabify options)))
           opts (-> opts
                    (assoc :base-url (or (:base-url opts) "https://api.cxengage.net/v1/"))
                    (assoc :reporting-refresh-rate (or (:reporting-refresh-rate opts) 10000))
                    (assoc :consumer-type (keyword (or (:consumer-type opts) :js)))
                    (assoc :log-level (keyword (or (:log-level opts) :info)))
                    (assoc :blast-sqs-output (or (:blast-sqs-output opts) false))
                    (assoc :environment (keyword (or (:environment opts) :prod))))]
       (if-not (s/valid? ::initialize-options opts)
         (do (js/console.error (clj->js (e/bad-sdk-init-opts-err)))
             nil)
         (let [{:keys [log-level consumer-type base-url environment blast-sqs-output reporting-refresh-rate]} options
               module-comm-chan (a/chan 1024)
               core (iu/camelify {:version *SDK-VERSION*
                                  :subscribe pu/subscribe
                                  :publish pu/js-publish
                                  :unsubscribe pu/unsubscribe
                                  :dump-state state/get-state-js
                                  :send-core-message #(a/put! module-comm-chan %)
                                  :register-module register-module
                                  :start-module start-external-module})]
           (aset js/window "CxEngage" core)
           (state/set-base-api-url! base-url)
           (state/set-consumer-type! consumer-type)
           (state/set-log-level! log-level l/levels)
           (state/set-reporting-refresh-rate! reporting-refresh-rate)
           (state/set-env! environment)
           (state/set-blast-sqs-output! blast-sqs-output)
           (start-base-modules module-comm-chan)
           (cxu/start-simple-consumer! module-comm-chan (partial route-module-message module-comm-chan))
           (let [api (aget js/window "CxEngage")]
             (if (= consumer-type :cljs)
               (iu/kebabify api)
               api))))))))
