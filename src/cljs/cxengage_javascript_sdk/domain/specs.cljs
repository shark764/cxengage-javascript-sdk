(ns cxengage-javascript-sdk.domain.specs
  (:require [cljs.spec :as s]
            [cljs-uuid-utils.core :as id]))

(s/def ::direction #{"inbound" "outbound"})
(s/def ::uuid id/valid-uuid?)
(s/def ::tenantId ::uuid)
(s/def ::tenant-id ::uuid)
(s/def ::interactionId ::uuid)
(s/def ::extensionId string?)
(s/def ::extension-id ::uuid)
(s/def ::queue-id ::uuid)
(s/def ::resource-id ::uuid)
(s/def ::transfer-type #{"cold" "warm"})
(s/def ::contactId ::uuid)
(s/def ::contact-id ::uuid)
(s/def ::layoutId ::uuid)
(s/def ::layout-id ::uuid)
(s/def ::subscription-id ::uuid)
(s/def ::topic string?)
(s/def ::state string?)
(s/def ::phoneNumber string?)
(s/def ::phone-number string?)
(s/def ::callback (s/or :callback fn?
                        :callback nil?))
(s/def ::username string?)
(s/def ::password string?)
(s/def ::message string?)
(s/def ::query (s/coll-of (s/or :keyword keyword?
                                :string string?)))
(s/def ::attributes map?)
(s/def ::wrapup string?)

(s/def ::level #{"debug" "info" "warn" "error" "fatal"})
