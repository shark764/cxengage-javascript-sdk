(ns cxengage-javascript-sdk.test-macros)

(defmacro with-reset
  [bindings & body]
  (let [names (take-nth 2 bindings)
        vals (take-nth 2 (drop 1 bindings))
        current-vals (map #(list 'identity %) names)
        tempnames (map (comp gensym name) names)
        binds (map vector names vals)
        resets (reverse (map vector names tempnames))
        bind-value (fn [[k v]] (list 'set! k v))]
    `(let [~@(interleave tempnames current-vals)]
       (try
         ~@(map bind-value binds)
         ~@body
         (finally
           ~@(map bind-value resets))))))
