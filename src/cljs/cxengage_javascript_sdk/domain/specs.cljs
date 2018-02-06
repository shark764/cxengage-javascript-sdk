(ns cxengage-javascript-sdk.domain.specs
  (:require [cljs.spec.alpha :as s]
            [cljs-uuid-utils.core :as id]))

(s/def ::uuid id/valid-uuid?)
(s/def ::name string?)
(s/def ::address string?)
(s/def ::active boolean?)
(s/def ::active-tab map?)
(s/def ::recipient
  (s/keys :req-un [::address ::name]))
(s/def ::type #{"pstn" "sip"})
(s/def ::crm-module #{:salesforce-classic :salesforce-lightning :zendesk :none})
(s/def ::value string?)
(s/def ::answers map?)
(s/def ::artifact-file-id ::uuid)
(s/def ::artifact-id ::uuid)
(s/def ::assign-type #{"contact" "relatedTo"})
(s/def ::attachment-id ::uuid)
(s/def ::attributes map?)
(s/def ::base-url string?)
(s/def ::bcc (s/or
              :empty (s/and vector? empty?)
              :recipient (s/coll-of ::recipient)))
(s/def ::blast-sqs-output boolean?)
(s/def ::body string?)
(s/def ::callback (s/or :callback fn? :callback nil?))
(s/def ::cc (s/or
             :empty (s/and vector? empty?)
             :recipient (s/coll-of ::recipient)))
(s/def ::contact-id ::uuid)
(s/def ::contact-ids (s/coll-of ::contact-id))
(s/def ::contact-point string?)
(s/def ::crm string?)
(s/def ::description string?)
(s/def ::digit #{"0" "1" "2" "3" "4" "5" "6" "7" "8" "9" "*" "#"})
(s/def ::direction #{"inbound" "outbound"})
(s/def ::disposition-id ::uuid)
(s/def ::environment #{:dev :qe :staging :prod})
(s/def ::extension-id string?)
(s/def ::extension-value (s/or ::uuid string?))
(s/def ::exclude-inactive boolean?)
(s/def ::exclude-offline boolean?)
(s/def ::height number?)
(s/def ::html-body string?)
(s/def ::id (s/or :id number? :id string?))
(s/def ::idp-id ::uuid)
(s/def ::interaction-id ::uuid)
(s/def ::interrupt-type string?)
(s/def ::interrupt-body map?)
(s/def ::items (s/coll-of map?))
(s/def ::item-value map?)
(s/def ::layout-id ::uuid)
(s/def ::level #{"debug" "info" "warn" "error" "fatal"})
(s/def ::list-id ::uuid)
(s/def ::list-item-key string?)
(s/def ::list-type-id ::uuid)
(s/def ::locale string?)
(s/def ::log-level #{:debug :info :warn :error :fatal :off})
(s/def ::message string?)
(s/def ::note-id ::uuid)
(s/def ::no-session boolean?)
(s/def ::page number?)
(s/def ::password string?)
(s/def ::phone-number string?)
(s/def ::plain-text-body string?)
(s/def ::query map?)
(s/def ::queue-id ::uuid)
(s/def ::reason string?)
(s/def ::reason-id ::uuid)
(s/def ::reason-list-id ::uuid)
(s/def ::reason-info
  (s/keys :req-un [::reason ::reason-id ::reason-list-id]))
(s/def ::resource-id ::uuid)
(s/def ::script-id ::uuid)
(s/def ::stat-id ::uuid)
(s/def ::state string?)
(s/def ::statistic string?)
(s/def ::stats map?)
(s/def ::subject string?)
(s/def ::subscription-id ::uuid)
(s/def ::sub-type string?)
(s/def ::tenant-id ::uuid)
(s/def ::title string?)
(s/def ::to (s/coll-of ::recipient))
(s/def ::token string?)
(s/def ::topic string?)
(s/def ::target-resource-id ::uuid)
(s/def ::transfer-extension (s/keys :req-un [::type ::value]))
(s/def ::transfer-type #{"cold" "warm"})
(s/def ::transfer-list-id ::uuid)
(s/def ::ttl number?)
(s/def ::username string?)
(s/def ::msg-type #{:js :cljs})
(s/def ::update-body map?)
(s/def ::visibility boolean?)
(s/def ::width number?)
(s/def ::wrapup string?)
