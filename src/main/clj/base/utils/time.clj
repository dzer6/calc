(ns base.utils.time
  (:require [clj-time.format :as f])
  (:import (org.joda.time DateTime)))

(defn ^String to-utc-string-or-nil [^DateTime date-time]
  (some->> date-time
           (f/unparse (f/formatter :date-time))))