(ns client-sdk.domain.errors)

(defn invalid-params-err
  ([] (invalid-params-err nil))
  ([msg]
   {:response nil
    :error {:code 15000
            :msg (or msg "Invalid arguments passed to SDK function.")}}))
