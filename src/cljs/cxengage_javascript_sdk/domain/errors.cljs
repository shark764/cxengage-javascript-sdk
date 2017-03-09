(ns cxengage-javascript-sdk.domain.errors)

(defn error-output [error]
  (js/console.error ">>>ERROR<<<" (clj->js (:error error)) (clj->js (:data error)))
  error)

(defn wrong-number-of-args-error [] (error-output {:code 1000 :error "Incorrect number of arguments passed to SDK fn."}))
(defn missing-required-permissions-error [] (error-output {:code 1003 :error "Missing required permissions"}))
(defn invalid-args-error [spec-explanation]
  (error-output {:code 1001 :error "Invalid arguments passed to SDK fn." :data spec-explanation}))
(defn api-error [error] (merge (error-output {:code 1002 :error "API returned an error."}) error))
(defn no-entity-found-for-specified-id [entity entity-id]
  (error-output {:code 1004 :error (str "No " entity " found by that ID") :data entity-id}))
(defn no-microphone-access-error [err]
  (error-output {:code 7000 :error (str "No access to microphone: " (.-message err))}))
(defn not-a-valid-extension [] {:code 5000 :error "that isn't a valid extension."})
(defn invalid-logging-level-specified-error [] "Invalid logging level specified.")
