(ns calc.rpc.api
  (:require [base.api.error :as error]
            [calc.rpc.controller.calculation :as calculation]
            [calc.rpc.controller.health :as health]
            [compojure.api.sweet :as sw]
            [compojure.route :as route]
            [ring.util.http-response :as ring.http]
            [schema.core :as s]))

(s/defschema ErrorResponse
  {:details     s/Any
   :error       s/Str
   :description s/Str})

(defn routes []
  (sw/routes
    (sw/api
      {:exceptions
       {:handlers
        {:compojure.api.exception/request-validation  error/request-validation-handler
         :compojure.api.exception/response-validation error/response-validation-handler
         :compojure.api.exception/default             error/default-handler}}
       :swagger
       {:ui   "/api-docs"
        :spec "/swagger.json"
        :data {:info     {:version     "1.0.0"
                          :title       "Calc API"
                          :description "REST API for expressions evaluation service"}
               :consumes ["application/json"]
               :produces ["application/json"]}}}

      (sw/POST "/calc" []
        :body [body calculation/EvaluateExpressionRequest]
        :return calculation/EvaluateExpressionResponse
        :summary "Evaluates math expressions"
        :responses {400 {:schema ErrorResponse :description "Wrong input data"}
                    500 {:schema ErrorResponse :description "Internal issue"}}
        (ring.http/ok (calculation/evaluate body)))

      (sw/GET "/history" []
        :query-params [offset :- Long
                       limit :- Long]
        :return calculation/FindPastEvaluationsResponse
        :summary "Returns previous calculations"
        :responses {400 {:schema ErrorResponse :description "Wrong input data"}
                    500 {:schema ErrorResponse :description "Internal issue"}}
        (ring.http/ok (calculation/obtain-past-evaluations offset limit)))

      (sw/GET "/health/live" []
        :summary "Check liveness probe"
        :return health/HealthCheckResponse
        (health/liveness-handler))

      (sw/GET "/health/ready" []
        :summary "Check readiness probe"
        :return health/HealthCheckResponse
        (health/readiness-handler))

      (sw/GET "/" []
        :summary "Redirect to Swagger UI"
        (ring.http/moved-permanently "/api-docs"))

      (sw/undocumented
        (route/not-found "Not Found")))))
