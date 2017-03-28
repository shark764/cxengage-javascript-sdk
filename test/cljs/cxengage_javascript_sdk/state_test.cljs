(ns cxengage-javascript-sdk.state-test
  (:require [cxengage-javascript-sdk.state :as state]
            [cljs.test :refer-macros [deftest is testing run-tests async use-fixtures]]
            [cljs-uuid-utils.core :as u]
            [cljs.core.async :as a]
            [clojure.set :as set]))

#_(deftest core-mutators-test
    (testing "The first few functions in the state namespace"
      (let [_ (state/reset-state)
            chan (:async-module-registration @state/sdk-state)
            fixture {:authentication {}
                     :user {}
                     :session {}
                     :config {}
                     :interactions {:pending {}
                                    :active {}
                                    :past {}}
                     :logs {:unsaved-logs []
                            :saved-logs []
                            :valid-levels [:debug :info :warn :error :fatal]}}]
        (is (= fixture @state/sdk-state))
        (is (= fixture (state/get-state)))
        (is (= (assoc-in fixture [:config :env] "dev") (state/set-env! "dev")))
        (is (= "dev" (state/get-env)))
        (state/set-consumer-type! :js)
        (is (= :js (state/get-consumer-type)))
        (is (= (-> fixture
                   (assoc-in [:config :env] "dev")
                   (assoc-in [:config :consumer-type] :cljs)) (state/set-consumer-type! :cljs)))
        (is (= :cljs (state/get-consumer-type))))))

#_(deftest state-interactions-test
    (testing "The functions for interactions specific state and relevant predicate"
      (let [_ (state/reset-state)
            chan (:async-module-registration @state/sdk-state)
            fixture {:authentication {}
                     :user {}
                     :session {}
                     :config {}
                     :interactions {:pending {}
                                    :active {}
                                    :past {}}
                     :logs {:unsaved-logs []
                            :saved-logs []
                            :valid-levels [:debug :info :warn :error :fatal]}}
            pending-interaction-id (u/make-random-uuid)
            pending-interaction-map {:interaction-id pending-interaction-id}
            active-interaction-id (u/make-random-uuid)
            active-interaction-map {:interaction-id active-interaction-id}
            past-interaction-id (u/make-random-uuid)
            past-interaction-map {:interaction-id past-interaction-id}]
        (is (= (assoc-in fixture [:interactions :pending pending-interaction-id] pending-interaction-map) (state/add-interaction! :pending pending-interaction-map)))
        (is (= (-> fixture
                   (assoc-in [:interactions :pending pending-interaction-id] pending-interaction-map)
                   (assoc-in [:interactions :active active-interaction-id] active-interaction-map)) (state/add-interaction! :active active-interaction-map)))
        (is (= (-> fixture
                   (assoc-in [:interactions :pending pending-interaction-id] pending-interaction-map)
                   (assoc-in [:interactions :active active-interaction-id] active-interaction-map)
                   (assoc-in [:interactions :past past-interaction-id] past-interaction-map)) (state/add-interaction! :past past-interaction-map)))
        (is (= {:pending {pending-interaction-id pending-interaction-map}
                :active {active-interaction-id active-interaction-map}
                :past {past-interaction-id past-interaction-map}} (state/get-all-interactions)))
        (is (= {active-interaction-id active-interaction-map} (state/get-all-active-interactions)))
        (is (= {pending-interaction-id pending-interaction-map} (state/get-all-pending-interactions)))
        (is (= active-interaction-map (state/get-active-interaction active-interaction-id)))
        (is (= pending-interaction-map (state/get-pending-interaction pending-interaction-id)))
        (is (= :active (state/find-interaction-location active-interaction-id)))
        (is (= :pending (state/find-interaction-location pending-interaction-id)))
        (is (= :past (state/find-interaction-location past-interaction-id)))
        (is (nil? (state/find-interaction-location (u/make-random-uuid))))
        (is (= active-interaction-map (state/get-interaction active-interaction-id)))
        (is (= (-> fixture
                   (assoc-in [:interactions :pending pending-interaction-id] pending-interaction-map)
                   (assoc-in [:interactions :active active-interaction-id] (merge active-interaction-map {:message-history [{:from "blah"
                                                                                                                             :to "blahblah"
                                                                                                                             :body "blahblahblah"}]}))
                   (assoc-in [:interactions :past past-interaction-id] past-interaction-map)) (state/add-messages-to-history! active-interaction-id [{:payload {:from "blah"
                                                                                                                                                                :to "blahblah"
                                                                                                                                                                :body "blahblahblah"}}])))
        (is (= (-> fixture
                   (assoc-in [:interactions :pending pending-interaction-id] pending-interaction-map)
                   (assoc-in [:interactions :active active-interaction-id] (merge active-interaction-map {:message-history [{:from "blah"
                                                                                                                             :to "blahblah"
                                                                                                                             :body "blahblahblah"}]} {:custom-field-details "Unit test"}))
                   (assoc-in [:interactions :past past-interaction-id] past-interaction-map)) (state/add-interaction-custom-field-details! "Unit test" active-interaction-id)))
        (is (= (-> fixture
                   (assoc-in [:interactions :pending pending-interaction-id] pending-interaction-map)
                   (assoc-in [:interactions :active active-interaction-id] (merge active-interaction-map {:message-history [{:from "blah"
                                                                                                                             :to "blahblah"
                                                                                                                             :body "blahblahblah"}]} {:custom-field-details "Unit test"} {:wrapup-details {:wrapup "Disabled"}}))
                   (assoc-in [:interactions :past past-interaction-id] past-interaction-map)) (state/add-interaction-wrapup-details! {:wrapup "Disabled"} active-interaction-id)))
        (is (= (-> fixture
                   (assoc-in [:interactions :pending pending-interaction-id] pending-interaction-map)
                   (assoc-in [:interactions :active active-interaction-id] (merge active-interaction-map {:message-history [{:from "blah"
                                                                                                                             :to "blahblah"
                                                                                                                             :body "blahblahblah"}]} {:custom-field-details "Unit test"} {:wrapup-details {:wrapup "Disabled"}} {:disposition-code-details {:reason "Lunch"}}))
                   (assoc-in [:interactions :past past-interaction-id] past-interaction-map)) (state/add-interaction-disposition-code-details! {:reason "Lunch"} active-interaction-id)))
        (is (= (-> fixture
                   (assoc-in [:interactions :pending pending-interaction-id] pending-interaction-map)
                   (assoc-in [:interactions :past active-interaction-id] (merge active-interaction-map {:message-history [{:from "blah"
                                                                                                                           :to "blahblah"
                                                                                                                           :body "blahblahblah"}]} {:custom-field-details "Unit test"} {:wrapup-details {:wrapup "Disabled"}} {:disposition-code-details {:reason "Lunch"}}))
                   (assoc-in [:interactions :past past-interaction-id] past-interaction-map)) (state/transition-interaction! :active :past active-interaction-id)))
        (is (= past-interaction-map (state/interaction-exists-in-state? :past past-interaction-id))))))

