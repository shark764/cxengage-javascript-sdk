(ns cxengage-javascript-sdk.domain.specs
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [cljs-uuid-utils.core :as id]))

(s/def ::uuid id/valid-uuid?)
(s/def ::tenantId ::uuid)
(s/def ::interactionId ::uuid)
(s/def ::contactId ::uuid)
(s/def ::layoutId ::uuid)
(s/def ::topic string?)
(s/def ::state string?)
(s/def ::phoneNumber string?)
(s/def ::callback (s/or :callback fn?
                        :callback nil?))
(s/def ::username string?)
(s/def ::password string?)
(s/def ::message string?)
(s/def ::query (s/coll-of (s/or :keyword keyword?
                                :string string?)))
(s/def ::attributes map?)
