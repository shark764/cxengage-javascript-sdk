(ns cxengage-javascript-sdk.core
  (:require-macros [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.spec.alpha :as s]
            [cljs.core.async :as a]
            [expound.alpha :as es]
            [camel-snake-kebab.core :as camel]
            [camel-snake-kebab.extras :refer [transform-keys]]

            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]

            [cxengage-javascript-sdk.pubsub :as pu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.modules.sqs :as sqs]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.modules.voice :as voice]
            [cxengage-javascript-sdk.modules.email :as email]
            [cxengage-javascript-sdk.modules.twilio :as twilio]
            [cxengage-javascript-sdk.modules.logging :as logging]
            [cxengage-javascript-sdk.modules.session :as session]
            [cxengage-javascript-sdk.modules.api :as api]
            [cxengage-javascript-sdk.modules.entities :as entities]
            [cxengage-javascript-sdk.modules.contacts :as contacts]
            [cxengage-javascript-sdk.interaction-management :as int]
            [cxengage-javascript-sdk.modules.reporting :as reporting]
            [cxengage-javascript-sdk.modules.messaging :as messaging]
            [cxengage-javascript-sdk.modules.interaction :as interaction]
            [cxengage-javascript-sdk.modules.authentication :as authentication]
            [cxengage-javascript-sdk.modules.zendesk :as zendesk]
            [cxengage-javascript-sdk.modules.salesforce-classic :as sfc]
            [cxengage-javascript-sdk.modules.salesforce-lightning :as sfl]
            [cxengage-javascript-sdk.modules.testing :as testing]))

(def *SDK-VERSION* "8.52.0")

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
        api (api/map->ApiModule.)
        entities (entities/map->EntitiesModule.)
        contacts (contacts/map->ContactsModule.)
        logging (logging/map->LoggingModule.)
        reporting (reporting/map->ReportingModule.)
        testing (testing/map->TestingModule.)]
    (doseq [module [authentication session interaction api entities contacts logging reporting testing]]
      (start-internal-module module))))

(defn start-crm-module
  "Starts the appropriate CRM module if one is specified on initialization."
  [comm< crm-module]
  (let [zendesk (zendesk/map->ZendeskModule.)
        sfc (sfc/map->SFCModule.)
        sfl (sfl/map->SFLModule.)]
    (cond
      (= crm-module :zendesk) (start-internal-module zendesk)
      (= crm-module :salesforce-classic) (start-internal-module sfc)
      (= crm-module :salesforce-lightning) (start-internal-module sfl)
      :else (log :debug "<----- No CRM Module specified ----->"))))

(defn start-session-dependant-modules
  "Starts any core SDK modules which are only able to be turned on once we have the users integrations by way of having started a session."
  [comm<]
  (let [enabled-modules (state/get-enabled-modules)
        sqs {:name "sqs" :record (sqs/map->SQSModule. {:on-msg-fn int/sqs-msg-router})}
        messaging {:name "messaging" :record (messaging/map->MessagingModule {:on-msg-fn int/messaging-msg-router})}
        voice {:name "voice" :record (voice/map->VoiceModule.)}
        email {:name "email" :record (email/map->EmailModule.)}
        twilio {:name "twilio" :record (twilio/map->TwilioModule.)}
        modules-to-be-enabled (if-not (state/get-supervisor-mode)
                                [sqs messaging voice email twilio]
                                (if (state/is-default-extension-twilio)
                                  [sqs voice twilio]
                                  [sqs voice]))]
    (doseq [module modules-to-be-enabled]
      (if-not (some #(= % (get module :name)) enabled-modules)
        (start-internal-module (get module :record))
        (log :debug "Module already enabled: " (get module :name))))))

(defn handle-module-registration-status
  [m]
  (let [{:keys [status module-name]} m]
    (if (= status :failure)
      (pu/publish {:topics "cxengage/errors/error/failed-to-start-module-error"
                   :error (e/required-module-failed-to-start-err (name module-name))})
      (do (log :debug (str "<----- Started " (name module-name) " module! ----->"))
          (state/set-module-enabled! (name module-name))))))

(defn route-module-message [comm< m]
  (case (:type m)
    :config-ready (start-session-dependant-modules comm<)
    :module-registration-status (handle-module-registration-status m)
    nil))

(defn start-simple-consumer!
  [ch handler]
  (go-loop []
    (when-let [message (a/<! ch)]
      (handler message)
      (recur))))

(s/def ::initialize-options
  (s/keys :req-un []
          :opt-un [::specs/consumer-type ::specs/log-level ::specs/environment ::specs/base-url ::specs/blast-sqs-output ::specs/reporting-refresh-rate ::specs/crm-module ::specs/locale]))

(defn initialize
  "Internal initialization function (called by the CxEngage namespace where an external initalize() function is exposed). Validates the SDK options provided & bootstraps the whole system."
  [& options]
  (if (> 1 (count (flatten (ih/kebabify options))))
    (do (js/console.error (clj->js (e/wrong-number-sdk-opts-err))
             nil))
    (let [opts (first (flatten (ih/kebabify options)))
          opts (-> opts
                   (assoc :base-url (or (:base-url opts) "https://api.cxengage.net/v1/"))
                   (assoc :crm-module (keyword (or (:crm-module opts) :none)))
                   (assoc :reporting-refresh-rate (or (:reporting-refresh-rate opts) 10000))
                   (assoc :consumer-type (keyword (or (:consumer-type opts) :js)))
                   (assoc :log-level (keyword (or (:log-level opts) :debug)))
                   (assoc :locale (or (:locale opts) "en-US"))
                   (assoc :blast-sqs-output (or (:blast-sqs-output opts) false))
                   (assoc :environment (keyword (or (:environment opts) :prod)))
                   (assoc :supervisor-mode (or (:supervisor-mode opts) false)))]
      (if-not (s/valid? ::initialize-options opts)
        (do (js/console.error (clj->js (e/bad-sdk-init-opts-err)))
            nil)
        (let [{:keys [log-level consumer-type base-url environment blast-sqs-output reporting-refresh-rate crm-module locale supervisor-mode]} opts
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
          (state/set-locale! locale)
          (state/set-base-api-url! base-url)
          (state/set-consumer-type! consumer-type)
          (state/set-reporting-refresh-rate! reporting-refresh-rate)
          (state/set-env! environment)
          (state/set-blast-sqs-output! blast-sqs-output)
          (state/set-supervisor-mode! supervisor-mode)
          (start-base-modules module-comm-chan)
          (start-crm-module module-comm-chan crm-module)
          (start-simple-consumer! module-comm-chan (partial route-module-message module-comm-chan)))))))
