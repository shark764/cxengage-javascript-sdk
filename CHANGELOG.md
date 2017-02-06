# Change Log

## [Unreleased]
* remove boot audio playing during build (causing jenkins builds to fail)

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
