# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [8.34.0] - 2018-11-26
### Added
* CXV1-16295 - AD/TB2 - Outbound Identification - Include Outbound ANI list in new interaction panel for SMS

## [8.33.1] - 2018-11-26
### Fixed
- create user function now can take null values

## [8.33.0] - 2018-11-21
### Changed
- CXV1-16134 - Update SDK to send realtime-report-id

## [8.32.1] - 2018-11-14
### Fixed
- CXV1-15957 - Has-Proficiency value was not handled when was true on creating Skills.

## [8.32.0] - 2018-11-13
### Added
- Functions to getPlatformUser and getIdentityProvders

## [8.31.1] - 2018-11-12
### Fixed
- CXV1-16011 - Members array was not handled by create function for Data Access Control.

## [8.31.0] - 2018-11-10
### Added
- Function to update platform user details
### Changed
- boot dev enviroments > save creds in localstorage and option to skip tenant selection

## [8.30.1] - 2018-11-09
### Changed
- CXV1-15438 - "Interaction ending" 4xxx error codes now return "interaction-fatal" error when their requests 404

## [8.30.0] - 2018-11-08
### Added
- Added flowid in the function voice for ANI outbound

## [8.29.0] - 2018-11-06
### Added
- Added function to bulk add stat subscriptions
- Added function to bulk remove stat subscriptions

## [8.28.1] - 2018-11-06
### Changed
- Updating functions for Access Control to match fixes in APIs.
- Members in Access Control are handled as array within parameters instead of functions for adding/removing.

### Fixed
- CXV1-15814 - Setting updated date and updateBy same as created and createdBy if item hasn't been updated.

## [8.28.0] - 2018-11-06
### Added
- Added function to get message templates

### Fixed
- create group function now sends owner id
- create skill function can handle nil or false has-proficiency

## [8.27.0] - 2018-11-2
### Added
- Added function to get sub entities from api easily

## [8.26.0] - 2018-11-2
### Added
- CXV1-12476 - getUserOutboundIdentifierLists function
- CXV1-12476 - outboundAni parameter and functionality to the click to dial function

## [8.25.0] - 2018-11-2
### Added
- Added function to get Data Access User details

## [8.24.0] - 2018-10-31
### Added
- associate and dissociate functions to add or remove list type items between entities in a more generic fashion
### Removed
- add/remove SkillMembers and add/remove GroupMembers functions

## [8.23.4] - 2018-10-30
### Changed
- CXV1-15438 - Applicable 4xxx error codes now return "interaction-fatal" error when their requests 404

## [8.23.3] - 2018-10-29
### Changed
- getSkill and getGroup now return entitys members as an array of uuids

## [8.23.2] - 2018-10-26
### Added
- Function to get any entity based on a vector path you pass in

### Changed
- Updated get-skill and get-skills to use this new function
- Updated get-skill to do 2 api calls to include basic list members

## [8.23.1] - 2018-10-25
### Added
- Adding function to get all platform roles predefined.
- Adding missing functions for Data Access Control.
- Missing functions to add members to Skill and Group lists.

### Changed
- Hygen templates to be consistent with documentation of functions.

## [8.23.0] - 2018-10-23
### Added
* CXV1-15495 - Handling new signal from flow that tell us if a customer has picked up the call on an outbound voice interaction

## [8.22.0] - 2018-10-22
### Added
- CXV1-15516 - Adding new SDK Functions for Skills Page (Create, Update Get All, Get).
- CXV1-15517 - Adding new SDK Functions for Groups Page (Create, Update Get All, Get).
- CXV1-15503 - Adding new SDK Functions for Users Page (Create, Update Get All, Get).

### Changed
- Changed function to get a list of historical report folders.
- Updates at functions for Data Access Control. data-access-group-id parameter added.

### [8.21.2]
### Added
* CXV1-15326 - Adding code and message from errored API requests as new data to be logged to Kibana

## [8.21.1]
### Fixed
* CXV1-15018 - Updated CxEngage.entities.getDataAccessReport() to match current reporting api

## [8.21.0]
### Added
* CXV1-15605 - Allow stat subscriptions to be added without triggering a batch API request

