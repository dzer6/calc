(ns base.data.json
  (:require [base.utils.time :as time]
            [jsonista.core :as json])
  (:import (com.fasterxml.jackson.core JsonGenerator)
           (com.fasterxml.jackson.databind SerializationFeature)
           (java.util Date)
           (org.joda.time DateTime)))

(def encoders
  {Date     (fn [^Date dt ^JsonGenerator gen]
              (.writeString gen ^String (time/to-utc-string-or-nil (DateTime. dt))))
   DateTime (fn [^DateTime dt ^JsonGenerator gen]
              (.writeString gen ^String (time/to-utc-string-or-nil dt)))})

(def mapper
  (-> {:decode-key-fn keyword :encoders encoders}
      (json/object-mapper)
      (.disable SerializationFeature/FAIL_ON_EMPTY_BEANS)))

;;; Parsing

(defn parse [object]
  (json/read-value object mapper))

(defn ^String write [object]
  (json/write-value-as-string object mapper))