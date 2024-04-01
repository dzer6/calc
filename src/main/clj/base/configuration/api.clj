(ns base.configuration.api
  (:refer-clojure :exclude [get])
  (:require [base.utils.json-utils :as ju]
            [base.utils.string :as core.string]
            [clj-fuzzy.metrics :as fuzzy]
            [clojure.core.memoize :as memo]
            [slingshot.slingshot :as error]))

;;;

(defn feature-on? [config & path]
  {:pre [(map? config)]}
  (let [turn-on? (get-in config (flatten [:feature path :turn-on]))]
    (when (nil? turn-on?)
      (error/throw+ {:message (format "Unknown feature [%s]" path)}))
    turn-on?))

;;;

(defn get-suggestion
  "Get level vise suggestion based on levenshtein distance
  function will return nil if no suggestion found or if difference more than 2"
  [paths target]
  (let [suggestion (->> paths
                        (mapv (juxt identity (partial fuzzy/levenshtein target)))
                        (sort-by second)
                        (filter (comp (partial >= 2) second))
                        (first))]
    (if-let [[ks distance] suggestion]
      {:key      (core.string/string->keywords ks)
       :distance distance})))

(def memo--get-suggestion
  (memo/memo get-suggestion))

;;;

(defn fire-error-with-suggestion [config ks]
  (let [paths (ju/get-json-flattened-paths config " " "*" (count ks))
        ks-string (core.string/keywords->strings ks)
        suggestion (memo--get-suggestion paths ks-string)
        message (if suggestion
                  (format "Unknown key %s in config map. Maybe you mean: %s" ks (:key suggestion))
                  (format "Unknown key %s in config map." ks))]
    (error/throw+ {:message    message
                   :suggestion suggestion})))

(defn get-in-config [config ks]
  (let [value (get-in config ks ::not-found)]
    (if (= value ::not-found)
      (fire-error-with-suggestion config ks)
      value)))

(defn get [config & ks]
  {:pre [(map? config)]}
  (get-in-config config ks))
