---
inject: true
to: src/cljs/cxengage_javascript_sdk/modules/entities.cljs
after: ;;hygen-insert-before-<%= apiType %>
---

;; -------------------------------------------------------------------------- ;;
;; CxEngage.entities.<%= apiType %><%= functionName %>({
<%= docParams -%>
;;})
;; -------------------------------------------------------------------------- ;;

(s/def ::<%= apiType %>-<%= kebabName %>-params
  (s/keys :req-un [ <%= reqSpecParams %>]
          :opt-un [ ::specs/callback <%= optSpecParams %>]))

(def-sdk-fn <%= apiType %>-<%= kebabName %>
  {:validation ::<%= apiType %>-<%= kebabName %>-params
   :topic-key :<%= apiType %>-<%= kebabName %>-response}
  [params]
  (let [{:keys [callback topic]} params
        {:keys [status api-response] :as entity-response} (a/<! (rest/<%= pluralCheck? 'crud-entities-requests' : 'crud-entities-request' %> :<%= apiType === 'update'? 'put' : apiType %> "<%= pluralCheck? kebabNameNoLastLetter : kebabName %>"))]
    (if (= status 200)
      (p/publish {:topics topic
                  :response api-response
                  :callback callback})
      (p/publish {:topics topic
                  :error (e/failed-to-<%= apiType %>-<%= kebabName %>-err entity-response)
                  :callback callback}))))