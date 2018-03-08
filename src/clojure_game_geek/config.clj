(ns clojure-game-geek.config
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [com.stuartsierra.component :as component]))

(defrecord ClojureGameGeekConfig [config]

  component/Lifecycle

  (start [this]
         (assoc this :config (-> (io/resource "cgg-config.edn")
                               slurp
                               edn/read-string)))
  (stop [this]
        (assoc this :config nil)))

(defn new-config
  []
  {:config (map->ClojureGameGeekConfig {})})
