(ns cxengage-javascript-sdk.modules.messaging-test
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [clojure.string :as str])
  (:require [cxengage-javascript-sdk.modules.messaging :as messaging]
            [cljs-uuid-utils.core :as uuid]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.state :as state]
            [cljs.test :refer-macros [deftest is testing run-tests async]]))

(def mock-env :test)
(def mock-tenant-id (str (uuid/make-random-squuid)))
(def mock-user-id (str (uuid/make-random-squuid)))
(def mock-interaction-id (str (uuid/make-random-squuid)))
(def mock-interaction {:interaction-id mock-interaction-id
                       :tenant-id mock-tenant-id
                       :channel-type "Test"})

(defn mock-send-action-impl [_ _ action-topic callback]
  (let [pubsub-topic (topics/get-topic action-topic)]
    (println pubsub-topic)
    (go (p/publish {:topics pubsub-topic
                    :response true
                    :callback callback}))))

(state/set-active-tenant! mock-tenant-id)
(state/add-interaction! :pending mock-interaction)
(state/set-env! mock-env)