#_(deftest state-auth-test
    (testing "the few stateful auth functions"
      (let [_ (state/reset-state)
            token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyLWlkIjoiODEyZTE0ZjAtYmJlMy0xMWU2LTgyMWYtZTg5ODAwM2YzNDExIiwidGVuYW50LWlkIjoiOTJmNTBmMzAtNjYxYy0xMWU2LWIxYjktY2E4MTQ4NDQ4OGRmIiwidHlwZSI6InRva2VuIiwiZXhwIjoxNDg3NjE5NTAzLCJpYXQiOjE0ODc2MTk1MDJ9.-JNEAwZfWGyNC_VMc0Q8W4BLud0HoSMdIj7ci8hp0s0"
            fixture {:authentication {:token token}
                     :user {}
                     :session {}
                     :config {}
                     :interactions {:pending {}
                                    :active {}
                                    :past {}}
                     :logs {:unsaved-logs []
                            :saved-logs []
                            :valid-levels [:debug :info :warn :error :fatal]}}]
        (is (= fixture (state/set-token! token)))
        (is (= token (state/get-token))))))

#_(deftest state-identity-test
    (testing "the few stateful identity functions"
      (let [_ (state/reset-state)
            user-id (u/make-random-uuid)
            identity-map {:user-id user-id}
            fixture {:authentication {}
                     :user {}
                     :session {}
                     :config {}
                     :interactions {:pending {}
                                    :active {}
                                    :past {}}
                     :logs {:unsaved-logs []
                            :saved-logs []
                            :valid-levels [:debug :info :warn :error :fatal]}}]
        (is (= (assoc fixture :user identity-map) (state/set-user-identity! identity-map)))
        (is (= user-id (state/get-active-user-id))))))

