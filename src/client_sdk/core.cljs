(ns client-sdk.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :as a]
            [auth-sdk.core :as auth]
            [presence-sdk.core :as pres]))

(enable-console-print!)

(defonce sdk-state
  (atom {}))

(defn init-module [sdk-state module-name module-state]
  (assoc sdk-state module-name module-state))

(defn ^:export init []
  (-> @sdk-state
      (init-module :auth (auth/init))))

(defn login)
(defn change-presence-state)
