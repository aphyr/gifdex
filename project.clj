(defproject gifdex.core "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [aleph "0.4.6"]
                 [ring "1.9.5"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.clojure/data.codec "0.1.1"]
                 [fipp "0.6.25"]
                 [cheshire "5.10.2"]
                 [spootnik/unilog "0.7.29"]
                 [pandect "1.0.2"] ; For etags
                 [org.clojure/core.match "1.0.0"]
                 [com.stuartsierra/component "1.1.0"]
                 [org.javassist/javassist "3.17.1-GA"]
                 [bk/ring-gzip "0.3.0"]
                 [dom-top "1.0.7"]]
  :main gifdex.core
  :repl-options {:init-ns gifdex.core}
  :profiles {:uberjar {:aot       :all
                       :jvm-opts  ["-Dclojure.compiler.direct-linking=true"]}})
