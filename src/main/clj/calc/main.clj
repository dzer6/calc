(ns calc.main
  (:require [mount.core :as mount]
            [base.modules.signals :as signals]
            [calc.migrations]
            [calc.server])
  (:gen-class)
  (:import (org.slf4j.bridge SLF4JBridgeHandler)))

(SLF4JBridgeHandler/removeHandlersForRootLogger)
(SLF4JBridgeHandler/install)

(defn -main [& _]
  (signals/add-shutdown-hook (fn []
                               (mount/stop)
                               (shutdown-agents)))
  (mount/start))
