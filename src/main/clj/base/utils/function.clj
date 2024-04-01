(ns base.utils.function)

(defn flip
  "Flips the argument order that is passed to a function
   (flip str 1 2 3) => \"321\""
  [f & xs]
  {:pre [(fn? f)]}
  (apply f (reverse xs)))
