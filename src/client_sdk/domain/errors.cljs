(ns client-sdk.domain.errors)

(defn error-response [code msg]
  (clj->js {:response nil
            :error {:code code
                    :message msg}}))

(defn invalid-params-err
  ([] (invalid-params-err nil))
  ([msg]
   (let [code 1000
         msg (or msg "Invalid arguments passed to SDK function.")
         msg-str (str code ": " msg)]
     (js/console.error msg-str)
     (error-response code msg))))
