# Change Log

## [Unreleased]

## [6.12.2]
* CXV1-12707 - MQTT Banner on connection loss

## [6.12.1]
* CXV1-12778 - Use salesforce-classic user id for contact assignment's hook-by id

## [6.12.0]
* Added support functions for CXV1-12639

## [6.11.4]
* CXV1-12750 - Work items screen pop and assign

## [6.11.3]
* CXV1-12715 - MQTT Reconnect logic
* Updated Paho to 1.0.3

## [6.11.2]
* CXV1-10834 - Allowed for optional "ttl" property on CxEngage.authentication.login() method to allow for specifying token expiration via the front end

## [6.11.1]
* CXV1-11510 - Undo duplicate retry 5xx logic

## [6.11.0]
* CXV1-12186 - New pub subs for Eckoh (update-call-controls and show-banner)

## [6.10.6]
* CXV1-10482 - Prevent emails from being sent until the artifact for them has been created. Publish topic so client knows when they can send.

## [6.10.5]
* CXV1-11510 - Added retry logic for 5xx errors on interaction heartbeats

## [6.10.4]
* CXV1-12010 - Fixed calls being accepted before they were "incoming" on the Twilio device

## [6.10.3]
* CXV1-11841 - Send blank active tab set signals in zendesk when user and tickets are unfocused

## [6.10.2]
* CXV1-12330 - Give SSO popup window more unique, descriptive name

## [6.10.1]
* CXV1-9857 - Updated salesforce-lightning screen pops

## [6.10.0]
* CXV1-12292 - Add/update salesforce-lightning active tab, assign, unassign, and focus functions

## [6.9.4]
* CXV1-12427 - Fix hook for transferred interactions so focusInteraction will work

## [6.9.3]
* CXV1-12438 - Improved validation to salesforce assign and unassign function calls (include checks for interaction existing and blank active tab)
* CXV1-12292 - Fix salesforce lightning initialization and click to dial

## [6.9.2]
* CXV1-12438 - Added validation to salesforce assign and unassign function calls

## [6.9.1]
* CXV1-12427 - Fixed screen pop/auto-assign for transferred interactions
* CXV1-12327 - Fixed formatting of auto-assigned object id

## [6.9.0]
* CXV1-12379 - Added Get Tenant Details to Session module (me route)

## [6.8.13]
* CXV1-12238 - Default click to dial as disabled until logged in
* CXV1-12422 - Set active tab on initialisation
* CXV1-12327 - Fix auto-assigned contact name

## [6.8.12]
* CXV1-12327 - Create active tab changed pub sub for salesforce classic. Fix auto-assign when there is only one search result.

## [6.8.11]
* CXV1-11875 - Automatically pop multiple results modal for outbound email and sms interactions

## [6.8.10]
* CXV1-12328 - Fix id spec for assigning in salesforce

## [6.8.9]
* CXV1-12250 - Fix receiving scripts before work offers and getting interactions with unsubmitted scripts back (transferred)

## [6.8.8]
* CXV1-12247 - Fix "from" for Facebook messaging messages

## [6.8.7]
* CXV1-12283 - Unsubscribe from MQTT on wrapup-started

## [6.8.6]
* CXV1-12283 - Unsubscribe from MQTT on work-ended

## [6.8.5]
* CXV1-12004 - Update Salesforce Classic module

## [6.8.4]
* CXV1-11791 - Fixed reappearance of modal in zendesk when interaction is transferred back to the initial agent that answered the call.

