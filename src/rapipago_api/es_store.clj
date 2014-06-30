(ns rapipago_api.es_store
  (:require [clojurewerkz.elastisch.rest :as es]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [rapipago-scraper.core :as rapipago]))

(def index-name "rapipago")

(defn connect [port]
  (es/connect (str "http://127.0.0.1:" port)))

(defn create-index [conn index]
  (when-not (esi/exists? conn index)
    (esi/create conn
                index
                :mappings {:rapipago {:properties {:id {:type "string"
                                                         :store "yes"
                                                         :index "not_analyzed"}}}})))

(def es-conn (connect 9200))
(create-index es-conn index-name)

(defn save-rapipago [conn rapipago]
  (esd/put conn index-name :rapipago (:id rapipago) rapipago))

(comment
  (def store 
    (->> {:province {:id "C"} :city {:id "PALERMO"}}
         rapipago/search
         first))

  (save-rapipago es-conn store)

  (->> {:province {:id "C"} :city {:id "PALERMO"}}
       rapipago/search
       (map #(save-rapipago es-conn %)))
  
)
