(ns clojure-game-geek.system
  (:require 
    [com.stuartsierra.component :as component]
    [clojure-game-geek.schema :as schema]
    [clojure-game-geek.server :as server]
    [clojure-game-geek.config :as config]
    [clojure-game-geek.db :as db]))

(defn new-system
  []
  (merge (component/system-map)
         (server/new-server)
         (schema/new-schema-provider)
         (config/new-config)
         (db/new-db)))
