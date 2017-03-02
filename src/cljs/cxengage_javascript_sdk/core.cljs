(ns cxengage-javascript-sdk.core
  (:require-macros [lumbajack.macros :refer [log]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [camel-snake-kebab.core :as k]
            [camel-snake-kebab.extras :refer [transform-keys]]
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
        reporting-module (reporting/map->ReportingModule. (gen-new-initial-module-config comm<))]
    (doseq [module [auth-module session-module interaction-module entities-module contacts-module reporting-module]]
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
(s/def ::consumer-type #{:js :cljs})
(s/def ::environment #{"dev" "qe" "staging" "prod"})
(s/def ::log-level #{"debug" "info" "warn" "error" "fatal" "off"})
(s/def ::initialize-options
  (s/keys :req-un [::base-url ::environment]
          :opt-un [::consumer-type ::log-level]))

(defn ^:export initialize
  ([] (clj->js (e/wrong-number-of-args-error)))
  ([options & rest] (clj->js (e/wrong-number-of-args-error)))
  ([options]
   (let [options (iu/extract-params options)]
     (if-not (s/valid? ::initialize-options options)
       (clj->js (e/invalid-args-error (s/explain-data ::initialize-options options)))
       (let [{:keys [log-level consumer-tyoe base-url environment]} options
             log-level (or (:log-level options) :info)
             consumer-type (or (:consumer-type options) :js)
             environment (keyword environment)
             core (iu/camelify {:api {:subscribe pu/subscribe
                                      :publish pu/publish}
                                :modules {:register register-module
                                          :start start-external-module}})
             _ (js/console.info "CAMEL" core)
             module-comm-chan (a/chan 1024)]
         (state/set-base-api-url! base-url)
         (state/set-consumer-type! consumer-type)
         (state/set-log-level! log-level)
         (state/set-env! environment)
         (aset js/window "serenova" #js {"cxengage" core})
         (start-base-modules module-comm-chan)
         (cxu/start-simple-consumer! module-comm-chan (partial route-module-message module-comm-chan))
         (aget js/window "serenova"))))))
