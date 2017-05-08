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
      (let [args# (map ih/extract-params args#)
            callback# (second args#)]
        (if-let [error# (cond

                          (or (> (count args#) 2))
                          (e/wrong-number-of-sdk-fn-args-err)

                          (and (first args#)
                               (not (map? (first args#))))
                          (e/invalid-args-error)

                          (and (not (nil? callback#))
                               (not (fn? callback#)))
                          (e/invalid-args-error)

                          :else false)]
          (p/publish {:topics ~topic
                      :error error#
                      :callback (if (fn? callback#) callback# nil)})
          (let [params# (first args#)
                params# (-> params#
                            (assoc :callback (second args#))
                            (assoc :topic ~topic))
                ~'params params#]
            (if (not (s/valid? ~spec params#))
              (do (js/console.info "Params object failed spec validation: " (s/explain-data ~spec params#))
                  (p/publish {:topics ~topic
                              :error (e/invalid-args-error)
                              :callback callback#}))
              (do (cljs.core.async.macros/go ~@body)
                  nil))))))))
