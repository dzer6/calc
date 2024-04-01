(ns calc.rpc.controller.health
  (:require [calc.db.postgres :as postgres]
            [ring.util.http-response :as http]
            [schema.core :as s]
            [taoensso.encore :as enc]))

(def statuses (s/enum "pass" "fail" "warn"))

;; https://inadarei.github.io/rfc-healthcheck/
(s/defschema HealthCheckResponse
  {:status statuses})

(def is-postgres-okay
  (enc/memoize 1000                                         ; do not check more often than once per second
               postgres/is-connection-alive))

(defn readiness-handler []
  (if (is-postgres-okay)
    (http/ok {:status "pass"})
    (http/service-unavailable {:status "fail"})))

(defn liveness-handler []
  (if (is-postgres-okay)
    (http/ok {:status "pass"})
    (http/service-unavailable {:status "fail"})))
