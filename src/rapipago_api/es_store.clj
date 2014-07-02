(ns rapipago_api.es_store
  (:require [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.response :as esrsp]
            [clojurewerkz.elastisch.query :as q]
            [clojure.core.cache :as cache]
            [com.climate.claypoole :as cp]
            [geocoder.google :as google]
            [rapipago-scraper.core :as rapipago]
            [rapipago-scraper.provinces :as provinces]
            [rapipago-scraper.cities :as cities]))

(def index-name "rapipago")

(defn connect [port]
  (es/connect (str "http://127.0.0.1:" port)))

(defn create-index [conn index]
  (when-not (esi/exists? conn index)
    (esi/create conn
                index
                :mappings {"rapipago" {:properties {:id {:type "string"
                                                         :store "yes"
                                                         :index "not_analyzed"}
                                                    :city-id {:type "string"
                                                              :store "yes"
                                                              :index "not_analyzed"}
                                                    :province-id {:type "string"
                                                                  :store "yes"
                                                                  :index "not_analyzed"}
                                                    :address {:type "string"
                                                              :store "yes"
                                                              :index "not_analyzed"}
                                                    :name {:type "string"
                                                           :store "yes"
                                                           :index "not_analyzed"}}}})))

(def es-conn (connect 9200))
(create-index es-conn index-name)

(def db (atom (cache/basic-cache-factory {})))

(defn provinces [db]
  (let [key :provinces
        c @db
        newdb (if (cache/has? c key)
                (cache/hit c key)
               (cache/miss c key (->> (provinces/find-all)
                                      (map (juxt :id :name))
                                      (into {}))))]
    (reset! db newdb)
    (cache/lookup newdb key)))

(defn full-address [{address :address {province-id :id} :province {city-id :id} :city}]
  (let [province-name (get (provinces db) province-id)]
    (apply str (interpose ", " [address city-id province-name "Argentina"]))))

(comment
  (get (provinces db) "A")
  )

(defn geolocate [rapipago]
  (assoc rapipago :location
         (-> (full-address rapipago)
             google/geocode-address
             first
             :geometry
             :location)))

(comment
  (def store (first (rapipago/search {:province {:id "C"}
                                      :city {:id "PALERMO" :name "PALERMO"}})))
  (full-address store)
  (geolocate store))

(defn save-rapipago [conn rapipago]
  (let [province-id (get-in rapipago [:province :id])
        city-id (get-in rapipago [:city :id])
        doc (-> rapipago
                (assoc :province-id province-id :city-id city-id)
                (dissoc :province :city))]
    (esd/put conn index-name "rapipago" (:id rapipago) doc)))

(defn refresh-city-index [province-id city-id]
  (let [conn (connect 9200)]
  (->> {:province {:id province-id} :city {:id city-id}}
       rapipago/search
       (map geolocate)
       (map #(save-rapipago conn %)))))

(comment

  (refresh-city-index "C" "PALERMO")

)

(defn search [{:keys [province-id city-id]}]
  (let [query (q/bool
                :must [(q/term :city-id city-id)
                       (q/term :province-id province-id)])
        res (esd/search es-conn index-name "rapipago"
                        :query query
                        :size 100)
        hits (esrsp/hits-from res)]
    (map :_source hits)))

(comment

  (->> (esd/search es-conn index-name "rapipago"
                   :query (q/bool :must
                                  [(q/term :city-id "BALVANERA")
                                   (q/term :province-id "D")]))
      esrsp/hits-from
      (map :_source))

  (count (search {:province-id "B" :city-id "LANUS"}))
  (count (search {:province-id "C" :city-id "BALVANERA"}))

  )
