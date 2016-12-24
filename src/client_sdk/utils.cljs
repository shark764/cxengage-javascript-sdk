(ns client-sdk.utils
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as a]
            [ajax.core :as ajax]))

(defn start-simple-consumer!
  [ch handler]
  (go-loop []
    (when-let [message (a/<! ch)]
      (handler message)
      (recur))))

(defn api-request [request-map]
  (let [{:keys [method url body token resp-chan]} request-map
        request (merge {:uri url
                        :method method
                        :timeout 30000
                        :handler (fn ajax-handler
                                   [[ok? response]]
                                   (a/put! resp-chan response))
                        :format (ajax/json-request-format)
                        :response-format (ajax/json-response-format {:keywords? true})}
                       (when body
                         {:params body})
                       (when token
                         {:headers {"Authorization" (str "Token " token)}}))]
    (ajax/ajax-request request)))
