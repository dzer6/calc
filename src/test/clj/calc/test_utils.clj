(ns calc.test-utils
  (:require [base.configuration.api :as config]
            [base.data.json :as json]
            [clj-http.client :as http-client]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log])
  (:import (java.net ServerSocket)))

(defn get-free-port []
  (try
    (with-open [server-socket (ServerSocket. 0)]
      (.getLocalPort server-socket))
    (catch Exception e
      (log/error e "Unable to get free port")
      (throw e))))

(def default-header {"Content-Type" "application/json"})

(defn resolve-api-url [app-config path]
  (let [port (config/get @app-config :web :server :port)
        url  (format "http://localhost:%s/%s" port path)]
    url))

(defn make-http-post-call [app-config uri body & [request-header]]
  (http-client/post (resolve-api-url app-config uri)
                    {:headers          (merge default-header request-header)
                     :body             (json/write body)
                     :throw-exceptions false}))

(defn make-http-get-call [app-config uri query-params & [request-header]]
  (http-client/get (resolve-api-url app-config uri)
                   {:headers          (merge default-header request-header)
                    :query-params     query-params
                    :throw-exceptions false}))

(defn parse-response-body [rpc-http-response]
  (some-> rpc-http-response :body json/parse))