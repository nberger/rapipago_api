(ns rapipago_api.es-store
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

(def es-url (or (System/getenv "BONSAI_URL") (str "http://127.0.0.1:" 9200)))

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
                                                    :location {:type "geo_point"}
                                                    :name {:type "string"
                                                           :store "yes"
                                                           :index "not_analyzed"}}}})))

(defn connect []
  (let [connection (es/connect es-url)]
    (create-index connection index-name)
    connection))

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
             :location
             (clojure.set/rename-keys {:lng :lon}))))

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

(defn build-city-index [province-id city-id]
  (->> {:province {:id province-id} :city {:id city-id}}
       rapipago/search
       (map geolocate)))

(defn refresh-city-index [province-id city-id]
  (let [conn (connect)
        rapipagos (build-city-index province-id city-id)]
    (doseq [rapipago rapipagos]
      (save-rapipago conn rapipago))
    rapipagos))

(defn refresh-province [province-id threads-count]
  (let [out *out*
        cities (cities/find-in-province {:id province-id})]
    (cp/pdoseq
      threads-count
      [city cities]
      (binding [*out* out]
        (let [stores-in-city (refresh-city-index province-id (:id city))]
          (println (:name city) ": " (count stores-in-city)))))))


(comment

  (build-city-index "C" "PALERMO")
  (refresh-city-index "C" "PALERMO")
  (refresh-province "C" 8)
  (doseq [province (keys (provinces db))] (refresh-province province 10))
)

(defn search [{:keys [province-id city-id]}]
  (let [query (q/bool
                :must [(q/term :city-id city-id)
                       (q/term :province-id province-id)])
        res (esd/search (connect) index-name "rapipago"
                        :query query
                        :size 1000)
        hits (esrsp/hits-from res)]
    (map :_source hits)))

(defn distance-search [center distance]
  (let [query {:filtered {:query (q/match-all)
                          :filter {:geo_distance {:distance distance
                                                  :location center}}}}
        res (esd/search (connect) index-name "rapipago"
                        :query query
                        :size 1000)
        hits (esrsp/hits-from res)]
    (map :_source hits)))

(defn trbl->tlbr [top-right bottom-left]
  [{:lat (:lat top-right) :lon (:lon bottom-left)}
    {:lat (:lat bottom-left) :lon (:lon top-right)}])

(defn bounding-box-search [top-right bottom-left]
  (let [[top-left bottom-right] (trbl->tlbr top-right bottom-left)
        query {:filtered {:query (q/match-all)
                          :filter {:geo_bounding_box
                                   {:location {:top_left top-left
                                               :bottom_right bottom-right}}}}}
        res (esd/search (connect) index-name "rapipago"
                        :query query
                        :size 1000)
        hits (esrsp/hits-from res)]
    (map :_source hits)))

(comment
  (->> (esd/search (connect) index-name "rapipago"
                   :query (q/bool :must
                                  [(q/term :city-id "BALVANERA")
                                   (q/term :province-id "D")]))
      esrsp/hits-from
      (map :_source))

  (count (search {:province-id "B" :city-id "LANUS"}))
  (count (search {:province-id "C" :city-id "PALERMO"}))

  (->> (search {:province-id "C" :city-id "PALERMO"})
       first
       geolocate
       )
  (distance-search {:lat -34.603272
                    :lon -58.396726}
                   "500m")


  (bounding-box-search {:lat -34.58023769631528
                        :lon -58.38676964013672}
                       {:lat -34.62262718004927
                        :lon -58.45543419091797})
  )
