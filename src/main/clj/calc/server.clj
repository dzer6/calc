(ns calc.server
  (:require [base.api.error :as error]
            [base.api.middleware.cross-origin :as cross-origin]
            [base.configuration.api :as config]
            [base.modules.server :as core.server]
            [base.modules.signals :as signals]
            [calc.config :as app-config]
            [calc.rpc.api :as api]
            [mount.core :as mount]
            [muuntaja.middleware :as mm]
            [ring.middleware.defaults :as rmd]))

(defn http-handler [config-holder]
  (-> (api/routes)
      (rmd/wrap-defaults rmd/api-defaults)
      (error/wrap)
      (mm/wrap-format)
      (mm/wrap-exception)
      (cross-origin/wrap (config/get config-holder :cors :allowed-origins)
                         (config/get config-holder :cors :allowed-headers))))

;;;

(defonce holder (atom nil))

(declare ^:dynamic *runner*)

(mount/defstate ^:dynamic *runner*
  :start (signals/run-or-die mount/stop
                             (core.server/start-server! holder
                                                        (http-handler @app-config/holder)
                                                        @app-config/holder))
  :stop (core.server/stop-server! holder))