## [8.20.1] - 2018-10-18
### Added
* CXV1-15326 - Added a pubsub error for when the SDK is unable to upload it's logs

## [8.20.0] - 2018-10-15
### Added
- CXV1-15018 - Adding new SDK Functions for Data Access Control (Create, Update Get All, Get)
- Adding function to get a list of historical report folders.

## [8.19.2]
* CXV1-13818 - Documentation for Session Module

## [8.19.1]
* CXV1-15293 - Removing a callback in add-stat-subscription to avoid getting response of the batch-request when adding a new subscription.

## [8.19.0]
* CXV1-14306 - Added SQS on-message error retry logic

## [8.18.3]
* CXV1-15353 - Fixed duplicate batch polling

## [8.18.2]
* CXV1-15422 - Js-utils version bump to 1.3.1. Update references from utils inside each repo to serenova-js-utils.

## [8.18.1]
* <no-ticket> - Adding condition to handle sla-abandon-threshold when type ignored-abandoned-calls is selected.

## [8.18.0]
* <no-ticket> - Removing the source maps generation from the make-prod-release task (they don't work anyways).
* <no-ticket> - Adding a new boot task: make-dev-release, so we can have source maps in that one.

## [8.17.2]
* CXV1-15151 - Rename attributes customMetricsId, customMetricsName on SLA form to coincide with API new fixes.

## [8.17.1]
* CXV1-15294 - Outbound Identifiers - Spec Fails when creating outbound identifier list. SDK treating Description as a required field.

## [8.17.0]
* CXV1-15259 - Added screen pop for click to dial/sms for salesforce

## [8.16.1]
* CXV1-13807 - Filter Private Functions from documentation

## [8.16.0]
* <no-ticket> - Added roles to entities modules and other get entity functions

## [8.15.2]
* CXV1-14896 - Screenpop for Skylight popping 2 browser windows (1 correct, 1 blank)

## [8.15.1]
* CXV1-13732 - Make sure we catch SDK JavaScript exceptions in Sentry

## [8.15.0]
* CXV1-13804 - Setup Codox Documentation Generation
* CXV1-13805 - Update def-sdk-fn macro to accept docstrings
* CXV1-13806 - Update all module functions to adhere to the updated macro
* <no-ticket> - Added "Documentation" section to README.md

## [8.14.0]
* CXV1-13733 - Update SDK to log errors to Kibana
* <no-ticket> - Remove Lumbajack as a dependency

## [8.13.2]
* CXV1-15284 - Hide "Failed to get available stats (code:12003) error" when hyperion is down

## [8.13.1]
* CXV1-15239 - Do not cease heatbeating until presence returns a 4xx

## [8.13.0]
* CXV1-15006 - Transfer menu agent refresh button should be greyed out until response is returned

## [8.12.0]
* CXV1-15094 - New User unable to monitor interactions

## [8.11.2]
* CXV1-15038 - Update abandon-threshold value depending of which abandon-type is selected.

## [8.11.1]
* CXV1-15198: Deleted voice heartbeat topic and error

## [8.11.0]
* CXV1-15088 - Added function session.clearMonitoredInteraction() so we can clear it when we catch an error

## [8.10.4]
* CXV1-15134 - Update API retry logic to any non 2xx/4xx error code (was previously any 5xx code)

## [8.10.3]
* CXV1-14962 - Update on batch request logic in Agent Desktop.

## [8.10.2]
* CXV1-15198 - Remove interaction hearbeats

## [8.10.1]
* CXV1-14962 - Batch request logic in Agent Desktop.

## [8.10.0]
* CXV1-14968 - Added Custom Metrics Services for retrieve and change information.

## [8.9.0]
* CXV1-14754 - Add ability to enter a silent session

## [8.8.0]
* CXV1-14931 - Outbound Identification - Create remaining sdk functions for outbound ani.

## [8.7.2]
* CXV1-14662 - Fix SSO identity-ui error handling

## [8.7.1]
* Add us-east-1-test to environment spec to support ilities environment

## [8.7.0]
* CXV1-14265 - Add update and create outbound identifier lists functions

## [8.6.6]
* CXV1-14703 - Fix org-id assignment in salesforce lightning

## [8.6.5]
* CXV1-14703 - Call apex class function to add org-id in hooks

## [8.6.4]
* CXV1-14274 - Email Reply Failing to Send

## [8.6.3]
* CXV1-14660 - hookBy parameter added to send-unassign-interrupt request in salesforce

## [8.6.2]
* CXV1-14660 - resourceId parameter added to send-unassign-interrupt request in salesforce

## [8.6.1]
* CXV1-13266 - Transferring Calls between agents and reporting on the last agent who handled the call as opposed to the 1st Agent

## [8.6.0]
* CXV1-14207 - Update setDirection function to allow "agent-initiated"
* CXV1-14205 - Update the direction from "outbound" to "agent-initiated" in click-to-XXX

## [8.5.1]
* CXV1-13095 - Change the error level severity of the microphone error

## [8.5.0]
* CXV1-12423 - Skylight for Zendesk window not properly resizing

## [8.4.1]
* SDK module to detect when the microphone is enabled within the browser

## [8.4.0]
* Add update default tenant function

## [8.3.0]
* CXV1-12770 - SDK Functions to retrieve Outbound Identifiers

## [8.2.0]
* CXV1-13838 - Added shared parameter to create and update lists

## [8.1.1]
* CXV1-13643 - Fixed SalesForce(Classic and Lightning) setHeight() issue where previously set heights were cached and not overwritten.

## [8.1.0]
* CXV1-13643 - Skylight toolbar cut off in SalesForce

## [8.0.0]
* CXV1-13512 - Added reporting args for auto dismiss scripts

## [7.1.0]
* CXV1-13897 - Save interaction monitoring call id in state and add a function to retrieve it

## [7.0.0]
* CXV1-13762 - Make entities function's responses consistent by always returning the api response directly or the altered response in the same shape: `{ result : { ... }}`

## [6.20.1]
* <no-ticket> - Ensure the Cognito event listener only gets triggered once

## [6.20.0]
* CXV1-13761 - Get function for protected branding

## [6.19.6]
* CXV1-13718 - Updated to make sure it's backward compatible

## [6.19.5]
* CXV1-13718 - Fixed auth-info response for SSO

## [6.19.4]
* <no-ticket> - Fixed Lightning to Classic screen pop

## [6.19.3]
* <no-ticket> - Fixed Focusing interactions for Salesforce

## [6.19.2]
* <no-ticket> - Fixed Salesforce Lightning work item not assigning on transfer

## [6.19.1]
* <no-ticket> - Fixed list item editing/deleting
* <no-ticket> - Fixed auto-assigned interactions not being able to be unassigned (Salesforce Lightning)

## [6.19.0]
* CXV1-12492 - Get functions for artifacts

## [6.18.1]
* Fixed Salesforce Lightning work items
* Fixed transfers between salesforce classic and salesforce lightning
* Fixed generic list item keys

## [6.18.0]
* Added Bulk Stat Query function
* Fixed Salesforce Lightning hook-by id

## [6.17.0]
* CXV1-13444 - CRUD functions for Custom Email Templates

## [6.16.2]
* <No Jira> - Changed getGroups and getSkills topic response to be entities instead of reporting.
* <No Jira> - Added reporting as a base module.

## [6.16.1]
* CXV1-12495 - Made response from CxEngage.salesforceLightning.isVisible() dynamic instead of just hard-coded to true

## [6.16.0]
* Added Groups and Skills entity functions
* Added Supervisor Mode
* Silent Monitoring Changes

## [6.15.0]
* CXV1-13353 - Bulk Upload and List Download

## [6.14.1]
* CXV1-12282 - fixed update entity call

## [6.14.0]
* CXV1-12814 - Added getListTypes function

## [6.13.1]
* CXV1-12814 - Fix error responses for some of Lists functions

## [6.13.0]
* CXV1-12814 - Add Lists API to the SDK

## [6.12.5]
* CXV1-12732 - Return interaction-fatal error on script send 404

## [6.12.4]
* CXV1-12834 - Automatically add hooks for work items

## [6.12.3]
* CXV1-11915 - Error Banner (Code: 7024) Has Spelling Error

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
