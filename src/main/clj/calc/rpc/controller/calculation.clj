(ns calc.rpc.controller.calculation
  (:require [base.api.error :as error]
            [base.modules.database-consistency :as database-consistency]
            [calc.db.postgres :as postgres]
            [calc.db.query :as query]
            [clojure.tools.logging :as log]
            [next.jdbc :as jdbc]
            [schema.core :as s]
            [slingshot.slingshot :refer [try+]])
  (:import (org.joda.time DateTime)
           (org.mvel2 MVEL)))

(s/defschema EvaluateExpressionRequest
  {:expression s/Str})

(s/defschema EvaluateExpressionResponse
  {:result s/Str})

(defn evaluate [body]
  (log/infof "evaluate, request body: %s " body)
  (try+
    (let [^String expression (:expression body)
          result             (-> expression MVEL/eval str)]
      (database-consistency/wrap-exceptions
        (jdbc/with-transaction [tx @postgres/db]
          (query/insert-expression tx {:input  expression
                                       :output result})))
      {:result result})
    (catch Exception e
      (log/error e)
      (error/fire :error-type :resource/bad-request
                  :cause e
                  :message "Wrong input. Unable to evaluate expression."))))

;;;

(s/defschema FindPastEvaluationsResponse
  [{:id         s/Uuid
    :input      s/Str
    :output     s/Str
    :created-at DateTime}])

(defn obtain-past-evaluations [offset limit]
  (log/infof "obtain-past-evaluations, offset: %d, limit: %d" offset limit)
  (try+
    (when (or (neg? offset) (neg? limit))
      (error/fire :error-type :resource/bad-request
                  :message "Wrong input. Unable to obtain past evaluations."))
    (database-consistency/wrap-exceptions
      (jdbc/with-transaction [tx @postgres/db]
        (query/find-expressions tx {:offset offset
                                    :limit  limit})))
    (catch Exception e
      (log/error e)
      (error/fire :error-type :resource/bad-request
                  :cause e
                  :message "Unable to obtain past evaluations."))))

