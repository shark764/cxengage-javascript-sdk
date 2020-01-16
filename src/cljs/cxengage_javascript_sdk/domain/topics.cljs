(ns cxengage-javascript-sdk.domain.topics
  (:require-macros [cxengage-javascript-sdk.domain.macros :refer [log]])
  (:require [cxengage-javascript-sdk.internal-utils :as iu]))

(def sdk-topics {
                 ;; Authentication Topics
                 :login-response "cxengage/authentication/login-response"
                 :auth-info-response "cxengage/authentication/auth-info-response"
                 :cognito-auth-response "cxengage/authentication/cognito-auth-response"
                 :identity-window-response "cxengage/authentication/identity-window-response"
                 :update-default-tenant-response "cxengage/authentication/update-default-tenant-response"

                 ;; Session Topics
                 :active-tenant-set "cxengage/session/set-active-tenant-response"
                 :config-response "cxengage/session/config-details"
                 :set-presence-state-response "cxengage/session/set-presence-state-response"
                 :presence-state-changed "cxengage/session/state-change-response"
                 :presence-state-change-request-acknowledged "cxengage/session/state-change-request-acknowledged"
                 :presence-heartbeats-response "cxengage/session/heartbeat-response"
                 :session-started "cxengage/session/started"
                 :set-direction-response "cxengage/session/set-direction-response"
                 :direction-changed "cxengage/session/direction-change-response"
                 :extension-list "cxengage/session/extension-list"
                 :tenant-list "cxengage/session/tenant-list"
                 :session-ended "cxengage/session/ended"
                 :get-active-user-id-response "cxengage/session/get-active-user-id-response"
                 :get-active-tenant-id-response "cxengage/session/get-active-tenant-id-response"
                 :get-default-extension-response "cxengage/session/get-default-extension-response"
                 :get-token-response "cxengage/session/get-token-response"
                 :set-token-response "cxengage/session/set-token-response"
                 :set-user-identity-response "cxengage/session/set-user-identity-response"
                 :get-sso-token-response "cxengage/session/get-sso-token-response"
                 :set-locale-response "cxengage/session/set-locale-response"
                 :get-tenant-details "cxengage/session/get-tenant-details"
                 :sqs-shut-down "cxengage/session/sqs-shut-down"

                 ;; Contact topics
                 :get-contact "cxengage/contacts/get-contact-response"
                 :get-contacts "cxengage/contacts/get-contacts-response"
                 :search-contacts "cxengage/contacts/search-contacts-response"
                 :create-contact "cxengage/contacts/create-contact-response"
                 :update-contact "cxengage/contacts/update-contact-response"
                 :delete-contact "cxengage/contacts/delete-contact-response"
                 :merge-contacts "cxengage/contacts/merge-contacts-response"
                 :list-attributes "cxengage/contacts/list-attributes-response"
                 :get-layout "cxengage/contacts/get-layout-response"
                 :list-layouts "cxengage/contacts/list-layouts-response"

                 ;; CRUD-topics
                 :create-response "cxengage/api/create-response"
                 :read-response "cxengage/api/read-response"
                 :update-response "cxengage/api/update-response"
                 :delete-response "cxengage/api/delete-response"
                 :get-queue-response "cxengage/entities/get-queue-response"
                 :get-queues-response "cxengage/entities/get-queues-response"
                 :get-user-response "cxengage/entities/get-user-response"
                 :get-users-response "cxengage/entities/get-users-response"
                 :get-dashboards-response "cxengage/entities/get-dashboards-response"
                 :get-branding-response "cxengage/entities/get-branding-response"
                 :get-protected-branding-response "cxengage/entities/get-protected-branding-response"
                 :get-list-response "cxengage/entities/get-list-response"
                 :get-list-item-response "cxengage/entities/get-list-item-response"
                 :get-lists-response "cxengage/entities/get-lists-response"
                 :get-list-types-response "cxengage/entities/get-list-types-response"
                 :create-list-response "cxengage/entities/create-list-response"
                 :create-list-item-response "cxengage/entities/create-list-item-response"
                 :update-list-response "cxengage/entities/update-list-response"
                 :update-list-item-response "cxengage/entities/update-list-item-response"
                 :delete-list-item-response "cxengage/entities/delete-list-item-response"
                 :update-users-capacity-rule-response "cxengage/entities/update-users-capacity-rule-response"
                 :download-list-response "cxengage/entities/download-list-response"
                 :upload-list-response "cxengage/entities/upload-list-response"
                 :get-groups-response "cxengage/entities/get-groups-response"
                 :get-group-response "cxengage/entities/get-group-response"
                 :create-group-response "cxengage/entities/create-group-response"
                 :update-group-response "cxengage/entities/update-group-response"
                 :get-skills-response "cxengage/entities/get-skills-response"
                 :get-entity-response "cxengage/entities/get-entity-response"
                 :get-skill-response "cxengage/entities/get-skill-response"
                 :create-skill-response "cxengage/entities/create-skill-response"
                 :update-skill-response "cxengage/entities/update-skill-response"
                 :get-email-types-response "cxengage/entities/get-email-types-response"
                 :get-email-templates-response "cxengage/entities/get-email-templates-response"
                 :get-flows-response "cxengage/entities/get-flows-response"
                 :get-flow-response "cxengage/entities/get-flow-response"
                 :create-flow-response "cxengage/entities/create-flow-response"
                 :update-flow-response "cxengage/entities/update-flow-response"
                 :create-flow-draft-response "cxengage/entities/create-flow-draft-response"
                 :remove-flow-draft-response "cxengage/entities/remove-flow-draft-response"
                 :create-email-template-response "cxengage/entities/create-email-template-response"
                 :update-email-template-response "cxengage/entities/update-email-template-response"
                 :delete-email-template-response "cxengage/entities/delete-email-template-response"
                 :get-artifacts-response "cxengage/entities/get-artifacts-response"
                 :get-artifact-response "cxengage/entities/get-artifact-response"
                 :get-outbound-identifiers-response "cxengage/entities/get-outbound-identifiers-response"
                 :get-user-outbound-identifier-lists-response "cxengage/entities/get-user-outbound-identifier-lists-response"
                 :update-outbound-identifier-response "cxengage/entities/update-outbound-identifier-response"
                 :create-outbound-identifier-response "cxengage/entities/create-outbound-identifier-response"
                 :delete-outbound-identifier-response "cxengage/entities/delete-outbound-identifier-response"
                 :dissociate-response "cxengage/entities/dissociate-response"
                 :associate-response "cxengage/entities/associate-response"
                 :get-outbound-identifier-list-response "cxengage/entities/get-outbound-identifier-list-response"
                 :update-outbound-identifier-list-response "cxengage/entities/update-outbound-identifier-list-response"
                 :create-outbound-identifier-list-response "cxengage/entities/create-outbound-identifier-list-response"
                 :get-outbound-identifier-lists-response "cxengage/entities/get-outbound-identifier-lists-response"
                 :add-outbound-identifier-list-member-response "cxengage/entities/add-outbound-identifier-list-member-response"
                 :remove-outbound-identifier-list-member-response "cxengage/entities/remove-outbound-identifier-list-member-response"
                 :get-custom-metrics-response "cxengage/entities/get-custom-metrics-response"
                 :get-custom-metric-response "cxengage/entities/get-custom-metric-response"
                 :update-custom-metric-response "cxengage/entities/update-custom-metric-response"
                 :create-custom-metric-response "cxengage/entities/create-custom-metric-response"
                 :get-slas-response "cxengage/entities/get-slas-response"
                 :get-sla-response "cxengage/entities/get-sla-response"
                 :update-sla-response "cxengage/entities/update-sla-response"
                 :create-sla-response "cxengage/entities/create-sla-response"
                 :create-sla-version-response "cxengage/entities/create-sla-version-response"
                 :get-roles-response "cxengage/entities/get-roles-response"
                 :get-role-response "cxengage/entities/get-role-response"
                 :get-reason-response "cxengage/entities/get-reason-response"
                 :get-reason-list-response "cxengage/entities/get-reason-list-response"
                 :get-platform-roles-response "cxengage/entities/get-platform-roles-response"
                 :get-integrations-response "cxengage/entities/get-integrations-response"
                 :get-integration-response "cxengage/entities/get-integration-response"
                 :create-integration-response "cxengage/entities/create-integration-response"
                 :update-integration-response "cxengage/entities/update-integration-response"
                 :get-integration-listeners-response "cxengage/entities/get-integration-listeners-response"
                 :get-integration-listener-response "cxengage/entities/get-integration-listener-response"
                 :create-integration-listener-response "cxengage/entities/create-integration-listener-response"
                 :update-integration-listener-response "cxengage/entities/update-integration-listener-response"
                 :get-capacity-rules-response "cxengage/entities/get-capacity-rules-response"
                 :get-reasons-response "cxengage/entities/get-reasons-response"
                 :get-reason-lists-response "cxengage/entities/get-reason-lists-response"
                 :get-permissions-response "cxengage/entities/get-permissions-response"
                 :create-role-response "cxengage/entities/create-role-response"
                 :update-role-response "cxengage/entities/update-role-response"
                 :get-historical-report-folders-response "cxengage/entities/get-historical-report-folders-response"
                 :get-data-access-reports-response "cxengage/entities/get-data-access-reports-response"
                 :get-data-access-report-response "cxengage/entities/get-data-access-report-response"
                 :create-data-access-report-response "cxengage/entities/create-data-access-report-response"
                 :update-data-access-report-response "cxengage/entities/update-data-access-report-response"
                 :get-data-access-member-response "cxengage/entities/get-data-access-member-response"
                 :create-user-response "cxengage/entities/create-user-response"
                 :update-user-response "cxengage/entities/update-user-response"
                 :update-platform-user-response "cxengage/entities/update-platform-user-response"
                 :get-platform-user-response "cxengage/entities/get-platform-user-response"
                 :get-platform-user-email-response "cxengage/entities/get-platform-user-email-response"
                 :get-identity-providers-response "cxengage/entities/get-identity-providers-response"
                 :update-user-skill-member-response "cxengage/entities/update-user-skill-member-response"
                 :update-reason-response "cxengage/entities/update-reason-response"
                 :update-reason-list-response "cxengage/entities/update-reason-list-response"
                 :update-disposition-list-response "cxengage/entities/update-disposition-list-response"
                 :create-reason-response "cxengage/entities/create-reason-response"
                 :create-reason-list-response "cxengage/entities/create-reason-list-response"
                 :create-disposition-list-response "cxengage/entities/create-disposition-list-response"
                 :get-dispositions-response "cxengage/entities/get-dispositions-response"
                 :get-disposition-response "cxengage/entities/get-disposition-response"
                 :create-disposition-response "cxengage/entities/create-disposition-response"
                 :update-disposition-response "cxengage/entities/update-disposition-response"
                 :get-dispatch-mappings-response "cxengage/entities/get-dispatch-mappings-response"
                 :get-dispatch-mapping-response "cxengage/entities/get-dispatch-mapping-response"
                 :create-dispatch-mapping-response "cxengage/entities/create-dispatch-mapping-response"
                 :update-dispatch-mapping-response "cxengage/entities/update-dispatch-mapping-response"
                 :get-transfer-lists-response "cxengage/entities/get-transfer-lists-response"
                 :get-transfer-list-response "cxengage/entities/get-transfer-list-response"
                 :create-transfer-list-response "cxengage/entities/create-transfer-list-response"
                 :update-transfer-list-response "cxengage/entities/update-transfer-list-response"
                 :get-tenant-response "cxengage/entities/get-tenant-response"
                 :get-tenants-response "cxengage/entities/get-tenants-response"
                 :create-tenant-response "cxengage/entities/create-tenant-response"
                 :update-tenant-response "cxengage/entities/update-tenant-response"
                 :get-api-keys-response "cxengage/entities/get-api-keys-response"
                 :create-api-key-response "cxengage/entities/create-api-key-response"
                 :update-api-key-response "cxengage/entities/update-api-key-response"
                 :delete-api-key-response "cxengage/entities/delete-api-key-response"
                 :get-business-hours-response "cxengage/entities/get-business-hours-response"
                 :get-business-hour-response "cxengage/entities/get-business-hour-response"
                 :create-business-hour-response "cxengage/entities/create-business-hour-response"
                 :update-business-hour-response "cxengage/entities/update-business-hour-response"
                 :create-exception-response "cxengage/entities/create-exception-response"
                 :delete-exception-response "cxengage/entities/delete-execption-response"
                 :get-timezones-response "cxengage/entities/get-timezones-response"
                 :get-message-templates-response "cxengage/entities/get-message-templates-response"
                 :get-message-template-response "cxengage/entities/get-message-template-response"
                 :create-message-template-response "cxengage/entities/create-message-template-response"
                 :update-message-template-response "cxengage/entities/update-message-template-response"
                 :get-recordings-response "cxengage/entities/get-recordings-response"
                ;;hygen-insert-above-CRUD-topics
                 ;; Reporting
                 :get-capacity-response "cxengage/reporting/get-capacity-response"
                 :get-stat-query-response "cxengage/reporting/get-stat-query-response"
                 :get-available-stats-response "cxengage/reporting/get-available-stats-response"
                 :get-contact-interaction-history-response "cxengage/reporting/get-contact-interaction-history-response"
                 :get-interaction-response "cxengage/reporting/get-interaction-response"
                 :batch-response "cxengage/reporting/batch-response"
                 :add-stat "cxengage/reporting/stat-subscription-added"
                 :bulk-add-stat "cxengage/reporting/bulk-stat-subscription-added"
                 :remove-stat "cxengage/reporting/stat-subscription-removed"
                 :bulk-remove-stat "cxengage/reporting/bulk-stat-subscription-removed"
                 :polling-started "cxengage/reporting/polling-started"
                 :polling-stopped "cxengage/reporting/polling-stopped"
                 :get-crm-interactions-response "cxengage/reporting/get-crm-interactions-response"
                 :get-bulk-stat-query-response "cxengage/reporting/get-bulk-stat-query-response"

                 ;; Logging
                 :logs-dumped "cxengage/logging/logs-dumped"
                 :log-level-set "cxengage/logging/log-level-set"
                 :logs-saved "cxengage/logging/logs-saved"

                 ;; Notifications
                 :show-banner "cxengage/notifications/show-banner"

                 ;; Generic Interaction Topics
                 :work-offer-received "cxengage/interactions/work-offer-received"
                 :generic-screen-pop-received "cxengage/interactions/screen-pop-received"
                 :work-initiated-received "cxengage/interactions/work-initiated-received"
                 :disposition-codes-received "cxengage/interactions/disposition-codes-received"
                 :disposition-code-changed "cxengage/interactions/disposition-code-changed"
                 :custom-fields-received "cxengage/interactions/custom-fields-received"
                 :interaction-update-transfer-menu "cxengage/interactions/interaction-update-transfer-menu"
                 :work-accepted-received "cxengage/interactions/work-accepted-received"
                 :work-rejected-received "cxengage/interactions/work-rejected-received"
                 :work-ended-received "cxengage/interactions/work-ended-received"
                 :interaction-end-acknowledged "cxengage/interactions/end-acknowledged"
                 :interaction-accept-acknowledged "cxengage/interactions/accept-acknowledged"
                 :interaction-focus-acknowledged "cxengage/interactions/focus-acknowledged"
                 :interaction-unfocus-acknowledged "cxengage/interactions/unfocus-acknowledged"
                 :contact-assignment-acknowledged "cxengage/interactions/contact-assign-acknowledged"
                 :contact-unassignment-acknowledged "cxengage/interactions/contact-unassign-acknowledged"
                 :script-received "cxengage/interactions/script-received"
                 :wrapup-details-received "cxengage/interactions/wrapup-details-received"
                 :enable-wrapup-acknowledged "cxengage/interactions/enable-wrapup-acknowledged"
                 :disable-wrapup-acknowledged "cxengage/interactions/disable-wrapup-acknowledged"
                 :end-wrapup-acknowledged "cxengage/interactions/end-wrapup-acknowledged"
                 :wrapup-started "cxengage/interactions/wrapup-started"
                 :wrapup-ended "cxengage/interactions/wrapup-ended"
                 :send-script "cxengage/interactions/send-script"
                 :resource-added-received "cxengage/interactions/resource-added-received"
                 :resource-removed-received "cxengage/interactions/resource-removed-received"
                 :resource-hold-received "cxengage/interactions/resource-hold-received"
                 :resource-resume-received "cxengage/interactions/resource-resume-received"
                 :send-custom-interrupt-acknowledged "cxengage/interactions/send-custom-interrupt-acknowledged"
                 :get-note-response "cxengage/interactions/get-note-response"
                 :get-notes-response "cxengage/interactions/get-notes-response"
                 :create-note-response "cxengage/interactions/create-note-response"
                 :update-note-response "cxengage/interactions/update-note-response"
                 :flow-action-acknowledged "cxengage/interactions/flow-action-acknowledged"

                 ;; Email Interaction Topics
                 :attachment-received "cxengage/interactions/email/attachment-received"
                 :attachment-list "cxengage/interactions/email/attachment-list"
                 :email-artifact-received "cxengage/interactions/email/artifact-received"
                 :plain-body-received "cxengage/interactions/email/plain-body-received"
                 :html-body-received "cxengage/interactions/email/html-body-received"
                 :details-received "cxengage/interactions/email/details-received"
                 :email-reply-artifact-created "cxengage/interactions/email/email-reply-artifact-created"
                 :add-attachment "cxengage/interactions/email/attachment-added"
                 :remove-attachment "cxengage/interactions/email/attachment-removed"
                 :send-reply "cxengage/interactions/email/send-reply"
                 :start-outbound-email "cxengage/interactions/email/start-outbound-email"
                 :agent-reply-started-acknowledged "cxengage/interactions/email/agent-reply-started-acknowledged"
                 :agent-no-reply-acknowledged "cxengage/interactions/email/agent-no-reply-acknowledged"
                 :agent-cancel-reply-acknowledged "cxengage/interactions/email/agent-cancel-reply-acknowledged"

                 ;; Voice Interaction Topics
                 :hold-acknowledged "cxengage/interactions/voice/hold-acknowledged"
                 :resume-acknowledged "cxengage/interactions/voice/resume-acknowledged"
                 :mute-acknowledged "cxengage/interactions/voice/mute-acknowledged"
                 :unmute-acknowledged "cxengage/interactions/voice/unmute-acknowledged"
                 :recording-start-acknowledged "cxengage/interactions/voice/start-recording-acknowledged"
                 :recording-stop-acknowledged "cxengage/interactions/voice/stop-recording-acknowledged"
                 :customer-hold-received "cxengage/interactions/voice/customer-hold-received"
                 :customer-resume-received "cxengage/interactions/voice/customer-resume-received"
                 :customer-transfer-acknowledged "cxengage/interactions/voice/customer-transfer-acknowledged"
                 :cancel-transfer-acknowledged "cxengage/interactions/voice/cancel-transfer-acknowledged"
                 :cancel-dial-acknowledged "cxengage/interactions/voice/cancel-dial-acknowledged"
                 :customer-hold "cxengage/interactions/voice/customer-hold-received"
                 :customer-resume "cxengage/interactions/voice/customer-resume-received"
                 :resource-muted "cxengage/interactions/voice/resource-mute-received"
                 :resource-unmuted "cxengage/interactions/voice/resource-unmute-received"
                 :recording-started "cxengage/interactions/voice/recording-start-received"
                 :recording-ended "cxengage/interactions/voice/recording-end-received"
                 :dial-send-acknowledged "cxengage/interactions/voice/dial-send-acknowledged"
                 :send-digits-acknowledged "cxengage/interactions/voice/send-digits-acknowledged"
                 :transfer-connected "cxengage/interactions/voice/transfer-connected"
                 :transfer-started "cxengage/interactions/voice/transfer-started"
                 :transfer-cancelled "cxengage/interactions/voice/transfer-cancelled"
                 :resource-hold-acknowledged "cxengage/interactions/voice/resource-hold-acknowledged"
                 :resource-resume-acknowledged "cxengage/interactions/voice/resource-resume-acknowledged"
                 :resume-all-acknowledged "cxengage/interactions/voice/resume-all-acknowledged"
                 :resource-removed-acknowledged "cxengage/interactions/voice/resource-removed-acknowledged"
                 :silent-monitoring-start-acknowledged "cxengage/interactions/voice/silent-monitoring-start-acknowledged"
                 :force-killed-twilio-connection "cxengage/interactions/voice/force-killed-twilio-connection"
                 :update-call-controls "cxengage/interactions/voice/update-call-controls"
                 :silent-monitor-start "cxengage/interactions/voice/silent-monitor-start"
                 :silent-monitor-end "cxengage/interactions/voice/silent-monitor-end"
                 :get-monitored-interaction-response "cxengage/interactions/voice/get-monitored-interaction-response"
                 :set-monitored-interaction-response "cxengage/interactions/voice/set-monitored-interaction-response"
                 :customer-connected "cxengage/interactions/voice/customer-connected"
                 :customer-hold-error "cxengage/interactions/voice/customer-hold-error"
                 :customer-resume-error "cxengage/interactions/voice/customer-resume-error"

                 ;; Twilio Topics
                 :twilio-device-ready "cxengage/twilio/device-ready"

                 ;; Messaging Interaction Topics
                 :transcript-response "cxengage/interactions/messaging/transcript-received"
                 :smooch-history-received "cxengage/interactions/messaging/smooch-history-received"
                 :smooch-message-received "cxengage/interactions/messaging/smooch-message-received"
                 :smooch-conversation-read-received "cxengage/interactions/messaging/smooch-conversation-read-received"
                 :smooch-conversation-read-agent-received "cxengage/interactions/messaging/smooch-conversation-read-agent-received"
                 :smooch-typing-received "cxengage/interactions/messaging/smooch-typing-received"
                 :smooch-typing-agent-received "cxengage/interactions/messaging/smooch-typing-agent-received"
                 :messaging-history-received "cxengage/interactions/messaging/history-received"
                 :send-message-acknowledged "cxengage/interactions/messaging/send-message-acknowledged"
                 :new-message-received "cxengage/interactions/messaging/new-message-received"
                 :initialize-outbound-sms-response "cxengage/interactions/messaging/initialize-outbound-sms-response"
                 :send-outbound-sms-response "cxengage/interactions/messaging/send-outbound-sms-response"
                 :set-typing-indicator "cxengage/interactions/messaging/set-typing-indicator"
                 :mark-as-seen "cxengage/interactions/messaging/mark-as-seen"

                 ;; Errors
                 :failed-to-refresh-sqs-integration "cxengage/errors/fatal/failed-to-refresh-sqs-integration"
                 :failed-to-delete-sqs-message "cxengage/errors/warn/failed-to-delete-sqs-message"
                 :failed-to-receive-sqs-message "cxengage/errors/warn/failed-to-receive-sqs-message"
                 :sqs-uncaught-exception "cxengage/errors/error/sqs-uncaught-exception"
                 :sqs-loop-ended "cxengage/errors/warn/sqs-loop-ended"
                 :mqtt-failed-to-connect "cxengage/errors/fatal/mqtt-failed-to-connect"
                 :mqtt-lost-connection "cxengage/errors/error/mqtt-lost-connection"
                 :failed-to-retrieve-messaging-history "cxengage/errors/error/failed-to-retrieve-messaging-history"
                 :failed-to-retrieve-messaging-metadata "cxengage/errors/error/failed-to-retrieve-messaging-metadata"
                 :failed-to-create-email-reply-artifact "cxengage/errors/error/failed-to-create-email-reply-artifact"
                 :failed-to-create-outbound-email-interaction "cxengage/errors/error/failed-to-create-outbound-email-interaction"
                 :unknown-agent-notification-type-received "cxengage/errors/error/unknown-agent-notification-type"
                 :api-rejected-bad-client-request "cxengage/errors/error/api-rejected-bad-client-request"
                 :api-encountered-internal-error "cxengage/errors/error/api-encountered-internal-server-error"
                 :failed-to-send-digits-invalid-interaction "cxengage/errors/error/failed-to-send-digits-invalid-interaction"
                 :api-returned-404-not-found "cxengage/errors/error/api-returned-404-not-found"
                 :no-twilio-integration "cxengage/errors/error/no-twilio-integration"})

(defn get-topic
  "Gets the SDK consumer topic string for a specific internal topic key."
  [k]
  (if-let [topic (get sdk-topics k)]
    topic
    (log :error "Topic not found in topic list" (clj->js k))))
