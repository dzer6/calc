(ns base.utils.string
  (:require [clojure.string :as s]))

(defn keywords->strings
  "Transform seq of keywords to string separated by space
  do not support namespace qualified keywords"
  [ks]
  (some->> ks (mapv name) (s/join " ")))

(defn string->keywords
  "Transform string separated by single space to seq of keywords"
  [string]
  (some->> (s/split string #"\s") (mapv keyword)))
