(ns client-sdk.domain.specs
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [lumbajack.macros :refer [log]])
  (:require [cljs.core.async :as a]
            [cljs.spec :as s]
            [client-sdk.state :as state]
            [client-sdk.api.helpers :as h]))

(s/def ::tenantId string?)
(s/def ::topic string?)
(s/def ::interactionId string?)
(s/def ::state string?)
(s/def ::callback (s/or :callback fn?
                        :callback nil?))
(s/def ::username string?)
(s/def ::password string?)
(s/def ::message string?)
