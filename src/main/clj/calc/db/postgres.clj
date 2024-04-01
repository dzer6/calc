(ns calc.db.postgres
  (:require [base.configuration.api :as config]
            [base.modules.postgres :as postgres-component]
            [clojure.tools.logging :as log]
            [mount.core :as mount]
            [next.jdbc :as jdbc]
            [calc.config :as app-config]))

(def queries-file-path "db/postgres/queries.sql")

(defonce db (atom nil))

(defn start! []
  (when (and (nil? @db) (config/feature-on? @app-config/holder :database :postgres))
    (log/info "Starting Postgres connection")

    (postgres-component/generate-query-wrappers 'calc.db.query
                                                queries-file-path)

    (->> (postgres-component/connect (config/get @app-config/holder :postgres)
                                     :enable-sql-logger (config/feature-on? @app-config/holder :database :print-queries))
         (reset! db))))

(defn stop! []
  (when (some? @db)
    (log/info "Stopping Postgres connection")
    (postgres-component/close @db)
    (reset! db nil)
    (postgres-component/remove-generated-query-wrappers 'calc.db.query
                                                        queries-file-path)))

(defn is-connection-alive []
  (try
    (-> (jdbc/execute! @db ["SELECT true AS okay;"])
        (first)
        :okay
        (true?))
    (catch Exception e
      (log/warn e "Postgres connection is not alive")
      false)))

(declare ^:dynamic *database*)

(mount/defstate ^:dynamic *database*
  :start (start!)
  :stop (stop!))
