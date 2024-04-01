(ns base.configuration.common
  (:require [schema.core :as s]))

(s/defschema TurnOn
  {:turn-on s/Bool})

(def dev-mode
  {:dev-mode s/Bool})

(def rpc
  {:web {:server {:host s/Str
                  :port s/Int}}})

(def postgres
  {:feature  {:database {:postgres                TurnOn
                         :print-queries           TurnOn
                         :run-migrations-on-start TurnOn}}
   :postgres {:server-name              s/Str
              :port-number              s/Int
              :database-name            s/Str
              :username                 s/Str
              :password                 s/Str
              :leak-detection-threshold s/Int}})
