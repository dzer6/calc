(ns base.api.middleware.cross-origin
  (:require [ring.util.http-response :refer :all]))

(defn wrap
  ([handler]
   (wrap handler
         "*"
         "X-Requested-With,Content-Type,Cache-Control,Origin,Accept,Authorization"))
  ([handler allowed-origins]
   (wrap handler
         allowed-origins
         "X-Requested-With,Content-Type,Cache-Control,Origin,Accept,Authorization"))
  ([handler allowed-origins allowed-headers]
   (fn [request]
     ;; TODO do we need options for request method?
     (if (= (request :request-method) :options)
       (-> (ok)
           (assoc-in [:headers "Access-Control-Allow-Origin"] allowed-origins)
           (assoc-in [:headers "Access-Control-Allow-Methods"] "GET,PUT,POST,DELETE,OPTIONS")
           (assoc-in [:headers "Access-Control-Allow-Headers"] allowed-headers)
           (assoc :status 200))
       (-> request
           (handler)
           (assoc-in [:headers "Access-Control-Allow-Origin"] allowed-origins))))))