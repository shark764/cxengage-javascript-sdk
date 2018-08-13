---
inject: true
to: src/cljs/cxengage_javascript_sdk/domain/errors.cljs
before: ;;hygen-insert-before-11000s
---
(defn failed-to-<%= apiType %>-<%= kebabName %>-err [data]
  {:code INSERT_ERROR_CODE_HERE
  :context :entities
  :data {:api-response data}
  :level "error"
  :message "Failed to <%= apiType %> <%= normalName %>."})
