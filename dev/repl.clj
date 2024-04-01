(ns repl
  (:require [cemerick.pomegranate :as pomegranate]
            [cemerick.pomegranate.aether :as aether]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.namespace.repl :as tn]
            [clojure.test]
            [mount.core :as mount]
            [clojure.tools.logging :as log]))

(def aliases
  {'config   'base.configuration.api
   'query    'calc.db.query
   'postgres 'calc.db.postgres
   'io       'clojure.java.io
   'log      'clojure.tools.logging
   'mount    'mount.core})

(defn deinit-aliases []
  (doseq [[from _] aliases]
    (ns-unalias 'repl from)))

(defn init-aliases []
  (deinit-aliases)
  (doseq [[from to] aliases]
    (alias from to)))

(defn add-dependency [coordinates]
  (pomegranate/add-dependencies
    :coordinates [coordinates]
    :repositories (merge aether/maven-central
                         {"clojars" "https://clojars.org/repo"})))

;;

(def db-query-wrappers-declaration-path "src/main/clj/calc/db/query.clj")

(def db-query-wrappers-declaration-template "(ns calc.db.query)

(declare
  <placeholder>)")

(defn generate-db-query-wrappers-declaration []
  (->> (io/file "src/main/resources/db/postgres/queries.sql")
       (slurp)
       (string/split-lines)
       (mapv string/trim)
       (filterv (fn [v] (string/starts-with? v "-- :name")))
       (mapv (fn [v] (string/split v #"\s")))
       (flatten)
       (filterv (fn [v] (re-find #"^\w+" v)))
       (sort)
       (string/join "\n  ")
       (string/replace db-query-wrappers-declaration-template "<placeholder>")
       (spit db-query-wrappers-declaration-path)))

;; Lifecycle
(defn init []
  (try
    (in-ns 'repl)
    (generate-db-query-wrappers-declaration)
    (deinit-aliases)
    (let [result (tn/refresh-all)]
      (when (instance? Throwable result)
        (throw result))
      (mount/start)
      (init-aliases)
      :done)
    (catch Throwable e
      (log/error e "Unable to init app"))))

(defn reset []
  (try
    (deinit-aliases)
    (mount/stop)
    (generate-db-query-wrappers-declaration)
    (tn/refresh :after 'mount.core/start)
    (init-aliases)
    :done
    (catch Throwable e
      (log/error e "Unable to reset app"))))

(defn keyword->state [kw]
  (get {:web        'calc.server/*runner*
        :config     'calc.config/*loader*
        :postgres   'calc.db.postgres/*database*
        :migrations 'calc.migrations/*runner*}
       kw))

(defn r
  "Reloads changed namespaces, and restarts the defstates within them.
   Accepts optional keywords representing defstates to restart (regardless of
   a need for it)
   Example:
     (r :config :db)
   Refer to keyword->state to see what states can be restarted in this way"
  [& states]
  (try
    (when (= (ns-name *ns*) 'repl)
      (deinit-aliases))
    (let [states (->> states
                      (map (fn [kw]
                             (let [state (keyword->state kw)]
                               (when-not state
                                 (throw (Exception. (format "State %s does not exist" kw))))
                               (ns-resolve 'repl state)))))
          _      (when (seq states)
                   (apply mount/stop states))
          result (tn/refresh)]
      (when (instance? Throwable result)
        (throw result))
      (when (seq states)
        (apply mount/start states))
      (when (= (ns-name *ns*) 'repl)
        (init-aliases))
      :done)
    (catch Throwable e
      (log/error e "Unable to reset app"))))

(defn run-tests
  ([]
   (run-tests #"calc.*?\-test"))
  ([regular-expression]
   (mount/stop)
   (clojure.test/run-all-tests regular-expression)))
