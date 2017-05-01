(ns blah #_cxengage-javascript-sdk.modules.authentication-test
    #_(:require-macros [cljs.core.async.macros :refer [go]])
    #_(:require [cxengage-javascript-sdk.modules.authentication :as auth]
                [cljs.test :refer-macros [deftest is testing async use-fixtures]]
                [cxengage-javascript-sdk.pubsub :as p]
                [cxengage-javascript-sdk.internal-utils :as iu]
                [cxengage-javascript-sdk.core :as core]
                [ajax.core :as ajax]
                [cljs.core.async :as a]
                [cxengage-javascript-sdk.state :as state]))

#_(deftest login-test
    (testing "The login function happy and sad path"
      (async done
             (go
               (let [the-chan (a/promise-chan)
                     _ (a/>! the-chan {:status 200
                                       :api-response {:result {:platform-role-name "Platform Admin"
                                                               :tenants "blahblahblah"}
                                                      :token "blah"}})
                     old iu/api-request]

                 (let [AuthModule (auth/map->AuthenticationModule. (core/gen-new-initial-module-config (a/chan)))
                       _ (state/reset-state)
                       _ (set! iu/api-request (fn [request-map]
                                                (let [{:keys [method url body] :as params} request-map]
                                                  the-chan)))]
                   (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (auth/login AuthModule)))
                   (is (= {:code 1000 :error "Incorrect number of arguments passed to SDK fn."} (auth/login AuthModule {} {})))
                   (let [_ (p/subscribe "cxengage" (fn [error topic response]
                                                     (cond
                                                       (= topic "cxengage/authentication/login-response") (is (= {:platformRoleName "Platform Admin" :tenants "blahblahblah"} (js->clj response :keywordize-keys true)))
                                                       (= topic "cxengage/session/tenant-list") (is (= "blahblahblah" response)))))]
                     (auth/login AuthModule {:username "blah"
                                             :password "blah"})
                     (set! iu/api-request old)
                     (done))))))))
