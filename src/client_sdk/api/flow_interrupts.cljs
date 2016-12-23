(ns client-sdk.api.flow-interrupts
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as a]
            [client-sdk.state :as state]))

(defn interrupt-handler [params callback]
   (let [{:keys [interrupt interruptType]} params
         interrupt-result-chan (a/promise-chan)
         interrupt-msg {:resp-chan interrupt-result-chan
                        :token (state/get-token)
                        :tenant-id (state/get-active-tenant-id)
                        :interaction-id nil #_(state/get-interaction-id)
                        :interrupt-details interrupt
                        :interrupt-type interruptType}
         interrupt-chan (state/get-module-chan :flow)]
      (a/put! interrupt-chan interrupt-msg)
      (go (let [{:keys [result]} (a/<! interrupt-result-chan)]
           (callback)))))

(def api {:sendInterrupt nil #_login-handler})
