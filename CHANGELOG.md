# Change Log

## [Unreleased]
* CXV1-9332 Seperated interrupts into their own front facing functions. No changes to public-facing API, just internal cleanup.

## [5.0.1]
* CXV1-9346 - Split note actions out into their own independent public-facing functions. Does not impact the front-end.
* CXV1-9428 - Fixed failing to retrieve email bodies on incoming emails

## [5.0.0]
* ** BREAKING ** refactored entities module - each entity now has it's own function and will require passing the proper type-id ie; "resource-id" rather than the generic "entity-id".
* ** BREAKING ** changed global window exposure from "serenova.cxengage.api...." to just "CxEngage"
* ** BREAKING ** removed (due to them now being necessary) the "capabilities" pub/sub messages
* added graceful logout functionality
* removed build-api-url-with-params, replace with api-url
* Because of the above two changes, the usage for initializating the SDK is now: CxEngage.initialize(options). Beyond that you will need to just reference "CxEngage" on the window, and *not create an alias to it like you used to* (via var SDK = serenova.cxengage.initialize(options);)
* reporting functions moved to Reporting Module.
* add paging functionality to get contact interaction history
* added stat-query function to reporting module - allows for one-off batch queries
* fixed pub/sub system to only call callbacks for the subscribers whose topics match
* renamed "goOffline" (in session) - changed to "logout" in authentication
* fixed pub/sub system to only call callbacks for the subscribers whose topics match
* removed the ability to pass callbacks as a part of the params object (must be passed as a 2nd parameter)
* migrated session & authentication modules to use the sdk macro
* added click to email functionality
* added lots of code comments, tided up namespace deps, removed lots of dead code
* fixed capacity callbacks
* fixed reason id/reason/reason list id on go-not-ready not being passed correctly

## [4.1.0]
* added support for click-to-sms and send-sms-by-interrupt
* fixed missing intermediary {internal} object on exposed global
* verify if callbacks are fn's before attempting to call them

## [4.0.1]
* fixed token refresh for SQS

## [4.0.0]
* fixed tenant/resource capacity
* added merging and deleting of contacts
* fixed refreshing of twilio and sqs tokens
* added resource/tenant capacity function
* added retry logic to api-request fn for http 5xx response codes
* added support for reason codes when going not ready
* changed build-api-url-with-params fn to use any kv pair to replace in the url
* renamed SDK.voice.hold & SDK.voice.resume to SDK.voice.customerHold & SDK.voice.customerResume in accordance with new resource-specific controls
* remove old reporting stuff
* make reporting module use user-passed refresh rate
* added resource hold/resume and remove resource
* broke change state out into 3 separate functions internally
* added resource-removed handler
* perform a one-off batch request any time a stat is added to the sub list
* added resource-hold/resume handlers
* removed current users id from muted resources by default (no longer need stop-gap)
* added resume-all topics and API fn
* added active-resources, customer-on-hold, and recording to work-accepted pubsub
* fixed a critical bug where the email module startup log wasn't formatted correctly
* fixed SQS stealing messages from other sessions
* fixed getTranscripts interaction & tenant id parameters being swapped

## [3.0.0]
* added resource-added signal
* broadcast resource id on session start
* altered parameters to mute & unmute to require targetResourceId
* change blast SQS output to debug level logging

## [2.3.2]
* don't send disposition signal if none are present

## [2.3.1]
* temporarily re-add the old reporting pollers

## [2.3.0]
* fixed messaging interaction customer name metadata
* re-worked reporting api batch fns

## [2.2.1]
* fixed send script (filtering them in the catch-all flow action acknowledgement)
* account for time offset on work offers (like we do elsewhere)
* return interaction-id as result for send script
* added twilio debugging when log level is debug
* fixed jenkins build job to fail when tests fail
* changed resource mute broadcast to account for inconsistencies in what flow returns
* added ARTIFACTS_CREATE_ALL as a required permission for login to desktop
* added query parameter to get all notes endpoint to retrieve bodies as well
* re-add auto-holding on warm transfer

## [2.2.0]
* broke cancel-transfer into three functions (resource/queue/extension)
* fixed transfer cancel interrupt
* added email reply functionality (& attachment add/remove)
* fixed bugs with interaction notes
* fixed bugs with contact endpoints
* better output on receiving messages from old sessions

## [2.1.0]
* added send script functionality
* added support for deselecting your disposition code
* added support for artifacts
* added support for disposition codes
* fixed updating user's extension
* receiving incoming emails & broadcasting attachments, html body, plain text body
* fixed wrap-up fns
* added stop-polling API fn
* added getRecordings & getTranscripts
* include artifact id on email response so they can get attachments
* added getAttachmentUrl

## [2.0.1]
* fix casing on all contact-related endpoint outputs

## [2.0.0]
* added interaction notes api
* fixed transfer to extension
* added queues/transfer-lists to entities module
* added support of screen pop type url
* started checking for microphone access prior to starting twilio module
* added DTMF signaling capability
* refactored all modules in accordance with new module system
* refactored many public-facing responses & pubsub topics
* made all SDK init parameters optional with defaults
* added send-script sqs listener
* added interaction-focus & interaction-unfocus api fns
* added twilio init missing-required-integration error
* added ability to publish to multiple topics at once
* added get-all contacts fn

## [1.4.2]
* update lumbajack dep to fix compilation bug

## [1.4.1]
* started auto-holding customers on transfers
* fixed env-passing bug w/ mqtt subscriptions preventing messaging from working

## [1.4.0]
* added ability to set direction to either outbound or inbound
* started returning resource id in Mute / hold / transfers publications
* added appending plus sign to sms phone numbers
* replace the fb id with the fb name in the "from" field of facebook messaging interactions
* added resource check capacity functionality
* added realtime stats polling and available stats
* added permissions checking at tenant set
* added wrapup functionality
* added extension setting functionality
* added click-to-dial functionality
* allow end interaction & accept interaction in states other than ready
* remove boot audio playing during build (causing jenkins builds to fail)
* added to ability to retrive a specific queue or user, or all queues and all users
* reworked transfers & added extension transfers
* retrieve messaging history on interaction accept (in addition to on interaction work offer)
* added transfer-lists crud
* alter interrupt function to accommodate complex interrupt types
* modules no longer live in core state
* fix boot source mapping
* add some error handling if certain integrations arent returned via api

## [1.3.0]
* re-did entire production build process (& fixed advanced clojurescript compilation)
* re-did entire unit testing workflow (run once and get automatic live feedback on if you broke tests)
* re-did entire development workflow (now boot reload + boot repl instead of figwheel)
* fixed small bug in our demo where messaging interactions UI boxes wouldn't properly disappear on interaction end
* removed all lein things
* fixed ns deps in test runner causing certain tests to not run
* removed dead testing macro with-reset
* removed devtools from production build

## [1.2.0]
* N/A (testing jenkins release script stuff, no code changes)

## [1.1.0]
* N/A (testing jenkins release script stuff, no code changes)

## [1.0.3]
* Added contacts Module.

## [1.0.0] - 2017-01-18
* Initial release
