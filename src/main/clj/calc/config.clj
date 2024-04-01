(ns calc.config
  (:require [base.configuration.common :as common]
            [base.modules.configuration :as config]
            [base.utils.collections :as collections]
            [mount.core :as mount]
            [schema.core :as s]))

(def cors {:cors {:allowed-origins s/Str
                  :allowed-headers s/Str}})

(s/defschema Configuration
  (collections/deep-merge common/rpc
                          common/dev-mode
                          common/postgres
                          cors))

(defonce holder (atom {}))

(def hide-passwords-key-names #{:pass :password :secret})

(declare ^:dynamic *loader*)

(mount/defstate ^:dynamic *loader*
  :start (config/load! mount/stop holder hide-passwords-key-names Configuration)
  :stop (config/stop! holder))
