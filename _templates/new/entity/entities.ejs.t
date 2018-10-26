---
inject: true
to: src/cljs/cxengage_javascript_sdk/modules/entities.cljs
after: ;;hygen-insert-before-<%= apiType %>
---

(s/def ::<%= apiType %>-<%= kebabName %>-params
  (s/keys :req-un [ <%= reqSpecParams %>]
          :opt-un [ ::specs/callback <%= optSpecParams %>]))

(def-sdk-fn <%= apiType %>-<%= kebabName %>
  "``` javascript
  CxEngage.entities.<%= apiType %><%= functionName %>({
    <%= docParams -%>
  });
  ```
  Calls rest/<%= apiType %>-<%= kebabName %>-request
  with the provided data for current tenant.

  Topic: cxengage/entities/<%= apiType %>-<%= kebabName %>-response

  Possible Errors:

  - [Entities: INSERT_ERROR_CODE_HERE](/cxengage-javascript-sdk.domain.errors.html#var-failed-to-<%= apiType %>-<%= kebabName %>-err)"
  {:validation ::<%= apiType %>-<%= kebabName %>-params
   :topic-key :<%= apiType %>-<%= kebabName %>-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/<%= pluralCheck? 'crud-entities-request' : 'crud-entity-request' %> :<%= apiType === 'update'? 'put' : apiType %> "<%= pluralCheck? kebabNameNoLastLetter : kebabName %>" <%= (!pluralCheck && apiType === 'get')? kebabNameNoLastLetter + 'id' : null %>))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-<%= apiType %>-<%= kebabName %>-err entity-response)
                  :callback callback}))))
