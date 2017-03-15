(ns cxengage-javascript-sdk.domain.errors)

(defn wrong-number-of-args-error []
  {:code 1000 :error "Incorrect number of arguments passed to SDK fn."})
(defn missing-required-permissions-error []
  {:code 1003 :error "Missing required permissions"})
(defn invalid-args-error [spec-explanation]
  {:code 1001 :error "Invalid arguments passed to SDK fn." :data spec-explanation})
(defn api-error [error]
  {:code 1002 :error "API returned an error."})
(defn no-entity-found-for-specified-id [entity entity-id]
  {:code 1004 :error (str "No " entity " found by that ID") :data entity-id})
(defn incorrect-disposition-selected []
  {:code 4001 :error "No disposition found by that ID"})
(defn no-microphone-access-error [err]
  {:code 7000 :error (str "No access to microphone: " (.-message err))})
(defn not-a-valid-extension []
  {:code 5000 :error "that isn't a valid extension."})
(defn invalid-logging-level-specified-error []
  {:code 9001 :error "Invalid logging level specified."})
