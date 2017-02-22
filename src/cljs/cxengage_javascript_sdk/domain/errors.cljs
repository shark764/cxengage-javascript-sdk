(ns cxengage-javascript-sdk.domain.errors)

(defn error [code msg]
  (js/console.info (str code ": " msg))
  (clj->js {:code code
            :message msg}))

(defn invalid-params-err
  ([] (invalid-params-err nil))
  ([msg]
   (let [code 1000
         msg (or msg "Invalid arguments passed to SDK function.")]
     (error code msg))))

(defn invalid-sdk-state-err
  ([] (invalid-sdk-state-err nil))
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

(defn sdk-request-error
  ([] (sdk-request-error nil))
  ([msg]
   (let [code 1300
         msg (or msg "SDK failed to retrieve requested information or state from server.")]
     (error code msg))))

(defn insufficient-permissions-err
  ([] (insufficient-permissions-err nil))
  ([msg]
   (let [code 1400
         msg (or msg "Resource does not have the sufficient permissions.")]
     (error code msg))))
