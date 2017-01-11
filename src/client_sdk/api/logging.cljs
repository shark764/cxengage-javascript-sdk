(ns client-sdk.api.logging
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [client-sdk.state :as state]
            [client-sdk.api.helpers :as h]))

(defn set-level
  ([params callback]
   (set-level (merge params {:callback callback})))
  ([params]
   (let [module-chan (state/get-module-chan :logging)
         response-chan (a/promise-chan)
         {:keys [level callback]} (h/extract-params params)
         msg (merge (h/base-module-request :LOGGING/SET_LEVEL response-chan)
                    {:level (keyword level)})]
     (a/put! module-chan msg)
     (go (a/<! response-chan)
         (when callback (callback))))))
