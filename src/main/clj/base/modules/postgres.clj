(ns base.modules.postgres
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [next.jdbc.prepare :as prepare]
            [next.jdbc.connection :as connection]
            [clojure.tools.logging :as log]
            [base.data.json :as json]
            [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [hugsql.adapter :as adapter]
            [camel-snake-kebab.core :as csk]
            [clj-time.coerce :as tc])
  (:import (org.postgresql.util PGobject)
           (clojure.lang IPersistentVector IPersistentMap Keyword)
           (java.sql PreparedStatement Array)
           (com.zaxxer.hikari HikariDataSource)
           (java.util Date List Set)
           (org.joda.time DateTime)))

;;; PGObject -> Json
(defn ->pgobject
  "Transforms Clojure data to a PGobject that contains the data as
  JSON. PGObject type defaults to `jsonb` but can be changed via
  metadata key `:pgtype`"
  [x]
  (let [pgtype (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pgtype)
      (.setValue (json/write x)))))

(defn <-pgobject
  "Transform PGobject containing `json` or `jsonb` value to Clojure data."
  [^PGobject v]
  (let [type  (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (when value
        (with-meta (json/parse value) {:pgtype type}))
      value)))

;; if a SQL parameter is a Clojure hash map or vector, it'll be transformed
;; to a PGobject for JSON/JSONB:
(extend-protocol prepare/SettableParameter
  Keyword
  (set-parameter [kw ^PreparedStatement s i]
    (let [nmspc (namespace kw)
          v     (if nmspc (str nmspc "/" (name kw)) (name kw))]
      (.setObject s i v)))
  DateTime
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (tc/to-sql-time m)))
  Date
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (tc/to-sql-time m)))
  List
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (->pgobject m)))
  Set
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (->pgobject m)))
  IPersistentMap
  (set-parameter [m ^PreparedStatement s i]
    (.setObject s i (->pgobject m)))
  IPersistentVector
  (set-parameter [v ^PreparedStatement s i]
    (.setObject s i (->pgobject v))))

;; if a row contains a PGobject then we'll convert them to Clojure data
;; while reading (if column is either "json" or "jsonb" type):
(extend-protocol rs/ReadableColumn
  Array
  (read-column-by-label [^Array v _]
    (vec (.getArray v)))
  (read-column-by-index [^Array v _2 _3]
    (vec (.getArray v)))

  PGobject
  (read-column-by-label [^PGobject v _]
    (<-pgobject v))
  (read-column-by-index [^PGobject v _2 _3]
    (<-pgobject v))

  java.sql.Date
  (read-column-by-label [^PGobject v _]
    (tc/to-date v))
  (read-column-by-index [^PGobject v _2 _3]
    (tc/to-date v))

  java.sql.Timestamp
  (read-column-by-label [^PGobject v _]
    (tc/to-date-time v))
  (read-column-by-index [^PGobject v _2 _3]
    (tc/to-date-time v)))
;;; end PGObject -> Json

(defn get-version [db]
  (-> db
      (jdbc/execute-one! ["SHOW server_version"])
      :server-version))

(defn sql-logger [_ sql-params]
  {:start      (System/nanoTime)
   :sql-params sql-params})

(defn result-logger [_ {:keys [start sql-params]} result]
  (log/infof "query %s" {:query   sql-params
                         :elapsed (Math/round (/ (double (- (System/nanoTime) start)) 1000000.0))
                         :result  (if (coll? result) (count result) result)}))

(defn build-connection-pool [{:keys [server-name port-number database-name username password leak-detection-threshold jdbc-url]}]
  (let [db-spec (if jdbc-url
                  {:jdbcUrl jdbc-url}
                  {:jdbcUrl                (format "jdbc:postgresql://%s:%d/%s" server-name port-number database-name)
                   :username               username
                   :password               password
                   :leakDetectionThreshold leak-detection-threshold})]
    (connection/->pool HikariDataSource db-spec)))

(defn connect [db-spec & {:keys [enable-sql-logger]
                          :or   {enable-sql-logger false}}]
  (log/info "Connecting to Postgres...")
  (-> db-spec
      (build-connection-pool)
      (cond-> enable-sql-logger
              (jdbc/with-logging sql-logger result-logger))))

(defn close [db]
  (log/info "Closing Postgres connection...")
  (.close ^HikariDataSource (jdbc/get-datasource db)))

(hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc jdbc/unqualified-snake-kebab-opts))

;;;

(defn db-fn*
  "Given parsed sql `psql` and optionally a `command`, `result`, and `options`,
  return an anonymous function that can run hugsql database
  execute/queries and supports hugsql parameter replacement"
  ([psql] (db-fn* psql :default :default {}))
  ([psql command] (db-fn* psql command :default {}))
  ([psql command result] (db-fn* psql command result {}))
  ([psql command result options]
   (fn y
     ([db] (y db {} {}))
     ([db param-data] (y db param-data {}))
     ([db param-data opts & command-opts]
      (let [o          (merge hugsql/default-db-options options opts
                              {:command command :result result})
            o          (if (seq command-opts)
                         (assoc o :command-options command-opts) o)
            a          (or (:adapter o) (hugsql/get-adapter))
            param-data (->> param-data
                            (map (fn [[k v]] [(csk/->snake_case_keyword k) v]))
                            (into {}))]
        (try
          (as-> psql x
                (hugsql/prepare-sql x param-data o)
                ((resolve (hugsql/hugsql-command-fn command)) a db x o)
                ((resolve (hugsql/hugsql-result-fn result)) a x o))
          (catch Exception e
            (adapter/on-exception a e))))))))

(defn generate-query-wrappers [queries-ns file]
  (let [queries-ns (create-ns queries-ns)]
    (binding [*ns* queries-ns]
      (with-redefs [hugsql/db-fn* db-fn*]
        (hugsql/def-db-fns file)))))

(defn remove-generated-query-wrappers [queries-ns queries-file-path]
  (->> (ns-map queries-ns)
       (filterv (fn [[_ v]] (= queries-file-path (some-> v meta :file))))
       (mapv (fn [[k _]] (ns-unmap queries-ns k)))))