(ns clojure-game-geek.schema
  "Contains custom resolvers and a function to provide the full schema"
  (:require
    [clojure.java.io :as io]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.schema :as schema]
    [com.walmartlabs.lacinia.resolve :refer [resolve-as]]
    [com.stuartsierra.component :as component]
    [clojure-game-geek.db :as db]
    [clojure.edn :as edn]))

(defn game-by-id
  [db]
  (fn [_ args _]
    (db/find-game-by-id db (:id args))))

(defn member-by-id
  [db]
  (fn [_ args _]
    (db/find-member-by-id db (:id args))))

(defn rate-game
  [db]
  (fn [_ args _]
    (let [{game-id :game_id
           member-id :member_id
           rating :rating} args
          game (db/find-game-by-id db game-id)
          member (db/find-member-by-id db member-id)]
      (cond 
        (nil? game)
        (resolve-as nil {:message "Game not found"
                         :status 404})
        
        (nil? member)
        (resolve-as nil {:message "Member not found"
                         :status 404})
        
        (not (<= 1 rating 5))
        (resolve-as nil {:message "Rating must be between 1 and 5"
                         :status 400})
        
        :else
        (do
          (db/upsert-game-rating db game-id member-id rating)
          game)))))

(defn board-game-designers
  [db]
  (fn [_ _ board-game]
    (db/list-designers-for-game db (:id board-game))))

(defn designer-games
  [db]
  (fn [_ _ designer]
    (db/list-games-for-designer db (:id designer))))

(defn rating-summary
  [db]
  (fn [_ _ board-game]
    (let [id (:id board-game)
          ratings (map :rating (db/list-ratings-for-game db (:id board-game)))
          n (count ratings)]
      {:count n
       :average (if (zero? n)
                  0
                  (/ (apply + ratings)
                     (float n)))})))

(defn member-ratings
  [db]
  (fn [_ _ member]
    (db/list-ratings-for-member db (:id member))))

(defn game-rating->game
  [db]
  (fn [_ _ game-rating]
    (db/find-game-by-id db (:game_id game-rating))))

(defn deployments
  [config]
  (fn [_ _ _]
    [{:id 123
      :force_package_download false
      :force_package_redeployment true
      :created "01-01-2018:12:00:45"
      :release_id 456
      :comments "lets see if this works"}]))

(defn deployment-release
  [config]
  (fn [_ _ dep]
    {:id (:release_id dep)
     :version "1.2.3456"
     :release_notes "this is a smashing release"}))


(defn resolver-map
  [component]
  (let [db (:db component)
        config (:config component)]
    (prn config)
    {:query/game-by-id (game-by-id db)
     :query/member-by-id (member-by-id db)
     :query/deployments (deployments config)
     :mutation/rate-game (rate-game db)
     :Deployment/release (deployment-release config)
     :BoardGame/designers (board-game-designers db)
     :BoardGame/rating-summary (rating-summary db)
     :GameRating/game (game-rating->game db)
     :Designer/games (designer-games db)
     :Member/ratings (member-ratings db)}))

(defn load-schema
  [component]
  (-> (io/resource "cgg-schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map component))
      schema/compile))

(defrecord SchemaProvider [schema]
  component/Lifecycle
  (start [this]
    (assoc this :schema (load-schema this)))
  (stop [this]
    (assoc this :schema nil)))

(defn new-schema-provider
  []
  {:schema-provider (-> {}
                        map->SchemaProvider
                        (component/using [:config])
                        (component/using [:db]))})
