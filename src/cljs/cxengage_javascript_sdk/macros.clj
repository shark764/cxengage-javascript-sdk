(ns cxengage-javascript-sdk.macros)

(defmacro def-sdk-fn
  "Boilerplate-reducing macro intended to save time and reduce errors in writing new SDK API functions.
  Automatically handles:
  - optional javascript callback function arity
  - validation of javascript object spec
  - converting camelCase javascript objects to kebab-case clojure maps
  - wrapping the SDK API function in a (go) block to cover core/async use
  - publishing any generic errors (spec validation, arity errors) to the SDK pub/sub system
  - returning nil to prevent core/async channel leakage
  - providing the SDK API fn with a simple params map to work with
  - merging optional callback function into said params map."
  [name spec topic _ & body]
  `(defn ~name
     ([& args#]
      (let [args# (map cxengage-javascript-sdk.interop-helpers/extract-params args#)
            callback# (second args#)]
        (if-let [error# (cond

                          (or (> (count args#) 2))
                          (cxengage-javascript-sdk.domain.errors/wrong-number-of-sdk-fn-args-err)

                          (and (first args#)
                               (not (map? (first args#))))
                          (cxengage-javascript-sdk.domain.errors/params-isnt-a-map-err)

                          (and (not (nil? callback#))
                               (not (fn? callback#)))
                          (cxengage-javascript-sdk.domain.errors/callback-isnt-a-function-err)

                          :else false)]
          (cxengage-javascript-sdk.pubsub/publish {:topics ~topic
                                                   :error error#
                                                   :callback callback#})
          (let [params# (first args#)
                params# (-> params#
                            (assoc :callback (second args#))
                            (assoc :topic ~topic))
                ~'params params#]
            (if (not (cljs.spec/valid? ~spec params#))
              (do (js/console.info "Params object failed spec validation: " (cljs.spec/explain-data ~spec params#))
                  (cxengage-javascript-sdk.pubsub/publish {:topics ~topic
                                                           :error (cxengage-javascript-sdk.domain.errors/args-failed-spec-err)
                                                           :callback callback#}))
              (do (cljs.core.async.macros/go ~@body)
                  nil))))))))
