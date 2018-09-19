(ns cxengage-javascript-sdk.domain.macros)

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
            topic# (if (string? topic-key#)
                    topic-key#
                    (cxengage-javascript-sdk.domain.topics/get-topic topic-key#))
            args# (map #(cxengage-javascript-sdk.domain.interop-helpers/extract-params % preserve-casing?#) args#)
            callback# (if (fn? (first args#)) (first args#) (second args#))]
        (if (> (count args#) 2)
          (cxengage-javascript-sdk.domain.interop-helpers/publish {:topics topic#
                                                                   :error (cxengage-javascript-sdk.domain.errors/wrong-number-of-sdk-fn-args-err)
                                                                   :callback callback#})
          (let [params# (if (fn? (first args#))
                          {:callback (first args#)}
                          (if (map? (first args#))
                            (first args#)
                            {}))
                params# (-> params#
                            (assoc :callback callback#)
                            (assoc :topic topic#))
                ~'params params#]
            (if (not (cljs.spec.alpha/valid? validation# params#))
              (let [spec-explanation# (expound.alpha/expound-str validation# params#)]
                (cxengage-javascript-sdk.internal-utils/log-message :warn "The value you provided to the SDK fn did not satisfy the spec; check your values against the documentation and try again." spec-explanation#)
                (cxengage-javascript-sdk.domain.interop-helpers/publish {:topics topic#
                                                                         :error (cxengage-javascript-sdk.domain.errors/args-failed-spec-err spec-explanation#)
                                                                         :callback callback#}))
              (do (cljs.core.async.macros/go ~@body)
                  nil))))))))

(defmacro log
  "Log macro for deferring argument evaluation"
  [level & data]
  `(if-not (some #{~level} (keys cxengage-javascript-sdk.internal-utils/levels))
     (js/console.error "Invalid logging level specified, unable to log message.")
     (when (and (aget js/window "CxEngage")
                (aget js/window "CxEngage" "logging")
                (aget js/window "CxEngage" "logging" "level"))
       (cxengage-javascript-sdk.internal-utils/log-message ~level ~@data))))
