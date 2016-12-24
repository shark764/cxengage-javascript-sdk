(ns client-sdk.api.logging
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [lumbajack.core :refer [log]]
            [client-sdk.state :as state]
            [client-sdk.api.helpers :as h]))

(defn set-log-level-handler [module-chan response-chan params]
  (let [{:keys [level callback]} (h/extract-params params)
        msg (merge (h/base-module-request :LOGGING/SET_LEVEL response-chan)
                   {:level (keyword level)})]
    (a/put! module-chan msg)
    (go (a/<! response-chan)
        (when callback (callback)))))

(defn api [] {:setLogLevel (partial set-log-level-handler
                                    (state/get-module-chan :logging)
                                    (a/promise-chan))})
