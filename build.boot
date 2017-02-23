(set-env!

 :resource-paths #{"resources"}

 :source-paths #{}

 :dependencies '[[org.clojure/tools.nrepl "0.2.12" :scope "test"]
                 [org.clojure/clojure "1.9.0-alpha14"]
                 [org.clojure/clojurescript "1.9.473"]
                 [org.clojure/core.async "0.2.395"
                  :exclusions [org.clojure/tools.reader]]

                 [adzerk/boot-cljs "1.7.228-2" :scope "test"]
                 [adzerk/boot-cljs-repl "0.3.2" :scope "test"]
                 [adzerk/boot-reload "0.5.1" :scope "test"]
                 [pandeiro/boot-http "0.7.6" :scope "test"]
                 [com.cemerick/piggieback "0.2.1" :scope "test"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [weasel "0.7.0" :scope "test"]
                 [crisptrutski/boot-cljs-test "0.3.0" :scope "test"]
                 [camel-snake-kebab "0.4.0"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [cljsjs/aws-sdk-js "2.2.41-4"]
                 [cljsjs/paho "1.0.1-0"]
                 [binaryage/devtools "0.9.1"]
                 [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]

                 [org.serenova/cxengage-cljs-utils "1.0.0"]
                 [org.serenova/lumbajack "1.0.1-SNAPSHOT"]]

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
  (set-env! :source-paths #(conj % "src/cljs" "src/dev_cljs"))
  (task-options! cljs {:compiler-options {:optimizations :none
                                          :source-map true
                                          ;:verbose true
                                          }})
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
                                          :source-map true
                                          :verbose true}})
  (comp (cljs)))

(ns-unmap 'boot.user 'test)

;; =============================================
;; Below are the only boot tasks you should ever have to run.
;; =============================================

(deftask make-prod-release []
  (comp (production*)
        (sift :move {#"main.js" "cxengage-javascript-sdk.min.js"})
        (sift :move {#"cxengage-javascript-sdk.min.js.map" "main.js.map"})
        (target :dir #{"release"})))

(deftask test-once []
  (comp (testing*)
        (test-cljs :js-env :phantom)))

(deftask test []
  (comp (testing*)
        (watch)
        (test-cljs :js-env :phantom)))

(deftask dev []
  (comp (development*)
        (run*)))
