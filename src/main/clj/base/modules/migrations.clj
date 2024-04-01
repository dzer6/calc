(ns base.modules.migrations
  (:import (javax.sql DataSource)
           (org.flywaydb.core Flyway)))

(defn migrate [^DataSource db & [{:keys [enable-baseline-on-migrate]
                                  :or   {enable-baseline-on-migrate false}}]]
  (-> (Flyway/configure)
      (.baselineOnMigrate enable-baseline-on-migrate)
      (.dataSource db)
      (.load)
      (.migrate)))