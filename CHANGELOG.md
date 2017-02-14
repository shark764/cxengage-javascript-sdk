# Change Log

## [Unreleased]
* remove boot audio playing during build (causing jenkins builds to fail)
### Changed
* Voice Module - Added queue transfers, and pubsub topics
* Voice Module - Added transfer functionality (Warm/Cold)
* Crud Module - Added to ability to retrive a specific Queue or User, or all Queues and all Users
* Alter interrupt function to accommodate complex interrupt types
* Modules no longer live in core state.
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

## [1.1.1]
* N/A (testing jenkins release script stuff)

## [1.0.3]
* Added contacts Module.

## [1.0.0] - 2017-01-18
* Initial release

[Unreleased]: https://github.com/liveops/client-sdk-core/tags/1.0.0...HEAD
[1.0.0]: https://github.com/liveops/client-sdk-core/compare/1.0.0
