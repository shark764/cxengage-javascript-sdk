(ns cxengage-javascript-sdk.next.errors
  (:require [cljs.spec :as s]))

(defn wrong-number-of-args-error [] {:code 1000 :error "Incorrect number of arguments passed to SDK fn."})
(defn missing-required-permissions-error [] {:code 1003 :error "Missing required permissions"})
(defn invalid-args-error [spec-explanation]
  (js/console.log spec-explanation)
  {:code 1001 :error "Invalid arguments passed to SDK fn."})
(defn api-error [error] (merge {:code 1002 :error "API returned an error."} error))
