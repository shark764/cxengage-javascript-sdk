(set-env!

 :resource-paths #{"resources"}

 :source-paths #{}

 :dependencies '[[org.clojure/core.async "0.3.442" :exclusions [org.clojure/tools.reader]]
                 [org.clojure/clojure "1.9.0-alpha16"]
                 [org.clojure/clojurescript "1.9.521"]

                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [cljsjs/aws-sdk-js "2.2.41-4"]
                 [binaryage/devtools "0.9.4"]
                 [camel-snake-kebab "0.4.0"]
                 [funcool/promesa "1.8.1"]
                 [cljsjs/paho "1.0.1-0"]
                 [cljs-ajax "0.6.0"]

                 [serenova/cljs-sdk-utils "0.0.14"]
                 [serenova/lumbajack "3.0.1"]

                 [crisptrutski/boot-cljs-test "0.3.0" :scope "test"]
                 [org.clojure/tools.nrepl "0.2.13" :scope "test"]
                 [com.cemerick/piggieback "0.2.1" :scope "test"]
                 [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
                 [tolitius/boot-check "0.1.4" :scope "test"]
                 [adzerk/boot-reload "0.5.1" :scope "test"]
                 [pandeiro/boot-http "0.8.0" :scope "test"]
                 [adzerk/boot-cljs "2.0.0" :scope "test"]
                 [weasel "0.7.0" :scope "test"]]

 :repositories #(apply conj %
                       [["releases" {:url "http://nexus.cxengagelabs.net/content/repositories/releases/"
                                     :snapshots false}]
                        ["snapshots" {:url "http://nexus.cxengagelabs.net/content/repositories/snapshots/"
                                      :update :always}]
                        ["thirdparty" {:url "http://nexus.cxengagelabs.net/content/repositories/thirdparty/"
                                       :snapshots false}]]))

(require
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[pandeiro.boot-http :refer [serve]]
 '[crisptrutski.boot-cljs-test :refer [test-cljs]]
 '[tolitius.boot-check :as check]
 'boot.repl)

(swap! boot.repl/*default-dependencies*
       concat '[[cider/cider-nrepl "0.15.0-SNAPSHOT"]])

(swap! boot.repl/*default-middleware*
       conj 'cider.nrepl/cider-middleware)

(deftask build* []
  (comp (cljs)))

(deftask run* []
  (comp (serve :port 3449)
        (watch)
        (cljs-repl)
        (reload)
        (build*)))

(deftask development* []
  (set-env! :source-paths #(conj % "src/cljs" "src/dev_cljs" "src/prod_cljs"))
  (task-options! cljs {:compiler-options {:optimizations :none
                                          :source-map true
                                          :parallel-build true
                                          :compiler-stats true
                                          :source-map-timestamp true
                                          :cache-analysis true
                                          :recompile-dependents false
                                          :warnings {:single-segment-namespace false}
                                          :source-map-path "resources/public"}})
  identity)

(deftask testing* []
  (set-env! :source-paths #(conj % "src/cljs" "test/cljs"))
  identity)

(deftask production* []
  (set-env! :source-paths #(conj % "src/cljs" "src/prod_cljs"))
  (task-options! cljs {:compiler-options {:optimizations :advanced
                                          :externs ["externs.js"]
                                          :pseudo-names true
                                          :output-wrapper true
                                          :compiler-stats true
                                          :anon-fn-naming-policy :mapped
                                          :pretty-print true
                                          :source-map true
                                          :parallel-build true
                                          :static-fns true
                                          :language-in :ecmascript5
                                          :print-input-delimiter true
                                          :language-out :ecmascript5
                                          :verbose true}})
  (comp (cljs)))

(ns-unmap 'boot.user 'test)

;; =============================================
;; Below are the only boot tasks you should ever have to run.
;; =============================================

(deftask make-prod-release []
  (comp (production*)
        (target :dir #{"release"})))

(deftask test-once []
  (comp (testing*)
        (test-cljs :js-env :phantom
                   :exit? true)))

(deftask check-sources []
  (set-env! :source-paths #{"src"})
  (comp
   (check/with-kibit)))

(deftask test []
  (comp (testing*)
        (watch)
        (test-cljs :js-env :phantom
                   ;;:namespaces ["cxengage-javascript-sdk.modules.interaction-test"]
                   )))

(deftask dev []
  (comp (development*)
        (run*)))
