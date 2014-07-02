(ns rapipago_api.handler
  (:use compojure.core
        [ring.middleware.json :only [wrap-json-response wrap-json-body]]
        [ring.util.response :only [response]])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.cors :refer [wrap-cors]]
            [rapipago-scraper.core :as rapipago]
            [rapipago-scraper.provinces :as provinces]
            [rapipago-scraper.cities :as cities]
            [rapipago_api.es_store :as es_store]))

(defn search-stores [province-id city-id]
  (->> {:province {:id province-id}
        :city     {:id city-id}}
       rapipago/search
       (map geolocate)))

(defroutes app-routes
  (GET "/provinces" []
       (response (provinces/find-all)))
  (GET "/provinces/:province-id/cities" [province-id]
       (response (cities/find-in-province {:id province-id})))
  (GET "/provinces/:province-id/cities/:city-id/stores" [province-id city-id]
       (response (->> (search-stores province-id city-id)
                      (take 20))))

  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (->
    (handler/site app-routes)
    wrap-json-response
    wrap-json-body
    (wrap-cors :access-control-allow-origin #".*"
               :access-control-allow-methods [:get])))
