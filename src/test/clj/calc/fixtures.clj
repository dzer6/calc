(ns calc.fixtures
  (:require [base.modules.configuration :as core.configuration]
            [base.modules.server :as core.server]
            [calc.config]
            [calc.db.postgres :as postgres]
            [calc.migrations]
            [calc.server :as web-server]
            [calc.test-utils :as tu]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [cprop.source :as cs]
            [mount.core :as mount]
            [next.jdbc :as jdbc])
  (:import (org.testcontainers.containers PostgreSQLContainer)
           (org.testcontainers.containers.output Slf4jLogConsumer)
           (org.testcontainers.utility DockerImageName)))

(def postgres-image "postgis/postgis:16-3.4-alpine")

(defn with-test-config [f]
  (with-redefs [cs/from-env        (fn [] (log/info "[with-test-config] skip ENV vars loading"))
                cs/read-system-env (fn [_] (log/info "[with-test-config] skip system vars loading"))]
    (binding [core.configuration/override-resource-path "test-config.edn"]
      (log/info "[with-test-config]")
      (try
        (mount/start #'calc.config/*loader*)
        (f)
        (catch Exception e
          (do-report {:type     :error
                      :message  "Uncaught exception, not in assertion."
                      :expected nil
                      :actual   e}))
        (finally
          (mount/stop #'calc.config/*loader*))))))

(defn with-web-server [app-config]
  (fn [f]
    (log/info "[with-web-server]")
    (let [http-server-holder (atom nil)
          port               (tu/get-free-port)]
      (try
        (swap! app-config assoc-in [:web :server :host] "localhost")
        (swap! app-config assoc-in [:web :server :port] port)
        (core.server/start-server! http-server-holder
                                   (web-server/http-handler @app-config)
                                   @app-config)
        (f)
        (catch Exception e
          (do-report {:type     :error
                      :message  "Uncaught exception, not in assertion."
                      :expected nil
                      :actual   e}))
        (finally
          (core.server/stop-server! http-server-holder))))))

(defn with-postgres [app-config]
  (fn [f]
    (log/info "[with-postgres]")
    (let [log-consumer (Slf4jLogConsumer. (.get-logger log/*logger-factory* "postgres-container"))
          container    (-> postgres-image
                           (DockerImageName/parse)
                           (.asCompatibleSubstituteFor "postgres")
                           (PostgreSQLContainer.))]
      (try
        (.start container)
        (.followOutput container log-consumer)
        (swap! app-config assoc-in [:postgres :server-name] (.getContainerIpAddress container)) ; TODO Refactor
        (swap! app-config assoc-in [:postgres :port-number] (.getMappedPort container 5432))
        (mount/start #'calc.db.postgres/*database*)
        (f)
        (catch Exception e
          (do-report {:type     :error
                      :message  "Uncaught exception, not in assertion."
                      :expected nil
                      :actual   e}))
        (finally
          (mount/stop #'calc.db.postgres/*database*)
          (.stop container))))))

(defn with-postgres-migrations [f]
  (log/info "[with-postgres-migrations]")
  (try
    (mount/start #'calc.migrations/*runner*)
    (f)
    (catch Exception e
      (do-report {:type     :error
                  :message  "Uncaught exception, not in assertion."
                  :expected nil
                  :actual   e}))
    (finally
      (mount/stop #'calc.migrations/*runner*))))

(defn with-postgres-seeding [seeding-file-path]
  (fn [f]
    (try
      (log/infof "[with-postgres-seeding] sql file path: %s" seeding-file-path)
      (->> (format seeding-file-path)
           (slurp)
           (vector)
           (jdbc/execute! @postgres/db))
      (f)
      (catch Exception e
        (do-report {:type     :error
                    :message  "Uncaught exception, not in assertion."
                    :expected nil
                    :actual   e})))))