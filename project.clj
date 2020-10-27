(defproject f0bec0d/regelwerk "0.1.0-SNAPSHOT"
  :description "Try CLI for Datalog"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [datascript "1.0.1"]]
  :main ^:skip-aot regelwerk.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
