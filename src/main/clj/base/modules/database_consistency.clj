(ns base.modules.database-consistency
  (:require [clojure.tools.logging :as log]
            [slingshot.slingshot :refer [try+]]
            [base.api.error :as error])
  (:import java.sql.SQLException
           (clojure.lang Keyword)))

(defn keyword-sql-state [^SQLException t]
  (keyword (.getSQLState t)))

(defn extract-data [^SQLException t]
  (let [t (if (-> t .getNextException nil?)
            t
            (.getNextException t))
        sql-state (keyword-sql-state t)
        message (.getMessage t)]
    (log/debugf "extract SQL error data, e: %s, sql-state: %s, message: %s" t sql-state message)
    (error/fire :error-type :database/operation-fail
                :throwable t
                :sql-state sql-state
                :message message)))

(defn check-message [^String message substrings]
  (if (string? substrings)
    (.contains message substrings)
    (reduce #(or %1 %2) (map #(.contains message %) substrings))))

(defn issue [^Keyword catching-state ^String catching-message-part]
  (partial
    (fn [catching-state catching-message-part {:keys [error-type sql-state message]}]
      (and
        (= error-type :database/operation-fail)
        (= sql-state catching-state)
        (check-message message catching-message-part)))
    catching-state catching-message-part))

(defn cause [^Class exception-cause-class]
  (fn [t]
    (some->> t
             (ex-cause)
             (instance? exception-cause-class))))

(defmacro wrap-exceptions
  "Usage:
  (try+
    (database-consistency/wrap-exceptions
      (db/create-project! request))
    (catch (database-consistency/issue :22003 \"integer out of range\") _
      (error/fire :error-type :resource/bad-request
                  :request request
                  :message \"Integer out of range\")))"
  [& body]
  `(try+
     ~@body
     (catch SQLException e#
       (extract-data e#))
     (catch (cause SQLException) e#
       (-> e# ex-cause extract-data))))
