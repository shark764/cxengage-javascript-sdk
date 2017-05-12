(ns cxengage-javascript-sdk.domain.rest-requests
  (:require [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cljs.core.async :as a]))

(defn get-config-request []
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        config-request {:method :get
                        :url (iu/api-url
                              "tenants/tenant-id/users/resource-id/config"
                              {:tenant-id tenant-id
                               :resource-id resource-id})}]
    (iu/api-request config-request)))

(defn update-user-request [update-user-body]
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        update-user-request {:method :put
                             :url (iu/api-url
                                   "tenants/tenant-id/users/resource-id"
                                   {:tenant-id tenant-id
                                    :resource-id resource-id})
                             :body update-user-body}]
    (iu/api-request update-user-request)))

(defn change-state-request [change-state-body]
  (let [resource-id (state/get-active-user-id)
        tenant-id (state/get-active-tenant-id)
        change-state-request {:method :post
                              :url (iu/api-url
                                    "tenants/tenant-id/presence/resource-id"
                                    {:tenant-id tenant-id
                                     :resource-id resource-id})
                              :body change-state-body}]
    (iu/api-request change-state-request)))
