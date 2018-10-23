---
inject: true
to: src/cljs/cxengage_javascript_sdk/domain/errors.cljs
before: ;;hygen-insert-before-11000s
---
(defn failed-to-<%= apiType %>-<%= kebabName %>-err
  "**Error Code:** INSERT_ERROR_CODE_HERE
   Message: Failed to <%= apiType %> <%= normalName %>.

   This error is usually due to an unexpected status code returned from the API.

   **Solution:** Check network tab for additional error information."
  [data]
  {:code INSERT_ERROR_CODE_HERE
  :context :entities
  :data {:api-response data}
  :level "error"
  :message "Failed to <%= apiType %> <%= normalName %>."})
