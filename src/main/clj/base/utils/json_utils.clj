(ns base.utils.json-utils
  (:require [clojure.string :as s]))

(defn extend-paths
  "Prepend given path to all paths"
  [path intermediate-paths]
  (mapv #(assoc % :path (conj (:path %) path)) intermediate-paths))

(defn json->flattened-json
  "Transform json to flattened json
  By traversing JSON structure and collecting path form bottom to up

  Example:  name     array
           /  |     /  |  \\
     first   last  1   2   3
       |      |
     'foo'  'bar'

  (json->flattened-json example '*')
  Will produce
  [{:path ['name' 'first'] :value 'foo'}
  {:path ['name' 'last']   :value 'bar'}
  {:path ['array' '*'] :value 1}
  {:path ['array' '*'] :value 2}
  {:path ['array' '*'] :value 3}]"
  ([json array-separator] (json->flattened-json json array-separator nil))
  ([json array-separator max-level] (json->flattened-json json array-separator max-level 1))
  ([json array-separator max-level level]
   (if (or (nil? max-level) (<= level max-level))
     (cond (map? json) (reduce-kv (fn [acc k v] (->> (json->flattened-json v array-separator max-level (inc level))
                                                     (extend-paths (name k))
                                                     (concat acc))) [] json)
           (vector? json) (->> (mapv #(json->flattened-json % array-separator max-level (inc level)) json)
                               (flatten)
                               (extend-paths array-separator))
           :else [{:path () :value json}])
     [{:path () :value json}])))

(defn escape [separator segment]
  (-> segment
      (s/replace (re-pattern (format "\\%s" separator)) (format "\\\\%s" separator))
      (s/replace (re-pattern (format "(\\\\)(?!\\%s)" separator)) "$1$1")))

(defn get-json-flattened-paths
  "Extract string paths from flattened JSON"
  ([json separator array-separator] (get-json-flattened-paths json separator array-separator nil))
  ([json separator array-separator max-level]
   (->> (json->flattened-json json array-separator max-level)
        (mapv (comp (partial mapv (partial escape separator)) :path))
        (mapv (partial s/join separator)))))