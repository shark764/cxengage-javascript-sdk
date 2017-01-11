(ns client-sdk.domain.errors)

(defn invalid-params-err
  ([] (invalid-params-err nil))
  ([msg]
   (clj->js {:response nil
             :error {:code 15000
                     :message (or msg "Invalid arguments passed to SDK function.")}})))
