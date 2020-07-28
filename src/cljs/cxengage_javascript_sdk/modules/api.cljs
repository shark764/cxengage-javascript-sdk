(ns cxengage-javascript-sdk.modules.api
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [cxengage-javascript-sdk.domain.macros :refer [def-sdk-fn log]])
  (:require [cljs.spec.alpha :as s]
            [cljs.core.async :as a]
            [clojure.set :refer [rename-keys]]
            [cxengage-javascript-sdk.domain.protocols :as pr]
            [cxengage-javascript-sdk.domain.errors :as e]
            [cxengage-javascript-sdk.pubsub :as p]
            [cxengage-javascript-sdk.domain.specs :as specs]
            [cxengage-javascript-sdk.domain.rest-requests :as rest]
            [cxengage-javascript-sdk.domain.topics :as topics]
            [cxengage-javascript-sdk.domain.interop-helpers :as ih]))

(s/def ::create-params
  (s/keys :req-un [::specs/path ::specs/body]
          :opt-un [::specs/callback ::specs/custom-topic ::specs/api-version]))

(def-sdk-fn create
  "CRUD api create request.
  ``` javascript
  CxEngage.api.create({
    path: {{array}} (required) Example Array ['groups', '0000-0000-0000-0000', 'outboundIdentifierLists']
    body: {{object}} (required)
    customTopic: {{string}} (optional)
    apiVersion: {{string}} (optional) (If this attribute is passed, it will be able to override the default api version used to make the
                                       requests, set up when initializing the SDK, to any existing version on the platform.
                                       E.g., 'v2', 'v3'. etc)
  });
  ```
  A generic method for creating an api entity.

  Possible Errors:

  - [Api: 11098](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-create-err)"
  {:validation ::create-params
   :topic-key :create-response}
  [params]
  (let [{:keys [path body callback topic custom-topic api-version]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/api-create-request (into [] path) body api-version))
        topic (or custom-topic topic)
        error (if-not (= status 200) (e/failed-to-create-err api-response))]
      (p/publish {:topics topic
                  :response api-response
                  :error error
                  :preserve-casing? (ih/is-entity-request-and-response-preserve-casing (first path))
                  :callback callback})))

(s/def ::read-params
  (s/keys :req-un [::specs/path]
          :opt-un [::specs/callback ::specs/custom-topic ::specs/api-version ::specs/tenant-id ::specs/platform-entity]))

(def-sdk-fn read
  "CRUD api get request.
  ``` javascript
  CxEngage.api.read({
    path: {{array}} (required) Example Array ['groups', '0000-0000-0000-0000', 'outboundIdentifierLists']
    customTopic: {{string}} (optional)
    apiVersion: {{string}} (optional) (If this attribute is passed, it will be able to override the default api version used to make the
                                       requests, set up when initializing the SDK, to any existing version on the platform.
                                       E.g., 'v2', 'v3'. etc)
    tenantId: {{string}} (optional)  (In the Tenants page, we need to fetch entities data that belong to a different tenant.)
    platformEntity: {{boolean}} (optional) (Entities like regions & timezones are platform-level entities, which needs to fetched without sending tenantId in the request)
  });
  ```
  A generic method of retrieving an api endpoint.

  Topic: cxengage/api/read-response

  - [Api Documentation](https://api-docs.cxengage.net/Rest/Default.htm#Introduction/Intro.htm)

  Possible Errors:

  - [Api: 11099](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-read-err)"
  {:validation ::read-params
    :topic-key :read-response}
  [params]
  (let [{:keys [path callback topic custom-topic api-version tenant-id platform-entity]} params
        {:keys [status api-response]} (a/<! (rest/api-read-request (into [] path) api-version tenant-id platform-entity))
        topic (or custom-topic topic)
        error (if-not (= status 200) (e/failed-to-read-err api-response))]
    (p/publish
     {:topics topic
      :response api-response
      :error error
      :preserve-casing? (ih/is-entity-request-and-response-preserve-casing (first path))
      :callback callback})))


(s/def ::update-params
  (s/keys :req-un [::specs/path ::specs/body]
          :opt-un [::specs/callback ::specs/custom-topic ::specs/api-version]))

(def-sdk-fn update
  "CRUD api update request.
  ``` javascript
  CxEngage.api.update({
    path: {{array}} (required) Example Array ['groups', '0000-0000-0000-0000', 'outboundIdentifierLists']
    body: {{object}} (required)
    customTopic: {{string}} (optional)
    apiVersion: {{string}} (optional) (If this attribute is passed, it will be able to override the default api version used to make the
                                       requests, set up when initializing the SDK, to any existing version on the platform.
                                       E.g., 'v2', 'v3'. etc)
  });
  ```
  A generic method for updating an api entity.

  Possible Errors:

  - [Api: 11100](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-update-err)"
  {:validation ::update-params
    :topic-key :update-response}
  [params]
  (let [{:keys [path body callback topic custom-topic api-version]} params
        {:keys [status api-response] :as api-response} (a/<! (rest/api-update-request (into [] path) body api-version))
        topic (or custom-topic topic)
        error (if-not (= status 200) (e/failed-to-update-err api-response))]
      (p/publish {:topics topic
                  :response api-response
                  :error error
                  :preserve-casing? (ih/is-entity-request-and-response-preserve-casing (first path))
                  :callback callback})))

(s/def ::delete-params
  (s/keys :req-un [::specs/path]
          :opt-un [::specs/callback ::specs/custom-topic ::specs/api-version]))

(def-sdk-fn delete
  "CRUD api delete request.
  ``` javascript
  CxEngage.api.delete({
    path: {{array}} (required) Example Array ['groups', '0000-0000-0000-0000', 'outboundIdentifierLists']
    customTopic: {{string}} (optional)
    apiVersion: {{string}} (optional) (If this attribute is passed, it will be able to override the default api version used to make the
                                       requests, set up when initializing the SDK, to any existing API version on the platform.
                                       E.g., 'v2', 'v3'. etc)
  });
  ```
  A generic method for deleting an api entity.

  Possible Errors:

  - [Api: 11101](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-delete-err)"
  {:validation ::delete-params
    :topic-key :delete-response}
  [params]
  (let [{:keys [path callback topic custom-topic api-version]} params
        {:keys [status api-response] :as api-response} (a/<! (rest/api-delete-request (into [] path) api-version))
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
