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

(defn active-tenant-not-set-err
 ([] (active-tenant-not-set-err nil))
 ([msg]
  (let [code 2000
        msg (or msg "Active Tenant has not been selected.")
        msg-str (str code ": " msg)]
    (js/console.error msg-str)
    (error-response code msg))))

(defn invalid-presence-state-err
  ([] (invalid-presence-state-err nil))
  ([msg]
   (let [code 3000
         msg (or msg "Agent is not in correct state.")
         msg-str (str code ": " msg)]
     (js/console.error msg-str)
     (error-response code msg))))

(defn interaction-not-found-err
 ([] (interaction-not-found-err nil))
 ([msg]
  (let [code 4000
        msg (or msg "Interaction was not found.")
        msg-str (str code ": " msg)]
    (js/console.error msg-str)
    (error-response code msg))))

(defn session-not-started-err
 ([] (session-not-started-err nil))
 ([msg]
  (let [code 5000
        msg (or msg "Session has not been started.")
        msg-str (str code ": " msg)]
    (js/console.error msg-str)
    (error-response code msg))))