#_(deftest state-session-test
    (testing "the stateful session functions and relevant predicates"
      (let [_ (state/reset-state)
            sessionId (u/make-random-uuid)
            tenant-id (u/make-random-uuid)
            region "us-east-1"
            direction "outbound"
            session {:session-id sessionId
                     :region region
                     :direction direction
                     :tenant-id tenant-id}
            config {:tenant-id tenant-id
                    :outboundIntegration
                    {:id tenant-id
                     :tenant-id tenant-id
                     :name "twilio"
                     :type "twilio"
                     :active true}
                    :activeExtension
                    {:description "Default Twilio extension"
                     :provider "twilio"
                     :region "default"
                     :type "webrtc"}}
            session-config (merge session config)
            session-direction (merge session-config {:direction "inbound"})
            session-capacity (merge session-direction {:capacity 5})
            session-state (merge session-capacity {:state "notready"})
            fixture {:authentication {}
                     :user {}
                     :session {}
                     :config {}
                     :interactions {:pending {}
                                    :active {}
                                    :past {}}
                     :logs {:unsaved-logs []
                            :saved-logs []
                            :valid-levels [:debug :info :warn :error :fatal]}}]
        (is (= (assoc fixture :session session) (state/set-session-details! session)))
        (is (= session (state/get-session-details)))
        (is (= (-> fixture
                   (assoc :session session)
                   (assoc-in [:session :config] config)) (state/set-config! config)))
        (is (= (-> fixture
                   (assoc :session session)
                   (assoc-in [:session :config] config)) (state/set-active-tenant! tenant-id)))
        (is (= tenant-id (state/get-active-tenant-id)))
        (is (= region (state/get-active-tenant-region)))
        (is (= (-> fixture
                   (assoc :session session)
                   (assoc-in [:session :config] config)
                   (assoc-in [:session :direction] "inbound")) (state/set-direction! "inbound")))
        (is (= sessionId (state/get-session-id)))
        (is (= (-> fixture
                   (assoc :session session)
                   (assoc-in [:session :config] config)
                   (assoc-in [:session :direction] "inbound")
                   (assoc-in [:session :capacity] 5)) (state/set-capacity! 5)))
        (is (= (-> fixture
                   (assoc :session session)
                   (assoc-in [:session :config] config)
                   (assoc-in [:session :direction] "inbound")
                   (assoc-in [:session :capacity] 5)
                   (assoc-in [:session :state] "notready")) (state/set-user-session-state! {:state "notready"})))
        (is (= "notready" (state/get-user-session-state)))
        (is (= sessionId (state/session-started?)))
        (is (= tenant-id (state/active-tenant-set?)))
        (is (true? (state/presence-state-matches? "notready")))
        (is (false? (state/presence-state-matches? "ready"))))))

#_(deftest state-twilio-test
    (testing "the stateful twilio functions"
      (let [_ (state/reset-state)
            interal {:twilio-device "microphone"}
            interal-connection (merge interal {:twilio-connection "active"})
            fixture {:authentication {}
                     :user {}
                     :session {}
                     :config {}
                     :interactions {:pending {}
                                    :active {}
                                    :past {}}
                     :logs {:unsaved-logs []
                            :saved-logs []
                            :valid-levels [:debug :info :warn :error :fatal]}
                     :interal {:twilio-device "microphone"}}]
        (is (= fixture (state/set-twilio-device "microphone")))
        (is (= (assoc-in fixture [:interal :twilio-connection] "active") (state/set-twilio-connection "active")))
        (is (= "microphone" (state/get-twilio-device)))
        (is (= "active" (state/get-twilio-connection))))))

#_(deftest state-log-test
    (testing "The Stateful log utility functions"
      (let [_ (state/reset-state)
            fixture {:authentication {}
                     :user {}
                     :session {}
                     :config {:log-level :info}
                     :interactions {:pending {}
                                    :active {}
                                    :past {}}
                     :logs {:unsaved-logs []
                            :saved-logs []
                            :valid-levels [:debug :info :warn :error :fatal]}}]
        (is (= (get-in fixture [:logs :valid-levels]) (state/get-valid-log-levels)))
        (is (= (assoc-in fixture [:logs :valid-levels] '(:fatal :error :warn :info)) (state/set-log-level! :info {:debug "" :info "" :warn "" :error "" :fatal ""})))
        (is (= '(:fatal :error :warn :info) (state/get-valid-log-levels)))
        (is (= (assoc-in fixture [:logs :valid-levels] '(:fatal)) (state/set-valid-log-levels! '(:fatal))))
        (is (= (-> fixture
                   (assoc-in [:logs :valid-level] '(:fatal))
                   (assoc-in [:logs :unsaved-logs] [{:level :fatal
                                                     :message "Son!"}]) (state/append-logs! {:level :fatal
                                                                                             :message "Son!"}))))
        (is (= (-> fixture
                   (assoc-in [:logs :valid-level] '(fatal))
                   (assoc-in [:logs :saved-logs] [{:level :fatal
                                                   :message "Son!"}])) (state/save-logs))))))
