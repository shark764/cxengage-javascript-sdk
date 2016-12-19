(ns client-sdk.core
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as a]
            [clojure.string :as str]
            [camel-snake-kebab.core :as c]
            [camel-snake-kebab.extras :as e]))

(enable-console-print!)

(def internal-state (atom {}))

(def presence-topics
  {:resource-state-change (a/chan)})

(def interaction-topics
  {:work-offer (a/chan)})

(swap! internal-state assoc-in [:topics :presence] presence-topics)
(swap! internal-state assoc-in [:topics :interactions] interaction-topics)

(defn send-msg-to-topic [topic msg]
  (-> (let [topic-path (into [:topics] (mapv keyword (str/split topic "/")))
            topic-chan (get-in @internal-state topic-path)]
        (go-loop []
          (a/put! topic-chan msg)))
      (a/close!)))

(defn subscribe-to-topic [topic handler]
  (let [topic-path (into [:topics] (mapv keyword (str/split topic "/")))]
    (go-loop []
      (if-let [topic-chan (get-in @internal-state topic-path)]
        (when-let [msg (a/<! topic-chan)]
          (handler msg)
          (recur))
        (do (js/console.warn "That topic wasn't found in the SDK!")
            nil)))))

(defn ^:export init []
  (clj->js {:subscribeToTopic subscribe-to-topic}))

(defn on-js-reload [])
