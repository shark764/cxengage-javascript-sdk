(ns cxengage-javascript-sdk.core
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [cljs.spec.alpha :as s]
            [lumbajack.core :as l]
            [cljs.core.async :as a]
            [expound.alpha :as es]
            [camel-snake-kebab.core :as camel]
            [camel-snake-kebab.extras :refer [transform-keys]]

            [cljs-sdk-utils.core :as cxu]
            [cljs-sdk-utils.errors :as e]
            [cljs-sdk-utils.specs :as specs]
            [cljs-sdk-utils.protocols :as pr]
            [cljs-sdk-utils.interop-helpers :as ih]

            [cxengage-javascript-sdk.pubsub :as pu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.modules.sqs :as sqs]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.modules.voice :as voice]
            [cxengage-javascript-sdk.modules.email :as email]
            [cxengage-javascript-sdk.modules.twilio :as twilio]
            [cxengage-javascript-sdk.modules.logging :as logging]
            [cxengage-javascript-sdk.modules.session :as session]
            [cxengage-javascript-sdk.modules.entities :as entities]
            [cxengage-javascript-sdk.modules.contacts :as contacts]
            [cxengage-javascript-sdk.interaction-management :as int]
            [cxengage-javascript-sdk.modules.reporting :as reporting]
            [cxengage-javascript-sdk.modules.messaging :as messaging]
            [cxengage-javascript-sdk.modules.interaction :as interaction]
            [cxengage-javascript-sdk.modules.authentication :as authentication]))

(def *SDK-VERSION* "5.3.28")

(defn register-module
  "Registers a module & its API functions to the CxEngage global. Performs a deep-merge on the existing global with the values provided."
  [module]
  (let [{:keys [api module-name]} (js->clj module :keywordize-keys true)
        old-api (ih/kebabify (ih/get-sdk-global))
        new-api (->> (iu/deep-merge old-api api) (transform-keys camel/->camelCase) (clj->js))]
    (when api
      (ih/set-sdk-global new-api))))

(defn start-internal-module
  "Given an internal Clojurescript SDK module that adheres to the SDKModule protocol, calls the (start) and (refresh-integration) methods to turn the module on."
  [module]
  (pr/start module)
  (pr/refresh-integration module))

(defn start-external-module
  "Given an external Javascript SDK module, calls the start prototype function on it."
  [module]
  (.start module (clj->js (state/get-config))))

(defn start-base-modules
  "Starts any core SDK modules which are not considered 'session-based', I.E. aren't dependent on any user-session specific integrations."
  [comm<]
  (let [authentication (authentication/map->AuthenticationModule.)
        session (session/map->SessionModule.)
        interaction (interaction/map->InteractionModule.)
        entities (entities/map->EntitiesModule.)
        contacts (contacts/map->ContactsModule.)
        logging (logging/map->LoggingModule.)]
    (doseq [module [authentication session interaction entities contacts logging]]
      (start-internal-module module))))

(defn start-session-dependant-modules
  "Starts any core SDK modules which are only able to be turned on once we have the users integrations by way of having started a session."
  [comm<]
  (let [enabled-modules (state/get-enabled-modules)
        sqs {:name "sqs" :record (sqs/map->SQSModule. {:on-msg-fn int/sqs-msg-router})}
        messaging {:name "messaging" :record (messaging/map->MessagingModule {:on-msg-fn int/messaging-msg-router})}
        voice {:name "voice" :record (voice/map->VoiceModule.)}
        email {:name "email" :record (email/map->EmailModule.)}
        reporting {:name "reporting" :record (reporting/map->ReportingModule.)}
        twilio {:name "twilio" :record (twilio/map->TwilioModule.)}]
    (doseq [module [sqs messaging voice email reporting twilio]]
      (when-not (some #(= % (get module :name)) enabled-modules)
        (start-internal-module (get module :record))))))

(defn handle-module-registration-status
  [m]
  (let [{:keys [status module-name]} m]
    (if (= status :failure)
      (log :error (clj->js (e/required-module-failed-to-start-err)))
      (do (log :debug (str "<----- Started " (name module-name) " module! ----->"))
          (state/set-module-enabled! (name module-name))))))

(defn route-module-message [comm< m]
  (case (:type m)
    :config-ready (start-session-dependant-modules comm<)
    :module-registration-status (handle-module-registration-status m)
    nil))

(s/def ::initialize-options
  (s/keys :req-un []
          :opt-un [::specs/consumer-type ::specs/log-level ::specs/environment ::specs/base-url ::specs/blast-sqs-output ::specs/reporting-refresh-rate]))

(defn initialize
  "Internal initialization function (called by the CxEngage namespace where an external initalize() function is exposed). Validates the SDK options provided & bootstraps the whole system."
  [& options]
  (if (> 1 (count (flatten (ih/kebabify options))))
    (do (log :error (clj->js (e/wrong-number-sdk-opts-err))
             nil))
    (let [opts (first (flatten (ih/kebabify options)))
          opts (-> opts
                   (assoc :base-url (or (:base-url opts) "https://api.cxengage.net/v1/"))
                   (assoc :reporting-refresh-rate (or (:reporting-refresh-rate opts) 10000))
                   (assoc :consumer-type (keyword (or (:consumer-type opts) :js)))
                   (assoc :log-level (keyword (or (:log-level opts) :debug)))
                   (assoc :blast-sqs-output (or (:blast-sqs-output opts) false))
                   (assoc :environment (keyword (or (:environment opts) :prod))))]
      (if-not (s/valid? ::initialize-options opts)
        (do (log :error (clj->js (e/bad-sdk-init-opts-err)))
            nil)
        (let [{:keys [log-level consumer-type base-url environment blast-sqs-output reporting-refresh-rate]} opts
              module-comm-chan (a/chan 1024)
              core (ih/camelify {:version *SDK-VERSION*
                                 :subscribe pu/subscribe
                                 :publish pu/publish
                                 :unsubscribe pu/unsubscribe
                                 :internal {:set-time-offset state/set-time-offset!
                                            :api-url iu/api-url}
                                 :dump-state state/get-state-js
                                 :send-core-message #(a/put! module-comm-chan %)
                                 :register-module register-module
                                 :logging {:level log-level}
                                 :start-module start-external-module})]
          (ih/set-sdk-global core)
          (ih/set-log-level! log-level)
          (state/set-base-api-url! base-url)
          (state/set-consumer-type! consumer-type)
          (state/set-reporting-refresh-rate! reporting-refresh-rate)
          (state/set-env! environment)
          (state/set-blast-sqs-output! blast-sqs-output)
          (start-base-modules module-comm-chan)
          (cxu/start-simple-consumer! module-comm-chan (partial route-module-message module-comm-chan)))))))
