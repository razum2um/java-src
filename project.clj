(defproject java-src "0.1.1"
  :description "Helps you to bring pure *.java files to your project"
  :url "http://github.com/razum2um/java-src"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :main ^:skip-aot java-src.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
