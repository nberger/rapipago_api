(ns rapipago_api.handler
  (:use compojure.core
        [ring.middleware.json :only [wrap-json-response wrap-json-body]]
        [ring.util.response :only [response]])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [rapipago-scraper.core :as rapipago]
            [rapipago-scraper.provinces :as provinces]
            [ring.middleware.cors :refer [wrap-cors]]
            [rapipago-scraper.cities :as cities]))

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
