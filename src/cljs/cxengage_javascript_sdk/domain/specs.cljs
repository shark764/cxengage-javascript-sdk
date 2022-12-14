(ns cxengage-javascript-sdk.domain.specs
  (:require [cljs.spec.alpha :as s]
            [cljs-uuid-utils.core :as id]))

(s/def ::uuid id/valid-uuid?)
(s/def ::name (s/or :name string? :name nil?))
(s/def ::address string?)
(s/def ::active boolean?)
(s/def ::active-tab map?)
(s/def ::agent-id ::uuid)
(s/def ::recipient (s/keys :req-un [::address] :opt-un [::name]))
(s/def ::type #{"pstn" "sip" "webrtc"})
(s/def ::crm-module #{:salesforce-classic :salesforce-lightning :zendesk :none})
(s/def ::device-ids (s/or
                      :device-ids string?
                      :device-ids (s/coll-of string?)))
(s/def ::value string?)
(s/def ::channel-type #{"voice" "sms" "email" "messaging" "work-item" "any"})
(s/def ::answers map?)
(s/def ::artifact-file-id ::uuid)
(s/def ::outbound-identifier-id id/valid-uuid?)
(s/def ::outbound-identifier-list-id id/valid-uuid?)
(s/def ::sla-abandon-type #{"ignored-abandoned-calls" "count-against-sla"})
(s/def ::sla-abandon-threshold (s/or :sla-abandon-threshold number? :sla-abandon-threshold nil?))
(s/def ::sla-id id/valid-uuid?)
(s/def ::active-sla (s/or :active-sla (s/keys :req-un [::version-name ::version-description ::sla-threshold ::abandon-type ::abandon-threshold]) :active-sla nil?))
(s/def ::version-name (s/or :version-name string? :version-name nil?))
(s/def ::version-description (s/or :version-description string? :version-description nil?))
(s/def ::sla-threshold (s/or :sla-threshold number? :sla-threshold nil?))
(s/def ::abandon-type (s/or :abandon-type #{"ignore-abandons" "count-against-sla"} :abandon-type nil?))
(s/def ::abandon-threshold (s/or :abandon-threshold number? :abandon-threshold nil?))
(s/def ::data-access-report-id id/valid-uuid?)
(s/def ::report-type #{"realtime" "historical"})
(s/def ::realtime-report-type (s/or :realtime-report-type #{"standard" "custom"} :realtime-report-type nil?))
(s/def ::realtime-report-name (s/or :realtime-report-name string? :realtime-report-name nil?))
(s/def ::historical-catalog-name (s/or :historical-catalog-name string? :historical-catalog-name nil?))
(s/def ::skill-id id/valid-uuid?)
(s/def ::dispatch-mapping-id id/valid-uuid?)
(s/def ::proficiency number?)
(s/def ::has-proficiency boolean?)
(s/def ::group-id id/valid-uuid?)
(s/def ::user-id id/valid-uuid?)
(s/def ::role-id id/valid-uuid?)
(s/def ::entity-id id/valid-uuid?)
(s/def ::entity-name string?)
(s/def ::sub-entity-name string?)
(s/def ::platform-role-id id/valid-uuid?)
(s/def ::default-identity-provider (s/or :default-identity-provider id/valid-uuid? :default-identity-provider nil?))
(s/def ::capacity-rule-id (s/or :capacity-rule-id id/valid-uuid? :capacity-rule-id nil?))
(s/def ::extensions map?)
(s/def ::email string?)
(s/def ::no-password (s/or :no-password boolean? :no-password nil?))
(s/def ::status string?)
(s/def ::work-station-id (s/or :work-station-id string? :work-station-id nil?))
(s/def ::external-id (s/or :external-id string? :external-id nil?))
(s/def ::first-name (s/or :first-name string? :first-name nil?))
(s/def ::last-name (s/or :last-name string? :last-name nil?))
(s/def ::flow-id id/valid-uuid?)
(s/def ::draft-id id/valid-uuid?)
(s/def ::flow (s/or :flow string? :flow nil?))
(s/def ::flow-type #{"customer" "resource" "reusable"})
(s/def ::metadata string?)
(s/def ::active-version ::uuid)
(s/def ::version (s/or :version id/valid-uuid? :version nil?))
(s/def ::exclude-notations boolean?)
(s/def ::include-drafts boolean?)
(s/def ::minutes-day (s/int-in -1 1441))
(s/def ::artifact-id ::uuid)
(s/def ::assign-type #{"contact" "relatedTo"})
(s/def ::attachment-id ::uuid)
(s/def ::attributes map?)
(s/def ::base-url string?)
(s/def ::bcc (s/or
              :empty (s/and vector? empty?)
              :recipient (s/coll-of ::recipient)))
(s/def ::blast-sqs-output boolean?)
(s/def ::body any?)
(s/def ::callback (s/or :callback fn? :callback nil?))
(s/def ::cc (s/or
             :empty (s/and vector? empty?)
             :recipient (s/coll-of ::recipient)))
(s/def ::contact-id ::uuid)
(s/def ::contact-ids (s/coll-of ::contact-id))
(s/def ::member-ids (s/coll-of ::uuid))
(s/def ::users (s/coll-of ::uuid))
(s/def ::permissions (s/coll-of ::uuid))
(s/def ::contact-point string?)
(s/def ::crm string?)
;; TODO: Create an actual spec for dates on JSON format
(s/def ::date string?)
(s/def ::description (s/or ::description nil? ::description string?))
(s/def ::digit #{"0" "1" "2" "3" "4" "5" "6" "7" "8" "9" "*" "#"})
(s/def ::direction #{"inbound" "outbound" "agent-initiated"})
(s/def ::dismissed boolean?)
(s/def ::disposition-id ::uuid)
(s/def ::disposition-obj (s/keys :req-un [::disposition-id ::sort-order ::hierarchy]))
;;TODO: add the proper spec for Disposition List Dispositions, not allowing empty objects
(s/def ::dispositions (s/or
                        :empty (s/and vector? empty?)
                        :disposition-obj (s/coll-of ::disposition-obj)))
(s/def ::disposition-list-id id/valid-uuid?)
(s/def ::email-type-id ::uuid)
(s/def ::environment #{:dev :qe :staging :prod :us-east-1-test})
(s/def ::end-time-minutes ::minutes-day)
(s/def ::exception (s/keys :req-un [::date ::is-all-day ::start-time-minutes ::end-time-minutes]))
(s/def ::exceptions (s/coll-of ::exception))
(s/def ::extension-id string?)
(s/def ::extension-value (s/or ::uuid string?))
(s/def ::exclude-inactive boolean?)
(s/def ::exclude-offline boolean?)
(s/def ::exception-id id/valid-uuid?)
(s/def ::exit-reason #{"user-submitted" "script-timeout" "script-auto-dismissed"})
(s/def ::fallback boolean?)
(s/def ::file any?)
(s/def ::height number?)
(s/def ::force-logout boolean?)
(s/def ::html-body string?)
(s/def ::id (s/or :id number? :id string?))
(s/def ::idp-id ::uuid)
(s/def ::interaction-id ::uuid)
(s/def ::chosen-extension (s/keys :req-un [::description ::type ::value]))
(s/def ::interaction-field #{"customer" "contact-point" "source" "direction"})
(s/def ::interrupt-type string?)
(s/def ::interrupt-body map?)
(s/def ::integration-id id/valid-uuid?)
(s/def ::listener-id id/valid-uuid?)
(s/def ::integration-type #{"rest" "salesforce" "zendesk" "calabrio"})
(s/def ::is-all-day boolean?)
(s/def ::properties (s/or :properties map? :properties nil?))
(s/def ::items (s/coll-of map?))
(s/def ::update-body (s/coll-of map?))
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
(s/def ::no-state-reset boolean?)
(s/def ::outbound-ani string?)
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
(s/def ::reason-id id/valid-uuid?)
(s/def ::reason-obj (s/keys :req-un [::reason-id ::sort-order ::hierarchy]))
;;TODO: add the proper spec for Reason List Reasons, not allowing empty objects
(s/def ::reasons (s/or
                  :empty (s/and vector? empty?)
                  :reason-obj (s/coll-of ::reason-obj)))
(s/def ::is-default boolean?)
(s/def ::reason-list-id id/valid-uuid?)
(s/def ::reason-info
  (s/keys :req-un [::reason ::reason-id ::reason-list-id]))
(s/def ::realtime-report-id ::uuid)
(s/def ::resource-id ::uuid)
(s/def ::script-id ::uuid)
(s/def ::session-id ::uuid)
(s/def ::shared boolean?)
(s/def ::silent-monitoring boolean?)
(s/def ::stat-id (s/or ::uuid string?))
(s/def ::stat-ids (s/coll-of ::stat-id))
(s/def ::state string?)
(s/def ::statistic string?)
(s/def ::stats map?)
(s/def ::stat-query
  (s/keys :req-un [::statistic]
          :opt-un [::queue-id ::resource-id ::stat-id]))
(s/def ::start-time-minutes ::minutes-day)
(s/def ::subject string?)
(s/def ::subscription-id ::uuid)
(s/def ::sub-type string?)
(s/def ::tenant-id ::uuid)
(s/def ::title string?)
(s/def ::to (s/coll-of ::recipient))
(s/def ::token string?)
(s/def ::topic string?)
(s/def ::custom-topic string?)
(s/def ::target-resource-id ::uuid)
(s/def ::transfer-extension (s/keys :req-un [::type ::value]))
(s/def ::transfer-type #{"cold" "warm"})
(s/def ::transfer-list-id ::uuid)
(s/def ::endpoints vector?)
(s/def ::trigger-batch boolean?)
(s/def ::ttl number?)
(s/def ::username string?)
(s/def ::msg-type #{:js :cljs})
(s/def ::update-body map?)
(s/def ::visibility boolean?)
(s/def ::width number?)
(s/def ::without-active-dashboard boolean?)
(s/def ::wrapup string?)
(s/def ::enable-indicator boolean?)
(s/def ::business-hour-id id/valid-uuid?)
(s/def ::message-template-id ::uuid)
(s/def ::channels vector?)
(s/def ::template-text-type #{"plaintext" "html"})
(s/def ::template string?)
(s/def ::timezone string?)
;; TODO: Write an actual spec for this one
(s/def ::time-minutes map?)
(s/def ::typing boolean?)
(s/def ::agent-message-id string?)
(s/def ::api-version string?)
(s/def ::admin-user-id id/valid-uuid?)
(s/def ::parent-id id/valid-uuid?)
(s/def ::region-id id/valid-uuid?)
(s/def ::outbound-integration-id (s/or :outbound-integration-id id/valid-uuid? :outbound-integration-id nil?))
(s/def ::cxengage-identity-provider string?)
(s/def ::default-sla-id (s/or :default-sla-id id/valid-uuid? :default-sla-id nil?))
(s/def ::image-type string?)
(s/def ::styles string?)
(s/def ::platform-entity boolean?)
(s/def ::logo string?)
(s/def ::favicon string?)
(s/def ::interaction-metadata map?)
