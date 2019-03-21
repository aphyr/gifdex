(defproject gifdex.core "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [aleph "0.4.2-alpha12"]
                 [ring "1.5.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/data.codec "0.1.1"]
                 [fipp "0.6.17"]
                 [cheshire "5.8.1"]
                 [spootnik/unilog "0.7.17"]
                 [pandect "0.6.0"] ; For etags
                 [org.clojure/core.match "0.3.0-alpha4"]
                 [com.stuartsierra/component "0.3.2"]
                 [org.javassist/javassist "3.17.1-GA"]
                 [bk/ring-gzip "0.2.0"]
                 [dom-top "1.0.0"]]
  :main gifdex.core
  :repl-options {:init-ns gifdex.core}
  :profiles {:uberjar {:aot       :all
                       :jvm-opts  ["-Dclojure.compiler.direct-linking=true"]}})
