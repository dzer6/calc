(ns user
  (:require [clojure.repl :refer :all]
            [repl]))

(alter-var-root #'*warn-on-reflection* (constantly true))

(apply clojure.tools.namespace.repl/set-refresh-dirs ["src" "test"])

(def init repl/init)
