(ns cxengage-javascript-sdk.next.modules
  (:require-macros [lumbajack.macros :refer [log]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.spec :as s]
            [cljs.core.async :as a]
            [camel-snake-kebab.core :as k]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [cxengage-javascript-sdk.next.protocols :as pr]
            [cxengage-javascript-sdk.next.pubsub :as pu]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.next.authentication :as authentication]
            [cxengage-javascript-sdk.next.session :as session]
            [cxengage-javascript-sdk.next.errors :as e]
            [cxengage-javascript-sdk.internal-utils :as iu]))

(defn register-module [module]
  (let [{:keys [api topics module-name]} module
        cxengage (aget js/window "serenova" "cxengage")
        new-topics (into (js->clj (aget cxengage "topics"))
                         (map #(str "cxengage/" (name module-name) "/" %) topics))
        new-api (merge (js->clj (aget cxengage "api")) {module-name api})]
    (js/console.log (str "Registering " (name module-name) " module"))
    (aset cxengage "topics" (clj->js new-topics))
    (aset cxengage "api" (clj->js (transform-keys k/->camelCase new-api)))))

(defn start-external-module [module]
  (.start module (clj->js (state/get-config))))

(defn start-internal-module [module]
  (pr/start module))

(defn gen-new-initial-module-config []
  {:config (state/get-config) :state (atom {})})

(defn start-required-modules []
  (let [auth-module (authentication/map->Module. (gen-new-initial-module-config))
        session-module (session/map->Module. (gen-new-initial-module-config))]
    (doseq [module [auth-module session-module]]
      (start-internal-module module))))

(s/def ::base-url string?)
(s/def ::consumer-type #{:js :cljs})
(s/def ::log-level #{"debug" "info" "warn" "error" "fatal" "off"})
(s/def ::initialize-options
  (s/keys :req-un [::base-url]
          :opt-un [::consumer-type ::log-level]))

(defn ^:export initialize
  ([] (clj->js (e/wrong-number-of-args-error)))
  ([options & rest] (clj->js (e/wrong-number-of-args-error)))
  ([options]
   (let [options (iu/extract-params options)]
     (if-not (s/valid? ::initialize-options options)
       (clj->js (e/invalid-args-error (s/explain-data ::initialize-options options)))
       (let [log-level (or (:log-level options) :info)
             consumer-type (or (:consumer-type options) :js)
             base-api-url (get options :base-url)
             core {:topics []
                   :api {:subscribe pu/subscribe
                         :publish pu/publish}
                   :modules {:register register-module
                             :start start-external-module}}]
         (state/set-base-api-url! base-api-url)
         (state/set-consumer-type! consumer-type)
         (state/set-log-level! log-level)
         (-> (aset js/window "serenova" #js {})
             (aset "cxengage" (clj->js (transform-keys k/->camelCase core))))
         (start-required-modules)
         (aget js/window "serenova"))))))
