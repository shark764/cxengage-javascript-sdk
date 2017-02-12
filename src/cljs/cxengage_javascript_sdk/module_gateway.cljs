(ns cxengage-javascript-sdk.module-gateway
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [clojure.string :as str]
            [cxengage-cljs-utils.core :as u]
            [cxengage-javascript-sdk.modules.auth :as auth]
            [cxengage-javascript-sdk.modules.presence :as presence]
            [cxengage-javascript-sdk.modules.flow :as flow]
            [cxengage-javascript-sdk.modules.sqs :as sqs]
            [cxengage-javascript-sdk.modules.mqtt :as mqtt]
            [cxengage-javascript-sdk.modules.messaging :as msg]
            [cxengage-javascript-sdk.modules.reporting :as reporting]
            [cxengage-javascript-sdk.modules.crud :as crud]
            [cxengage-javascript-sdk.modules.contacts :as contacts]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.pubsub :as pubsub :refer [sdk-error-response]]
            [cxengage-javascript-sdk.modules.twilio :as twilio]
            [cxengage-javascript-sdk.state :as state]
            [lumbajack.core :as logging]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.errors :as err]))

(def pub-chan (a/chan))
(def err-chan (a/chan))

(defn register-module
  [publication module-name init-map]
  (->> init-map
       (:messages)
       (a/sub publication (str "modules/" (str/upper-case (name module-name)))))
  (->> init-map
       (:shutdown)
       (a/sub publication :modules/SHUTDOWN)))

(defn register-async-module
  [publication module-name init-fn router]
  (let [start-chan (a/promise-chan)]
    (a/sub publication (str "init/" (str/upper-case (name module-name))) start-chan)
    (go
      (let [{:keys [type module-name config]} (a/<! start-chan)
            done-chan (a/promise-chan)
            init-map (case module-name
                       :mqtt (init-fn (state/get-env) done-chan (state/get-active-user-id) config router err-chan)
                       :twilio (init-fn (state/get-env) done-chan config router err-chan)
                       :sqs (init-fn (state/get-env) done-chan config router)
                       (log :error "Unrecognized asynchronous module registration attempt."))]
        (register-module publication module-name init-map)
        (let [registration-response (a/<! done-chan)]
          (if-not (= (:status registration-response) :ok)
            (do
              (log :error (str "Failed to register module `" (name module-name) "`!"))
              (sdk-error-response "cxengage/errors/error" (err/sdk-request-error (str module-name " failed to start. You may want to reload the page.")) nil))
            (log :debug (str "SDK Module `" (str/upper-case (name module-name)) "` succesfully registered (async)."))))))))

(defn handle-shutdown!
  [message]
  (let [{:keys [type]} message]
    (case type
      (:fatal/SHUTDOWN :messaging/SHUTDOWN :interaction/SHUTDOWN) (a/put! pub-chan message)
      nil)))

(defn start-modules
  [env terseLogs logLevel twilio-router mqtt-router sqs-router]
  (let [publication (a/pub pub-chan
                           (fn [message] (cond
                                           (= "INIT"  (peek (clojure.string/split (str (:type message)) #"[:/]+"))) (str "init/" (second (clojure.string/split (str (:type message)) #"[:/]+")))
                                           (= :fatal/SHUTDOWN (:type message)) :core/SHUTDOWN
                                           (= :messaging/SHUTDOWN (:type message)) :core/SHUTDOWN
                                           (= :interaction/SHUTDOWN (:type message)) :core/SHUTDOWN
                                           (= :modules/SHUTDOWN (:type message)) :modules/SHUTDOWN
                                           :default (str "modules/" (second (clojure.string/split (str (:type message)) #"[:/]+"))))))]
    (register-module publication :logging (logging/init env {:terse? (or terseLogs false) :level logLevel}))
    (register-module publication :messaging (msg/init env))
    (register-module publication :interactions (flow/init env err-chan))
    (register-module publication :auth (auth/init env))
    (register-module publication :crud (crud/init env))
    (register-module publication :reporting (reporting/init env))
    (register-module publication :session (presence/init env err-chan))
    (register-module publication :contacts (contacts/init env))
    (register-async-module publication :twilio twilio/init twilio-router)
    (register-async-module publication :mqtt mqtt/init mqtt-router)
    (register-async-module publication :sqs sqs/init sqs-router)
    (pubsub/init env)
    (u/start-simple-consumer! err-chan handle-shutdown!)
    publication))

(defn >get-publication-channel
  []
  pub-chan)

(defn send-module-message [message]
  (let [{:keys [type]} message
        module-response-chan (a/promise-chan)]
    (if pub-chan
      (do (a/put! pub-chan message)
          (:resp-chan message))
      (do (log :error "Publication channel closed unexpectedly.")
          nil))))
