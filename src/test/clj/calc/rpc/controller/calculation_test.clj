(ns calc.rpc.controller.calculation-test
  (:require [calc.config :as app-config]
            [calc.fixtures :as fixtures]
            [calc.test-utils :as tu]
            [clojure.test :refer :all]))

(->> [fixtures/with-test-config
      (fixtures/with-postgres app-config/holder)
      fixtures/with-postgres-migrations
      (fixtures/with-postgres-seeding "src/test/resources/data/history.sql")
      (fixtures/with-web-server app-config/holder)]
     (join-fixtures)
     (use-fixtures :once))

(defn evaluate-expression [payload]
  (tu/make-http-post-call app-config/holder
                          "calc"
                          payload))

(deftest math-expression-evaluation
  (testing "correctness of a valid expression evaluation"
    (let [request-payload {:expression "-1 * (2 * 6 / 3)"}
          http-response   (evaluate-expression request-payload)
          response-body   (tu/parse-response-body http-response)]
      (is (= 200 (:status http-response)))
      (is (= "-4.0" (:result response-body)))))

  (testing "correctness of an invalid expression evaluation"
    (let [request-payload {:expression "abc"}
          http-response   (evaluate-expression request-payload)
          response-body   (tu/parse-response-body http-response)]
      (is (= 400 (:status http-response)))

      (is (= {:description "Wrong input. Unable to evaluate expression."
              :error       "resource/bad-request"}
             response-body)))))

;;;

(defn obtain-history [query-params]
  (tu/make-http-get-call app-config/holder
                         "history"
                         query-params))

(deftest expression-evaluation-history-obtaining
  (testing "correctness of expressions evaluation history request processing with valid limit, offset : 11, 0"
    (let [query-params  {:limit  11
                         :offset 0}
          http-response (obtain-history query-params)
          response-body (tu/parse-response-body http-response)]
      (is (= 200 (:status http-response)))
      (is (= 10 (count response-body)))
      (is (= "1 + 1" (-> response-body first :input)))))

  (testing "correctness of expressions evaluation history request processing with valid limit, offset : 10, 0"
    (let [query-params  {:limit  10
                         :offset 0}
          http-response (obtain-history query-params)
          response-body (tu/parse-response-body http-response)]
      (is (= 200 (:status http-response)))
      (is (= 10 (count response-body)))
      (is (= "1 + 1" (-> response-body first :input)))))

  (testing "correctness of expressions evaluation history request processing with valid limit, offset : 9, 0"
    (let [query-params  {:limit  9
                         :offset 0}
          http-response (obtain-history query-params)
          response-body (tu/parse-response-body http-response)]
      (is (= 200 (:status http-response)))
      (is (= 9 (count response-body)))
      (is (= "1 + 1" (-> response-body first :input)))))

  (testing "correctness of expressions evaluation history request processing with valid limit, offset : 9, 1"
    (let [query-params  {:limit  9
                         :offset 1}
          http-response (obtain-history query-params)
          response-body (tu/parse-response-body http-response)]
      (is (= 200 (:status http-response)))
      (is (= 9 (count response-body)))
      (is (= "2 * 2" (-> response-body first :input)))))

  (testing "correctness of expressions evaluation history request processing with valid limit, offset : 10, 100"
    (let [query-params  {:limit  10
                         :offset 100}
          http-response (obtain-history query-params)
          response-body (tu/parse-response-body http-response)]
      (is (= 200 (:status http-response)))
      (is (= 0 (count response-body)))))

  (testing "correctness of processing of invalid limit/offset"
    (let [query-params  {:limit  -1
                         :offset -1}
          http-response (obtain-history query-params)]
      (is (= 400 (:status http-response))))))
