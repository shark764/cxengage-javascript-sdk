(ns cxengage-javascript-sdk.domain.topics
  (:require-macros [lumbajack.macros :refer [log]])
  (:require [lumbajack.core :as jack]))

(def sdk-topics {;; Misc Topics
                 :voice-enabled "cxengage/capabilities/voice-available"
                 :messaging-enabled "cxengage/capabilities/messaging-available"

                 ;; Authentication Topics
                 :login-response "cxengage/authentication/login-response"

                 ;; Session Topics
                 :active-tenant-set "cxengage/session/set-active-tenant-response"
                 :config-response "cxengage/session/config-details"
                 :presence-state-changed "cxengage/session/state-change-response"
                 :presence-state-change-request-acknowledged "cxengage/session/state-change-request-acknowledged"
                 :presence-heartbeats-response "cxengage/session/heartbeat-response"
                 :session-started "cxengage/session/started"
                 :set-direction-response "cxengage/session/set-direction-response"
                 :extension-list "cxengage/session/extension-list"
                 :tenant-list "cxengage/session/tenant-list"
                 :session-ended "cxengage/session/ended"
                 :get-active-user-id-response "cxengage/session/get-active-user-id-response"
                 :get-active-tenant-id-response "cxengage/session/get-active-tenant-id-response"
                 :get-token-response "cxengage/session/get-token-response"

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

                 ;; CRUD topics
                 :get-queue-response "cxengage/entities/get-queue-response"
                 :get-queues-response "cxengage/entities/get-queues-response"
                 :get-transfer-list-response "cxengage/entities/get-transfer-list-response"
                 :get-transfer-lists-response "cxengage/entities/get-transfer-lists-response"
                 :get-user-response "cxengage/entities/get-user-response"
                 :get-users-response "cxengage/entities/get-users-response"
                 :update-user-response "cxengage/entities/update-user-response"
                 :get-branding-response "cxengage/entities/get-branding-response"

                 ;; Reporting
                 :get-capacity-response "cxengage/reporting/get-capacity-response"
                 :get-stat-query-response "cxengage/reporting/get-stat-query-response"
                 :get-available-stats-response "cxengage/reporting/get-available-stats-response"
                 :get-contact-interaction-history-response "cxengage/reporting/get-contact-interaction-history-response"
                 :get-interaction-response "cxengage/reporting/get-interaction-response"
                 :batch-response "cxengage/reporting/batch-response"
                 :add-stat "cxengage/reporting/stat-subscription-added"
                 :remove-stat "cxengage/reporting/stat-subscription-removed"
                 :polling-started "cxengage/reporting/polling-started"
                 :polling-stopped "cxengage/reporting/polling-stopped"

                 ;; Logging
                 :logs-dumped "cxengage/logging/logs-dumped"
                 :log-level-set "cxengage/logging/log-level-set"
                 :logs-saved "cxengage/logging/logs-saved"

                 ;; Generic Interaction Topics
                 :work-offer-received "cxengage/interactions/work-offer-received"
                 :screen-pop-received "cxengage/interactions/url-pop-received"
                 :generic-screen-pop-received "cxengage/interactions/screen-pop-received"
                 :work-initiated-received "cxengage/interactions/work-initiated-received"
                 :disposition-codes-received "cxengage/interactions/disposition-codes-received"
                 :disposition-code-changed "cxengage/interactions/disposition-code-changed"
                 :custom-fields-received "cxengage/interactions/custom-fields-received"
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
                 :add-attachment "cxengage/interactions/email/attachment-added"
                 :remove-attachment "cxengage/interactions/email/attachment-removed"
                 :send-reply "cxengage/interactions/email/send-reply"
                 :start-outbound-email "cxengage/interactions/email/start-outbound-email"
                 :agent-reply-started-acknowledged "cxengage/interactions/email/agent-reply-started-acknowledged"
                 :agent-no-reply-acknowledged "cxengage/interactions/email/agent-no-reply-acknowledged"
                 :agent-cancel-reply-acknowledged "cxengage/interactions/email/agent-cancel-reply-acknowledged"

                 ;; Voice Interaction Topics
                 :recording-response "cxengage/interactions/voice/recording-received"
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
                 :resource-hold-acknowledged "cxengage/interactions/voice/resource-hold-acknowledged"
                 :resource-resume-acknowledged "cxengage/interactions/voice/resource-resume-acknowledged"
                 :resume-all-acknowledged "cxengage/interactions/voice/resume-all-acknowledged"
                 :resource-removed-acknowledged "cxengage/interactions/voice/resource-removed-acknowledged"
                 :silent-monitoring-start-acknowledged "cxengage/interactions/voice/silent-monitoring-start-acknowledged"
                 :voice-interaction-heartbeat "cxengage/interactions/voice/heartbeat"
                 :force-killed-twilio-connection "cxengage/interactions/voice/force-killed-twilio-connection"

                 ;; Messaging Interaction Topics
                 :transcript-response "cxengage/interactions/messaging/transcript-received"
                 :messaging-history-received "cxengage/interactions/messaging/history-received"
                 :send-message-acknowledged "cxengage/interactions/messaging/send-message-acknowledged"
                 :new-message-received "cxengage/interactions/messaging/new-message-received"
                 :initialize-outbound-sms-response "cxengage/interactions/messaging/initialize-outbound-sms-response"
                 :send-outbound-sms-response "cxengage/interactions/messaging/send-outbound-sms-response"

                 ;; Errors
                 :failed-to-refresh-sqs-integration "cxengage/errors/fatal/failed-to-refresh-sqs-integration"
                 :mqtt-failed-to-connect "cxengage/errors/fatal/mqtt-failed-to-connect"
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
    (log :error "Topic not found in topic list" k)))
