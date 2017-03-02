(ns cxengage-javascript-sdk.pubsub)

(defn subscribe [])
(defn publish
  ([t v]
   (publish t v nil))
  ([t v c]
   (js/console.warn (str "[PUBSUB]: " t) v)
   (when c (c v))))
