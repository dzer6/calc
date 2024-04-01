(ns base.modules.signals
  (:require [clojure.tools.logging :as log]))

(defn add-shutdown-hook [f]
  (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable f)))

(defn graceful-shutdown [shutdown-fn]
  (shutdown-fn)
  (System/exit 0))

(defn die
  "Logs the exception and the message, stop all modules and kills JVM"
  [shutdown-fn ^Exception e message]
  (log/error e message)
  (graceful-shutdown shutdown-fn))

(defmacro run-or-die [shutdown-fn & body]
  `(try
     ~@body
     (catch Exception e#
       (die ~shutdown-fn e# (str "Can not run, because of: " (or (.getMessage e#) "<unknown>"))))))