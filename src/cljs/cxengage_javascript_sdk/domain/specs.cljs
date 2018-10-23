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
(s/def ::channel-type #{"voice" "sms" "email"})
(s/def ::answers map?)
(s/def ::artifact-file-id ::uuid)
(s/def ::outbound-identifier-id id/valid-uuid?)
(s/def ::custom-metric-id id/valid-uuid?)
(s/def ::sla-threshold number?)
(s/def ::custom-metrics-type #{"SLA"})
(s/def ::sla-abandon-type #{"ignored-abandoned-calls" "count-against-sla"})
(s/def ::sla-abandon-threshold (s/or :sla-abandon-threshold number? :sla-abandon-threshold nil?))
(s/def ::data-access-report-id id/valid-uuid?)
(s/def ::report-type #{"realtime" "historical"})
(s/def ::realtime-report-type (s/or :realtime-report-type #{"standard" "custom"} :realtime-report-type nil?))
(s/def ::realtime-report-name (s/or :realtime-report-name string? :realtime-report-name nil?))
(s/def ::historical-catalog-name (s/or :historical-catalog-name string? :historical-catalog-name nil?))
(s/def ::skill-id id/valid-uuid?)
(s/def ::has-proficiency boolean?)
(s/def ::group-id id/valid-uuid?)
(s/def ::user-id id/valid-uuid?)
(s/def ::role-id id/valid-uuid?)
(s/def ::default-identity-provider id/valid-uuid?)
(s/def ::capacity-rule-id id/valid-uuid?)
(s/def ::extensions map?)
(s/def ::email string?)
(s/def ::no-password boolean?)
(s/def ::status string?)
(s/def ::work-station-id string?)
(s/def ::external-id string?)
(s/def ::first-name string?)
(s/def ::last-name string?)
(s/def ::flow-id id/valid-uuid?)
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
(s/def ::member-ids (s/coll-of ::uuid))
(s/def ::contact-point string?)
(s/def ::crm string?)
(s/def ::description string?)
(s/def ::digit #{"0" "1" "2" "3" "4" "5" "6" "7" "8" "9" "*" "#"})
(s/def ::direction #{"inbound" "outbound" "agent-initiated"})
(s/def ::dismissed boolean?)
(s/def ::disposition-id ::uuid)
(s/def ::email-type-id ::uuid)
(s/def ::environment #{:dev :qe :staging :prod :us-east-1-test})
(s/def ::extension-id string?)
(s/def ::extension-value (s/or ::uuid string?))
(s/def ::exclude-inactive boolean?)
(s/def ::exclude-offline boolean?)
(s/def ::fallback boolean?)
(s/def ::file any?)
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
(s/def ::pop-uri string?)
(s/def ::query map?)
(s/def ::queries (s/coll-of ::stat-query))
(s/def ::queue-id ::uuid)
(s/def ::group-id ::uuid)
(s/def ::script-reporting boolean?)
(s/def ::skill-id ::uuid)
(s/def ::reason string?)
(s/def ::reason-id ::uuid)
(s/def ::reason-list-id ::uuid)
(s/def ::reason-info
  (s/keys :req-un [::reason ::reason-id ::reason-list-id]))
(s/def ::resource-id ::uuid)
(s/def ::script-id ::uuid)
(s/def ::shared boolean?)
(s/def ::silent-monitoring boolean?)
(s/def ::stat-id (s/or ::uuid string?))
(s/def ::state string?)
(s/def ::statistic string?)
(s/def ::stats map?)
(s/def ::stat-query
  (s/keys :req-un [::statistic]
          :opt-un [::queue-id ::resource-id]))
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
(s/def ::trigger-batch boolean?)
(s/def ::ttl number?)
(s/def ::username string?)
(s/def ::msg-type #{:js :cljs})
(s/def ::update-body map?)
(s/def ::visibility boolean?)
(s/def ::width number?)
(s/def ::wrapup string?)
