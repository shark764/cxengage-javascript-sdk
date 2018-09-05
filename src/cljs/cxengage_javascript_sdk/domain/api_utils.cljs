(ns cxengage-javascript-sdk.domain.api-utils
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [lumbajack.macros :refer [log]])
  (:require [cxengage-javascript-sdk.domain.interop-helpers :as ih]
            [lumbajack.core :as jack]
            [ajax.core :as ajax]
            [cljs.core.async :as a]))

(defn normalize-response-stucture
  [[ok? response] preserve-casing? third-party-request?]
  (if (and (false? ok?)
           (= (:status response) 200))
    {:api-response nil :status 200}
    (let [status (if ok? 200 (get response :status))
          response (if (or preserve-casing? third-party-request?) response (ih/kebabify response))
          api-response (if (map? response)
                         (dissoc response :status)
                         response)]
      {:api-response api-response :status status})))

(defn server-error?
  [status]
  (and (>= status 500)
       (< status 600)))

(defn update-local-time-offset
  [response]
  (when-let [timestamp (get-in response [:api-response :result :timestamp])]
    (let [local (js/Date.now)
          server (js/Date.parse timestamp)
          offset (- local server)]
      (ih/set-time-offset! offset))))

(defn api-request
  [request-map skip-retries]
  (let [resp-chan (a/promise-chan)]
    (go-loop [failed-attempts 0]
      (let [response-channel (a/promise-chan)
            {:keys [method url body preserve-casing? third-party-request? authless-request? csv-download?]} request-map
            request (merge {:uri url
                            :method method
                            :timeout 120000
                            :handler #(let [normalized-response (normalize-response-stucture % preserve-casing? third-party-request?)]
                                        (update-local-time-offset normalized-response)
                                        (a/put! response-channel normalized-response))
                            :format (ajax/json-request-format)
                            :response-format (if (or third-party-request? csv-download?)
                                               (ajax/text-response-format)
                                               (ajax/json-response-format {:keywords? true}))}
                           (when body
                             {:params (if preserve-casing? body (ih/camelify body))})
                           (when-let [token (ih/get-token)]
                             (if (or third-party-request? authless-request?)
                               {}
                               {:headers {"Authorization" (str "Token " token)}})))]
        (ajax/ajax-request request)
        (let [response (a/<! response-channel)
              {:keys [status]} response]
          (if (and (server-error? status) (< failed-attempts 3))
            (if (not skip-retries)
              (do
                (log :error (str "Received server error " status " retrying in " (* 3 failed-attempts) " seconds."))
                (a/<! (a/timeout (* 3000 (+ 1 failed-attempts))))
                (recur (inc failed-attempts)))
              (a/put! resp-chan response))
            (a/put! resp-chan response)))))
    resp-chan))
