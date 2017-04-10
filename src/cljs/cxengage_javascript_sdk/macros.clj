(ns cxengage-javascript-sdk.macros)

(defmacro def-sdk-fn [name spec topic _ & body]
  `(defn ~name
     ([& args#]
      (let [args# (map iu/extract-params args#)
            callback# (second args#)]
        (if-let [error# (cond

                          (or (> (count args#) 2))
                          (e/wrong-number-of-args-error)

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
