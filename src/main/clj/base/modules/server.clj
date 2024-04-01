(ns base.modules.server
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty9 :as ring-adapter]
            [base.configuration.api :as config])
  (:import (org.eclipse.jetty.server Server)))

;;;

(defn get-port [^Server server]
  (.. server (getURI) (getPort)))

;;;

(defn start-server! [http-server-atom handler app-config]
  (log/info "Starting HTTP server")
  (let [{:keys [host port]} (config/get app-config :web :server)
        server (ring-adapter/run-jetty handler {:ip    host
                                                :port  port
                                                :join? false})]
    (log/infof "HTTP server started on %s:%s" host (get-port server))
    (reset! http-server-atom server)))

;;;

(defn stop-server! [http-server-atom]
  (when @http-server-atom
    (log/info "Stopping HTTP server...")
    (.stop ^Server @http-server-atom)
    (reset! http-server-atom nil)))