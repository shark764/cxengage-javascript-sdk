(ns cxengage-javascript-sdk.next.pubsub)

(defn subscribe [])
(defn publish
  ([t v]
   (publish t v nil))
  ([t v c]
   (js/console.warn (str "[PUBSUB]: " t) v)))
