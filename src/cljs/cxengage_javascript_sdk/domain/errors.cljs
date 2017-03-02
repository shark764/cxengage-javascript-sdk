(ns cxengage-javascript-sdk.domain.errors)

(defn error [error]
  (js/console.error ">>>ERROR<<<" (clj->js (:error error)) (clj->js (:data error)))
  error)

(defn wrong-number-of-args-error [] (error {:code 1000 :error "Incorrect number of arguments passed to SDK fn."}))
(defn missing-required-permissions-error [] (error {:code 1003 :error "Missing required permissions"}))
(defn invalid-args-error [spec-explanation]
  (error {:code 1001 :error "Invalid arguments passed to SDK fn." :data spec-explanation}))
(defn api-error [error] (merge (error {:code 1002 :error "API returned an error."} error)))
(defn no-entity-found-for-specified-id [entity entity-id]
  (error {:code 1004 :error (str "No " entity " found by that ID") :data entity-id}))
