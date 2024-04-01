(ns base.utils.collections)

(defn deep-merge
  "Merge several maps into one with superset of fields. Latest wins"
  [a & maps]
  (if (map? a)
    (apply merge-with deep-merge a maps)
    (apply merge-with deep-merge maps)))