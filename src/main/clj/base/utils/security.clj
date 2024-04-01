(ns base.utils.security
  (:require [clojure.tools.logging :as log]
            [clojure.walk :as walk]))

(defn hide-passwords
  "Hide passwords and other sensitive information from a map.
  By default hides the keys with names `:pass` and `:password`,
  Optionally, you can override which names to hide or extra-path to hide and redefine hide-fn
  Safe, if error occurs log it and return original structure"
  [data & {:keys [key-names extra-paths hide-fn]
           :or   {key-names   #{:pass :password}
                  extra-paths []
                  hide-fn     (fn [_] "*******")}}]
  (try
    (let [key-names  (set key-names)
          transform  (fn [form]
                       (if (map-entry? form)
                         (let [[k v] form]
                           (if (and (contains? key-names k) (string? v))
                             [k (hide-fn v)]
                             {k v}))
                         form))
          hidden-map (walk/postwalk transform data)]
      (reduce
        (fn [acc path]
          (update-in acc path hide-fn))
        hidden-map
        extra-paths))
    (catch Exception e
      (log/error e)
      data)))