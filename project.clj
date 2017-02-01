(defproject org.serenova/client-sdk-core "1.1.2-SNAPSHOT"
  :description "Client SDK Core"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.7.1"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.391"
                  :exclusions [org.clojure/tools.reader]]
                 [camel-snake-kebab "0.4.0"]
                 [cljsjs/aws-sdk-js "2.2.41-3"]
                 [binaryage/devtools "0.8.3"]
                 [cljsjs/paho "1.0.1-0"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [org.serenova/client-sdk-utils "0.1.0-SNAPSHOT"]
                 [org.serenova/lumbajack "0.1.0-SNAPSHOT"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]]
  :plugins [[lein-figwheel "0.5.8"]
            [lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]
            [lein-shell "0.5.0"]
            [lein-doo "0.1.7"]]
  :source-paths ["src"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]
                :figwheel {}
                :compiler {:main client_sdk.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/client_sdk.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}
               {:id "test"
                :source-paths ["src" "test"]
                :compiler {:main client-sdk.runner
                           :output-dir "resources/public/js/compiled/test"
                           :output-to "resources/public/js/compiled/test/testable.js"
                           :optimizations :whitespace}}
               {:id "prod"
                :source-paths ["src"]
                :compiler {:main cxengage
                           :output-to "release/cxengage-js-sdk.min.js"
                           :optimizations :none}}]}
  :figwheel {}
  :profiles {:dev {:dependencies [[binaryage/devtools "0.8.2"]
                                  [karma-reporter "0.3.0"]
                                  [org.clojure/test.check "0.9.0"]
                                  [figwheel-sidecar "0.5.8"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   :source-paths ["src" "dev" "env"]
                   :repl-options {:init (set! *print-length* 50)
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}
  :aliases {"prod" ["do" "clean," "cljsbuild" "once" "prod"]
            "test" ["doo" "phantom" "test" "once"]}
  :doo {:build "test"
        :alias {:default [:phantom]}}
  :repositories [["releases" {:url "http://nexus.cxengagelabs.net/content/repositories/releases/"
                              :snapshots false}]
                 ["snapshots" {:url "http://nexus.cxengagelabs.net/content/repositories/snapshots/"
                               :update :always}]])
