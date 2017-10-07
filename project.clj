(defproject caesium "0.10.0-SNAPSHOT"
  :description "libsodium for clojure"
  :url "https://github.com/lvh/caesium"
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.github.jnr/jnr-ffi "2.1.0"]
                 [commons-codec/commons-codec "1.10"]
                 [byte-streams "0.2.2"]
                 [org.clojure/math.combinatorics "0.1.3"]
                 [medley "0.8.3"]]
  :main ^:skip-aot caesium.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[criterium "0.4.4"]
                                  [org.clojure/test.check "0.9.0"]
                                  [com.gfredericks/test.chuck "0.2.7"]
                                  [com.taoensso/timbre "4.7.3"]]}
             :test {:plugins [[lein-cljfmt "0.5.7"]
                              [lein-kibit "0.1.2"]
                              [jonase/eastwood "0.2.3"]
                              [lein-codox "0.9.4"]
                              [lein-cloverage "1.0.7-SNAPSHOT"]]}
             :benchmarks {:source-paths ["test/"]
                          :test-paths ^:replace ["benchmarks/"]}}
  :codox {:metadata {:doc/format :markdown}
          :output-path "doc"}
  :global-vars {*warn-on-reflection* true}
  :aliases {"benchmark" ["with-profile" "+benchmarks" "test"]})
