(ns base.modules.configuration
  (:require [clojure.tools.logging :as log]
            [cprop.source :as cs]
            [cprop.core :as cp]
            [base.modules.signals :as signal]
            [schema-tools.core :as sc]
            [schema-tools.coerce :as co]
            [schema.core :as s]
            [base.utils.security :as sec]
            [base.utils.function :as uf])
  (:import (java.util MissingResourceException)))

(def ^:dynamic resource-default-path "default-config.edn")
(def ^:dynamic override-resource-path "local-config.edn")
(def ^:dynamic file-external-path "./config.edn")

;;;

(defn custom-config
  "Loads file from external configuration. If not found uses empty map"
  []
  (try
    (let [config (cs/from-file file-external-path)]
      (log/infof "`%s` configuration file loaded successfully!" file-external-path)
      config)
    (catch Throwable _
      (log/infof "`%s` file was not found, use default configuration..." file-external-path)
      {})))

;;;

(defn override-config-map []
  (if override-resource-path
    (try
      (cs/ignore-missing-default cs/from-resource override-resource-path)
      (catch MissingResourceException _
        (log/infof "Not found override resource file: '%s'" override-resource-path)
        {}))
    {}))

;;;

(defn check [schema data]
  (-> data
      (sc/select-schema schema)
      (co/coerce schema {s/Str str})))

;;;


(defn load!
  "Loads app config.
   Config Merge Policy:
    1) default-config.edn (RESOURCE)
    2) local-config.edn (EXTRA RESOURCE)
    3) config.edn (EXTERNAL FILE)
    4) ENV VARIABLES (EXTERNAL ENV)
   The latest wins."
  [shutdown-fn config-holder hide-passwords-key-names config-schema]
  (log/info "Loading configuration...")
  (try
    (->> (apply cp/load-config
                :resource resource-default-path
                [:merge [(override-config-map)
                         (custom-config)
                         (cs/from-env)]])
         (check config-schema)
         (reset! config-holder))
    (->> @config-holder
         (uf/flip sec/hide-passwords hide-passwords-key-names :key-names)
         (clojure.pprint/pprint)
         (with-out-str)
         (log/infof "Loaded configuration data: %s\n"))
    @config-holder
    (catch Exception e
      (signal/die shutdown-fn e "FATAL ERROR: configuration is not valid"))))

(defn stop! [config-holder]
  (log/info "Resetting config atom...")
  (reset! config-holder {}))