## [6.8.3]
* CXV1-12247 - Fix "from" for non-customer messaging messages (keep agent's from id the same)

## [6.8.2]
* CXV1-12166 - Add initialised event for user/ticket to set active tab on

## [6.8.1]
* CXV1-11949 - Added dynamic detection of outbound integration type

## [6.8.0]
* CXV1-11731 - Added SDK support for non-voice transfers  

## [6.7.0]
* Added `updateUser` and `updateTicket` functions and pubsubs for zendesk to communicate with toolbar on user/ticket changes

## [6.6.4]
* minor SF classic fixes

## [6.6.3]
* Updated SSO identity page information to pass full api url

## [6.6.2]
* Give zendesk unassign unique topics

## [6.6.1]
* Fixed MQTT error logging
* Added missing topic for get-crm-interactions

## [6.6.0]
* Added `CxEngage.reporting.getCrmInteractions()` function to the Reporting module
* Updated Zendesk assign functions to the `interaction-hook-add` flow signal

## [6.5.1]
* Final fixes for SSO Identity Page

## [6.5.0]
* SSO Identity window changes

## [6.4.3]
* CXV1-11804 - Fixed auto-assign race condition

## [6.4.2]
* Update SSO token passthrough

## [6.4.1]
* SSO region missing fix

## [6.4.0]
* CXV1-10798 - Implement Single Sign On via AWS Cognito

## [6.3.2]
* [Zendesk] Support external modal url

## [6.3.1]
* CXV1-10523 - Focus Interaction bug fixes

## [6.3.0]
* [Zendesk] Fixed Auto Assign
* [Zendesk] Added Fuzzy Search
* [Zendesk] Fixed multiple result contact modal
* [Zendesk] Fixed Focus interaction
* [Zendesk] Added contextual information to assignment callbacks
* [Zendesk] Added internal-pop pubsub with contextual information

## [6.2.0]
* Added CxEngage.session.setLocale() fn for consumers to change their locale after initialization

## [6.1.1]
* Various Zendesk Module bug fixes
* CXV1-11406 - Add locale to /available request, provide consumers ability to specify locale on init

## [6.1.0]
* CXV1-11103 - Added CRM modules to core

## [6.0.2]
* Change error level of script error 4021 to error (from interaction-fatal)

## [6.0.1]
* Include session id in outbound interaction creation calls (no jira)

## [6.0.0]
* Removed some needless logging
* Added the `excludeOffline` flag to the getUsers() fn
* CXV1-11254 - Change error level of failed script updates from `interaction-fatal` to `error`, changed script implementation to require explicit passing of script id

## [5.4.1]
* CXV1-8541 - Decrease presence-recommended heartbeat delay by 25% and send heartbeats at that pace instead

## [5.4.0]
* CXV1-11103 - Merge SDK utils with Core
* CXV1-9840 - Store region in state to use when initializing SQS

## [5.3.28]
* CXV1-11151 - Added a twilio extern, added additional steps to the retry logic around accepting twilio connections
* Store created outbound interactions even before we get the work offer, so we can fetch things like the channel type of the interaction without necessarily having gotten the work offer yet

## [5.3.27]
* Update cljs-sdk-utils to 0.0.15
* Upgrade clojure dep
* Upgrade lumbajack + utils
* Upgrade clojurescript dep & fix cljs-spec breaking change
* Added the library 'expound' for better spec output
* Remove last remnant of ignore-work-items logic
* Change default log level to debug
* Throw error on failing to fetch the artifact id of the reply artifact created
* Add additional logging around failing to build API urls; which url we tried to build, what ID was nil, etc

## [5.3.26]
* Added error for force killing a twilio interaction

## [5.3.25]
* CXV1-10395, CXV1-10396, CXV1-10461
  * Removed extensions checking on login
  * Removed permissions checking on login
  * Removed auto-rejection of interactions where channel-type is work-item

## [5.3.24]
* Perform retries on finding the connection prior to assuming fatal error

## [5.3.23]
* Added error for not being able to find twilio connection object in state (no jira)

## [5.3.22]
* CXV1-9785 - Call disconnect on the twilio device when interaction end returns 404
* CXV1-9785 - Added voice interaction heartbeats

## [5.3.21]
* Start fetching email bodies prior to work accepted being received from flow

## [5.3.20]
* CXV1-10348 - Fixed cannot send script while being offered a parallel work offer

## [5.3.19]
* Broadcast artifact receipt pub/sub in the success case, not only the failure case

## [5.3.18]
* Remove delay on work offers for email until after manifest is downloaded

## [5.3.17]
* Remove no-extensions-found error

## [5.3.16]
* Change log statement for when logs fail to format to not be of type error

## [5.3.15]
* Fix typo in previous logging fix

## [5.3.14]
* CXV1-10349 - Catch JSON stringification errors in the event that attempting to stringify the value the client provides us fails

## [5.3.13]
* CXV1-9846 - Fixed twilio error catching
* CXV1-10345 - Temporarily only broadcast the work offer for emails after the manifest is already pub/sub'd (hacky interim fix)
  * The proper fix is captured in CXV1-10397

## [5.3.12]
* CXV1-10347 - Add error for if an agent attempts to go online while having no extensions configured

## [5.3.11]
* Bump cljs-sdk-utils to 0.0.7, fixing wrong `level` on twilio init error code (updated to session-fatal)

## [5.3.10]
* CXV1-9846 Update cljs-sdk-utils and throw error when Twilio device encounters an error
* CXV1-10121 Fixed regression surrounding messaging interactions when an agent receives a script before the work offer.
* CXV1-9746 Added error response assertions for reporting module unit tests
* CXV1-10308 Stop sending logs every thirty seconds after first failure

## [5.3.9]
* CXV1-10121 Fixed a regression introduced in 5.3.7 that causes agent messages to not be received
* CXV1-9744 Logging module error response unit test
* CXV1-9841 Every 30 seconds, push logs to logging pipeline
* CXV1-10071 Silent monitoring signal support w/ unit tests
* CXV1-9972 Add error context for codes 1xxx-5xxx, improve logging around failed specs
* Added additional logging around incoming MQTT messaging messages

## [5.3.8]
* CXV1-9972 - Improved contexualization and details of error functions

## [5.3.7]
* CXV1-10028 agent cancelled reply signal for email reporting
* CXV1-8994 - Expose api-url function for external modules
* Fixed a bug where scripts received before a work offer were unable to be replied to

## [5.3.6]
* Fixed small regression in batch request functionality.

## [5.3.5]
* CXV1-9750 - Voice module unit test coverage expanded significantly.

## [5.3.4]
* Fixed small regression in the kebabification of API responses (no JIRA)

## [5.3.3]
* CXV1-9580 Split out voice interrupts using def-sdk-fn macro
* CXV1-9907 - Create new email reporting event signals & appropriate SDK fns
* CXV1-9564 Added new error codes & new explicit error paths (rather than the previous generic error handling) to help with front-end error handling
* CXV1-9910 Added warnings on missing state properties to ease debugging

## [5.3.2]
* CXV1-9836 - Use region specified in twilio integration

## [5.3.1]
* CXV1-8933, CXV1-9389 - Fix callback firing for contacts fns, add a bunch of tests

## [5.3.0]
* CLJS SDK Utils split out
* Publish the latest extension list in a couple more places
* CXV1-8189 - Auto-reject work offers of channel-type "work-item"
* Added 3 new API fn's and relevant pub/subs:
  * `CxEngage.session.getToken();`
  * `CxEngage.session.getActiveUserId();`
  * `CxEngage.session.getActiveTenantId();`

## [5.2.1]
* Fixed MQTT startup error
* Don't associate interaction data for interactions that don't exist
* Don't try and parse agent notifications that don't have a session ID present
* Add additional debug logging around parsing session ID messages

## [5.2.0]
* CXV1-9225 Intelligently ack / delete messages from SQS based on timestamp in session-id
* CXV1-8548 Expose branding API via SDK
* CXV1-9500 Centralize all API requests under domain
* CXV1-9595 Filter out disposition code messages that are using the platform default disposition lists

## [5.1.1]
* CXV1-9536 Fixed work offers not accepting the call, and sending expired error

## [5.1.0]
* CXV1-9468 Added work-cancel interrupt for Click-to-dial interactions
* CXV1-9325 Added new public-facing logging functions CxEngage.logging.[debug/info/warn/error/fatal], to allow agent desktop logs to be sent to kibana alongside SDK logs
* CXV1-9495 Fixed an issue where passing a non-map to an SDK fn from the consumers side would throw an error
* CXV1-9496 Added additional error messaging around attempting to accept an expired work offer
* CXV1-9361 Dynamically choose which interrupt type + body to send when calling interaction end, to account for voice interactions needing to be ended differently

## [5.0.2]
* CXV1-9374 Decoupled voice module from twilio, fixed PSTN functionality in the absence of twilio integration.
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
