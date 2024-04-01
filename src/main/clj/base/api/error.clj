(ns base.api.error
  (:require [base.utils.function :as uf]
            [clojure.tools.logging :as log]
            [compojure.api.exception :as ex]
            [ring.util.http-response :as http-response]
            [slingshot.slingshot :refer [throw+]])
  (:import (java.net UnknownHostException)))

;;;

(defn detect-error-type [e]
  (or (:error-type e) (class e)))

(defmulti handle detect-error-type)

;;;

(defn fire [& {:keys [error-type cause message] :as err-map}]
  {:pre [(some? error-type)
         (some? message)]}
  (throw+ err-map cause message))

;;;

(defn process-3dparty [err-data]
  (log/error "3dparty-service details : " err-data)
  (if (-> err-data :response :status (= 500))
    {:error       :resource/server-error
     :description "Internal server error"}
    (assoc err-data :details (-> err-data :response :body))))

(defn error-resp-body [{:keys [message response error-type] :as err-data}]
  (let [details-from-3dparty-service (:body response)]
    (-> err-data
        (merge {:error       error-type
                :description message})
        (cond-> (some? details-from-3dparty-service)
                (process-3dparty))
        (dissoc :error-type :response :request :message :cause)))) ; TODO do we want to filter out `cause` (exception) as a part of response?

;;;

(defn request-validation-handler [^Throwable t data req]
  (log/warnf "REST API request validation failed, data=[%s], req=[%s], exception=[%s]" data req t)
  (->> (ex/request-validation-handler t data req)
       :body
       (uf/flip select-keys [:schema :errors :value])
       (assoc {:error-type :resource/bad-request
               :message    "Malformed request data"} :details)
       (handle)))

;;;

(defn response-validation-handler [^Throwable t data req]
  (log/warnf "REST API response validation failed, data=[%s], req=[%s], exception=[%s]" data req t)
  (->> (ex/response-validation-handler t data req)
       :body
       (uf/flip select-keys [:schema :errors :value])
       (assoc {:error-type :resource/server-error
               :message    "Invalid response data"} :details)
       (handle)))

;;;

(defn default-handler [^Throwable t data request]
  (log/errorf "Default error handler, data=[%s], request=[%s], exception=[%s]" data request t)
  (if-let [ex-details (ex-data t)]
    (handle ex-details)
    (handle t)))

;;;

(defmethod handle UnknownHostException [^Throwable t]
  (http-response/internal-server-error
    {:error       :resource/server-error
     :description (format "Unknown host exception: %s" (.getMessage t))}))

;;;

(defmethod handle :resource/not-found [error-data]
  (-> error-data
      (error-resp-body)
      (http-response/not-found)))

(defmethod handle :resource/bad-request [error-data]
  (-> error-data
      (error-resp-body)
      (http-response/bad-request)))

(defmethod handle :resource/unknown-error [error-data]
  (-> error-data
      (error-resp-body)
      (http-response/internal-server-error)))

(defmethod handle :resource/server-error [error-data]
  (-> error-data
      (error-resp-body)
      (http-response/internal-server-error)))

;;;

(defmethod handle :api/validation [error-data]
  (http-response/bad-request
    {:error       (:error-type error-data)
     :description (:message error-data)}))

;;; Default Handler

(defmethod handle :default [error-data]
  (log/error error-data)
  (log/warnf "Not handled error of type [%s]" (detect-error-type error-data))
  (http-response/internal-server-error
    {:error       :resource/server-error
     :description "Internal server error"}))

;;;

(defn wrap [handler]
  (fn [request]
    (try
      (handler request)
      (catch Throwable t
        (log/error t)
        (if-let [ex-details (ex-data t)]
          (handle ex-details)
          (handle t))))))