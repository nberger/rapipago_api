(ns rapipago_api.handler
  (:use compojure.core
        [ring.middleware.json :only [wrap-json-response wrap-json-body]]
        [ring.util.response :only [response]])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [clojure.core.cache :as cache]
            [rapipago-scraper.core :as rapipago]
            [rapipago-scraper.provinces :as provinces]
            [ring.middleware.cors :refer [wrap-cors]]
            [rapipago-scraper.cities :as cities]))

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


(defn full-address [{address :address {province-id :id} :province {city-name :name} :city}]
  (let [province-name (get (provinces db) province-id)]
    (apply str (interpose ", " [address city-name province-name "Argentina"]))))

(comment
  (get (provinces db) "A")
  (full-address (first (rapipago/search {:province {:id "E"} :city {:id "CHAJARI" :name "CHAJARI"}}))))

(defroutes app-routes
  (GET "/provinces" []
       (response (provinces/find-all)))
  (GET "/provinces/:province_id/cities" [province_id]
       (response (cities/find-in-province {:id province_id})))
  (GET "/provinces/:province_id/cities/:city_id/stores" [province_id city_id]
       (response (rapipago/search {:province {:id province_id}
                                :city     {:id city_id}})))

  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (->
    (handler/site app-routes)
    wrap-json-response
    wrap-json-body
    (wrap-cors :access-control-allow-origin #".*"
               :access-control-allow-methods [:get])))
