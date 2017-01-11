(ns client-sdk.domain.errors)

(defn invalid-params-err
  ([] (invalid-params-err nil))
  ([msg]
   {:error-code 15000
    :error-msg (or msg "Invalid arguments passed to SDK function.")}))
