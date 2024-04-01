(defproject com.dzer6/calc "1.0.0"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.memoize "1.0.257"]
                 [org.clojure/tools.logging "1.2.4"]

                 ;; Logging: use logback with slf4j, redirect JUL, JCL and Log4J:
                 [ch.qos.logback/logback-core "1.4.14"]
                 [ch.qos.logback/logback-classic "1.4.14"]
                 [ch.qos.logback.contrib/logback-json-classic "0.1.5"]
                 [ch.qos.logback.contrib/logback-jackson "0.1.5"]
                 [org.codehaus.janino/janino "3.1.11"]
                 [org.slf4j/slf4j-api "2.0.9"]
                 [org.slf4j/jul-to-slf4j "2.0.9"]           ; JUL to SLF4J
                 [org.slf4j/jcl-over-slf4j "2.0.9"]         ; JCL to SLF4J
                 [org.slf4j/log4j-over-slf4j "2.0.9"]       ; Log4j to SLF4J

                 [mount "0.1.17"]

                 ;; Common:
                 [camel-snake-kebab "0.4.3"]
                 [prismatic/schema "1.4.1"]
                 [metosin/schema-tools "0.13.1"]
                 [clj-time "0.15.2"]
                 [clj-fuzzy "0.4.1"]
                 [slingshot "0.12.2"]

                 ;; REST API
                 [ring "1.11.0" :exclusions [ring/ring-jetty-adapter]]
                 [ring/ring-core "1.11.0"]
                 [ring/ring-defaults "0.4.0"]
                 [info.sunng/ring-jetty9-adapter "0.30.4"]
                 [metosin/compojure-api "2.0.0-alpha31" :exclusions [frankiesardo/linked
                                                                     metosin/ring-swagger-ui]]
                 [metosin/ring-swagger-ui "5.9.0"]

                 ;; Configuration:
                 [cprop "0.1.19"]

                 ;; Memoizing
                 [com.taoensso/encore "3.74.0"]

                 ;; Expression evaluating
                 [org.mvel/mvel2 "2.5.2.Final"]

                 ;; JDBC
                 [com.zaxxer/HikariCP "5.1.0"]
                 [org.postgresql/postgresql "42.7.1"]
                 [com.github.seancorfield/next.jdbc "1.3.909"]
                 [com.layerware/hugsql-core "0.5.3"]
                 [com.layerware/hugsql-adapter-next-jdbc "0.5.3" :exclusions [seancorfield/next.jdbc]]
                 [org.flywaydb/flyway-core "10.4.1"]
                 [org.flywaydb/flyway-database-postgresql "10.4.1"]

                 ;; JSON encoding and decoding:
                 [metosin/jsonista "0.3.8"]
                 [com.fasterxml.jackson.core/jackson-core "2.16.1"]
                 [com.fasterxml.jackson.core/jackson-annotations "2.16.1"]]

  :main calc.main
  :source-paths ["src/main/clj"]
  :resource-paths ["src/main/resources"]
  :test-paths ["src/test/clj" "src/test/resources"]
  :uberjar-name "app.jar"

  :repl-options {:port 4010 :init-ns user}

  :profiles {:user    {:source-paths          ["dev"
                                               "src/main/clj"]

                       :resource-paths        ["src/test/resources"
                                               "src/main/resources"]

                       :test2junit-output-dir "./target/test-reports/"

                       :dependencies          [[com.cemerick/pomegranate "1.1.0"]
                                               [org.clojure/tools.nrepl "0.2.13"]

                                               [clj-http "3.12.3"]

                                               [org.clojure/test.check "1.1.1" :exclusions [org.clojure/clojure]]
                                               [com.gfredericks/test.chuck "0.2.14" :exclusions [org.clojure/clojure
                                                                                                 instaparse]]

                                               [net.java.dev.jna/jna "5.14.0"]
                                               [org.testcontainers/testcontainers "1.19.7"]
                                               [org.testcontainers/postgresql "1.19.7"]]}
             :uberjar {:aot         :all
                       :omit-source true}})