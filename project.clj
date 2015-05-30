(defproject rat "0.1.0"
  :description "Lithuanian RAT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [org.apache.lucene/lucene-analyzers-common "4.10.3"]
                 [ring "1.3.1" :exclusions [org.clojure/tools.reader]]
                 [compojure "1.2.0"]
                 [environ "1.0.0"]
                 [enlive "1.1.5"]
                 [org.clojure/clojurescript "0.0-2665"]
                 [enfocus "2.1.1"]
                 [jayq "2.5.2"]]
  :source-paths ["src/clj" "src/cljs"]
  :plugins [[lein-ring "0.8.12" :exclusions [org.clojure/clojure]]
            [lein-cljsbuild "1.0.4" :exclusions [org.clojure/clojurescript]]]
  :main ^:skip-aot rat.core
  :javac-options ["-target" "1.6" "-source" "1.6"]
  :cljsbuild {:builds
              [{:source-paths ["src/cljs"]
                :compiler {:output-to "resources/public/js/rat.js"
                           :optimizations :whitespace}}]}
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
