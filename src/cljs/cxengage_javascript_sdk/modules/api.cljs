(ns cxengage-javascript-sdk.modules.api
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]])
  (:require [cljs.spec.alpha :as s]
            [cljs.core.async :as a]
            [clojure.set :refer [rename-keys]]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.state :as state]
            [cxengage-javascript-sdk.internal-utils :as iu]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]))

(s/def ::create-params
  (s/keys :req-un [::specs/path ::specs/body]
          :opt-un [::specs/callback ::specs/custom-topic]))

(def-sdk-fn create
  "CRUD api create request.
  ``` javascript
  CxEngage.api.create({
    path: {{array}} (required) Example Array ['groups', '0000-0000-0000-0000', 'outboundIdentifierLists']
    body: {{object}} (required)
    customTopic: {{string}} (optional)
  });
  ```
  A generic method for creating an api entity.

  Possible Errors:

  - [Api: 11084](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-err)"
  {:validation ::create-params
   :topic-key :create-response}
  [params]
  (let [{:keys [path body callback topic custom-topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/api-create-request (into [] path) body))
        topic (or custom-topic topic)
        error (if-not (= status 200) (e/failed-to-create-err api-response))]
      (p/publish {:topics topic
                  :response api-response
                  :error error
                  :callback callback})))

(s/def ::read-params
  (s/keys :req-un [::specs/path]
          :opt-un [::specs/callback ::specs/custom-topic]))

(def-sdk-fn read
  "CRUD api get request.
  ``` javascript
  CxEngage.api.read({
    path: {{array}} (required) Example Array ['groups', '0000-0000-0000-0000', 'outboundIdentifierLists']
    customTopic: {{string}} (optional)
  });
  ```
  A generic method of retrieving an api endpoint.

  Topic: cxengage/api/read-response

  - [Api Documentation](https://api-docs.cxengage.net/Rest/Default.htm#Introduction/Intro.htm)

  Possible Errors:

  - [Api: 11085](/cxengage-javascript-sdk.domain.errors.html#var-failed-read-err)"
  {:validation ::read-params
    :topic-key :read-response}
  [params]
  (let [{:keys [path callback topic custom-topic]} params]
      (let [{:keys [status api-response]} (a/<! (rest/api-read-request (into [] path)))
            topic (or custom-topic topic)
            error (if-not (= status 200) (e/failed-to-read-err api-response))]
          (p/publish
            {:topics topic
              :response api-response
              :error error
              :callback callback}))))


(s/def ::update-params
  (s/keys :req-un [::specs/path ::specs/body]
          :opt-un [::specs/callback ::specs/custom-topic]))

(def-sdk-fn update
  "CRUD api update request.
  ``` javascript
  CxEngage.api.update({
    path: {{array}} (required) Example Array ['groups', '0000-0000-0000-0000', 'outboundIdentifierLists']
    body: {{object}} (required)
    customTopic: {{string}} (optional)
  });
  ```
  A generic method for updating an api entity.

  Possible Errors:

  - [Api: 11086](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-err)"
  {:validation ::update-params
    :topic-key :update-response}
  [params]
  (let [{:keys [path body callback topic custom-topic]} params
        {:keys [status api-response] :as api-response} (a/<! (rest/api-update-request (into [] path) body))
        topic (or custom-topic topic)
        error (if-not (= status 200) (e/failed-to-update-err api-response))]
      (p/publish {:topics topic
                  :response api-response
                  :error error
                  :callback callback})))


(s/def ::delete-params
  (s/keys :req-un [::specs/path]
          :opt-un [::specs/callback ::specs/custom-topic]))

(def-sdk-fn delete
  "CRUD api delete request.
  ``` javascript
  CxEngage.api.delete({
    path: {{array}} (required) Example Array ['groups', '0000-0000-0000-0000', 'outboundIdentifierLists']
    customTopic: {{string}} (optional)
  });
  ```
  A generic method for deleting an api entity.

  Possible Errors:

  - [Api: 11087](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-delete-err)"
  {:validation ::delete-params
    :topic-key :delete-response}
  [params]
  (let [{:keys [path callback topic custom-topic]} params
        {:keys [status api-response] :as api-response} (a/<! (rest/api-delete-request (into [] path)))
        topic (or custom-topic topic)
        error (if-not (= status 200) (e/failed-to-delete-err api-response))]
      (p/publish {:topics topic
                  :response api-response
                  :error error
                  :callback callback})))

(defrecord ApiModule []
  pr/SDKModule
  (start [this]
    (let [module-name :api]
      (ih/register {:api {module-name {:create create
                                       :read read
                                       :update update
                                       :delete delete}}
                    :module-name module-name})
      (ih/send-core-message {:type :module-registration-status
                             :status :success
                             :module-name module-name})))
  (stop [this])
  (refresh-integration [this]))