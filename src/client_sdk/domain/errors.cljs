(ns client-sdk.domain.errors)

(defn error [code msg]
  (js/console.error (str code ": " msg))
  {:code code
   :message msg})

(defn invalid-params-err
  ([] (invalid-params-err nil))
  ([msg]
   (let [code 1000
         msg (or msg "Invalid arguments passed to SDK function.")]
     (error code msg))))

(defn invalid-sdk-state-err
  ([] (invalid-state-err nil))
  ([msg]
   (let [code 1100
         msg (or msg "SDK is in an invalid state to perform this action.")]
     (error code msg))))

(defn subscription-topic-not-recognized-err
  ([] (subscription-topic-not-recognized-err nil))
  ([msg]
   (let [code 1200
         msg (or msg "Unknown subscription topic.")]
     (error code msg))))
