(ns cxengage-javascript-sdk.next-modules.messaging
  (:require-macros [cxengage-javascript-sdk.macros :refer [def-sdk-fn]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.helpers :refer [log]]
            [cljs-time.format :as fmt]
            [cljs-time.core :as time]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.interop-helpers :as ih]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cognitect.transit :as t]
            [cljs-uuid-utils.core :as id]
            [camel-snake-kebab.core :as camel]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.set :refer [rename-keys]]))

(defn- gen-payload [message]
  (let [{:keys [message resource-id tenant-id interaction-id]} message
        uid (str (id/make-random-uuid))
        metadata {:name "Agent"
                  :first-name "Agent"
                  :last-name "Agent"
                  :type "agent"}
        body {:text message}]
    {:id uid
     :tenant-id tenant-id
     :type "message"
     :to interaction-id
     :from resource-id
     :metadata metadata
     :body body
     :timestamp (fmt/unparse (fmt/formatters :date-time) (time/now))}))

(defn- format-payload [message]
  (t/write (t/writer :json-verbose) (clojure.walk/stringify-keys message)))

(defn- get-mqtt-client [])

(def-sdk-fn get-transcripts
  ::get-transcripts-spec
  (p/get-topic :transcript-response)
  [params]
  (let [{:keys [callback interaction-id topic]} params
        {:keys [api-response status]} (a/<! (iu/get-interaction-files interaction-id))]
    (when (= status 200)
      (let [tenant-id (state/get-active-tenant-id)
            {:keys [results]} api-response
            transcripts (filterv #(= (:artifact-type %) "messaging-transcript") results)]
        (if (= (count transcripts) 0)
          (p/publish {:topics topic
                      :response []
                      :callback callback})
          (doseq [t transcripts]
            (let [{:keys [artifact-id]} t
                  {:keys [api-response status]} (a/<! (iu/get-artifact interaction-id tenant-id artifact-id))
                  {:keys [files]} api-response]
              (when (= status 200)
                (p/publish {:topics topic
                            :response files
                            :callback callback})))))))))

(def-sdk-fn send-message
  ::send-message-spec
  (p/get-topic :send-message-acknowledged)
  [params]
  (let [{:keys [interaction-id callback message topic]} params
        tenant-id (state/get-active-tenant-id)
        resource-id (state/get-active-user-id)
        msg (-> {:resource-id resource-id
                 :tenant-id tenant-id
                 :interaction-id interaction-id
                 :topic topic
                 :callback callback}
                gen-payload
                format-payload
                (Paho.MQTT.Message.))
        mqtt-topic (str (name (state/get-env)) "/tenants/" tenant-id "/channels/" interaction-id)]
    (set! (.-destinationName msg) mqtt-topic)
    (set! (.-qos msg) 1)
    (.send (get-mqtt-client) msg)
    (p/publish {:topics topic
                :response true
                :callback callback})))

(defrecord MessagingModule [config state core-messages<]
  pr/SDKModule
  (start [this]
    (let [module-name :messaging
          client-id (state/get-active-user-id)
          mqtt-integration (state/get-integration-by-type "messaging")
          {:keys [credentials]} mqtt-integration
          mqtt-integration (->> (merge (select-keys mqtt-integration [:region :endpoint])
                                       (select-keys credentials [:secret-key
                                                                 :access-key
                                                                 :session-token]))
                                (transform-keys camel/->kebab-case-keyword)
                                (#(rename-keys % {:region :region-name})))]
      (if-not mqtt-integration
        (a/put! core-messages< {:module-registration-status :failure
                                :module module-name})
        (do (ih/register
             {:api {:interactions {module-name {:send-message send-message
                                                :get-transcripts get-transcripts}}}
              :module-name module-name})
            (log :info (str "<----- Started " (name module-name) " SDK module! ----->"))))))
  (stop [this])
  (refresh-integration [this]))
