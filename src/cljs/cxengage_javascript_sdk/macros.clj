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
  [name options _ & body]
  `(defn ~name
     ([& args#]
      (let [validation# (:validation ~options)
            topic-key# (:topic-key ~options)
            preserve-casing?# (or (:preserve-casing? ~options) false)
            topic# (cxengage-javascript-sdk.pubsub/get-topic topic-key#)
            args# (map #(cxengage-javascript-sdk.interop-helpers/extract-params % preserve-casing?#) args#)
            callback# (if (fn? (first args#)) (first args#) (second args#))]
        (if-let [error# (cond

                          (or (> (count args#) 2))
                          (cxengage-javascript-sdk.domain.errors/wrong-number-of-sdk-fn-args-err)

                          :else false)]
          (cxengage-javascript-sdk.pubsub/publish {:topics topic#
                                                   :error error#
                                                   :callback callback#})
          (let [params# (if (fn? (first args#)) {:callback (first args#)} (first args#))
                params# (-> params#
                            (assoc :callback callback#)
                            (assoc :topic topic#))
                ~'params params#]
            (if (not (cljs.spec/valid? validation# params#))
              (do (js/console.warn "Params object failed spec validation." (s/explain-data validation# params#))
                  (cxengage-javascript-sdk.pubsub/publish {:topics topic#
                                                           :error (cxengage-javascript-sdk.domain.errors/args-failed-spec-err)
                                                           :callback callback#}))
              (do (cljs.core.async.macros/go ~@body)
                  nil))))))))
