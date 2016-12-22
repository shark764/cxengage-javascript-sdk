(ns client-sdk.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :as a]
            [clojure.string :as str]
            [lumbajack.core :as logging :refer [log]]
            [client-sdk.state :as state]
            [client-sdk.api :as api]
            [auth-sdk.core :as auth]
            [presence-sdk.core :as presence]))

(enable-console-print!)

(defn register-module [sdk-state module-name module]
  (swap! sdk-state assoc-in [:module-channels module-name] module)
  sdk-state)

(defn ^:export init []
  (logging/init)
  (-> (state/get-state)
      (register-module :logging (logging/init))
      (register-module :auth (auth/init))
      (register-module :presence (presence/init)))
  api/api)
