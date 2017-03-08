(ns cxengage-javascript-sdk.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [camel-snake-kebab.core :as k]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [lumbajack.core :as l]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.pubsub :as pu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.modules.authentication :as authentication]
            [cxengage-javascript-sdk.modules.entities :as entities]
            [cxengage-javascript-sdk.modules.reporting :as reporting]
            [cxengage-javascript-sdk.modules.session :as session]
            [cxengage-javascript-sdk.modules.contacts :as contacts]
            [cxengage-javascript-sdk.modules.interaction :as interaction]
            [cxengage-javascript-sdk.modules.sqs :as sqs]
            [cxengage-javascript-sdk.modules.messaging :as messaging]
            [cxengage-javascript-sdk.modules.voice :as voice]
            [cxengage-javascript-sdk.modules.logging :as logging]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-cljs-utils.core :as cxu]
            [cxengage-javascript-sdk.interaction-management :as int]))

(defn register-module [module]
  (let [{:keys [api module-name]} module
        old-api (iu/kebabify (aget js/window "serenova" "cxengage" "api"))
        new-api (iu/deep-merge old-api api)]
    (when api
      (aset js/window "serenova" "cxengage" "api" (->> new-api (transform-keys k/->camelCase) (clj->js))))))

(defn start-external-module [module]
  (.start module (clj->js (state/get-config))))

(defn start-internal-module [module]
  (pr/start module))

(defn gen-new-initial-module-config [comm<]
  {:config (state/get-config) :state (atom {}) :core-messages< comm<})

(defn start-base-modules [comm<]
  (let [auth-module (authentication/map->AuthenticationModule. (gen-new-initial-module-config comm<))
        session-module (session/map->SessionModule. (gen-new-initial-module-config comm<))
        interaction-module (interaction/map->InteractionModule. (gen-new-initial-module-config comm<))
        entities-module (entities/map->EntitiesModule. (gen-new-initial-module-config comm<))
        contacts-module (contacts/map->ContactsModule. (gen-new-initial-module-config comm<))
        reporting-module (reporting/map->ReportingModule. (gen-new-initial-module-config comm<))
        logging-module (logging/map->LoggingModule. (gen-new-initial-module-config comm<))]
    (doseq [module [auth-module session-module interaction-module entities-module contacts-module reporting-module logging-module]]
      (start-internal-module module))))

(defn start-session-dependant-modules [comm<]
  (let [sqs-module (sqs/map->SQSModule. (assoc (gen-new-initial-module-config comm<) :on-msg-fn int/sqs-msg-router))
        messaging-module (messaging/map->MessagingModule (assoc (gen-new-initial-module-config comm<) :on-msg-fn int/messaging-msg-router))
        voice-module (voice/map->VoiceModule. (gen-new-initial-module-config comm<))]
    (doseq [module [sqs-module messaging-module voice-module]]
      (start-internal-module module))))

(defn route-module-message [comm< m]
  (case m
    :config-ready (start-session-dependant-modules comm<)
    nil))

(s/def ::base-url string?)
(s/def ::type #{:js :cljs})
(s/def ::environment #{:dev :qe :staging :prod})
(s/def ::log-level #{:debug :info :warn :error :fatal :off})
(s/def ::blast-sqs-output boolean?)
(s/def ::initialize-options
  (s/keys :req-un []
          :opt-un [::consumer-type ::log-level ::environment ::base-url ::blast-sqs-output]))

(defn initialize
  ([] (initialize {}))
  ([options & rest] (clj->js (e/wrong-number-of-args-error)))
  ([options]
   (let [options (iu/extract-params options)
         options (-> options
                     (assoc :base-url (or (:base-url options) "https://api.cxengage.net/v1/"))
                     (assoc :consumer-type (keyword (or (:consumer-type options) :js)))
                     (assoc :log-level (keyword (or (:log-level options) :info)))
                     (assoc :blast-sqs-output (or (:blast-sqs-output options) false))
                     (assoc :environment (keyword (or (:environment options) :prod))))]
     (if-not (s/valid? ::initialize-options options)
       (clj->js (e/invalid-args-error (s/explain-data ::initialize-options options)))
       (let [{:keys [log-level consumer-type base-url environment blast-sqs-output]} options
             core (iu/camelify {:api {:subscribe pu/subscribe
                                      :publish pu/publish
                                      :unsubscribe pu/unsubscribe
                                      :dump-state state/get-state-js}
                                :modules {:register register-module
                                          :start start-external-module}})
             module-comm-chan (a/chan 1024)]
         (state/set-base-api-url! base-url)
         (state/set-consumer-type! consumer-type)
         (state/set-log-level! log-level l/levels)
         (state/set-env! environment)
         (state/set-blast-sqs-output! blast-sqs-output)
         (aset js/window "serenova" #js {"cxengage" core})
         (start-base-modules module-comm-chan)
         (cxu/start-simple-consumer! module-comm-chan (partial route-module-message module-comm-chan))
         (let [api (aget js/window "serenova" "cxengage" "api")]
           (if (= consumer-type :cljs)
             (iu/kebabify api)
             api)))))))
