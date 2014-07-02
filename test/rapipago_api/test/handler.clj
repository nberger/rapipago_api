(ns rapipago_api.test.handler
  (:require [rapipago-scraper.core :as rapipago]
            [rapipago-scraper.provinces :as provinces]
            [rapipago-scraper.cities :as cities]
            [cheshire.core :as json])
  (:use clojure.test
        ring.mock.request  
        rapipago_api.handler))

(deftest test-app
  (def dummy-stores '({:address "Charcas 2143" :id "108787987" :name "Cool Rapipago"}
                      {:address "Sarandi 6412" :id "543423" :name "Awesome Rapipago"}))

  _# (testing "search"
    (with-redefs [rapipago/search (constantly dummy-stores)
                  rapipago_api.es_store/geolocate identity]
      (let [response (app (request :get "/provinces/C/cities/ALMAGRO/stores"))]
        (is (= (:status response) 200))
        (is (= (json/parse-string (:body response) true)
               dummy-stores)))))

  (def dummy-provinces '({:name "Corrientes" :id "C"}
                         {:name "Salta" :id "S"}))
  (testing "provinces"
    (with-redefs [provinces/find-all (constantly dummy-provinces)]
      (let [response (app (request :get "/provinces"))]
        (is (= (:status response) 200))
        (is (= (json/parse-string (:body response) true)
               dummy-provinces)))))

  (def dummy-cities '({:name "Goya" :id "Goya"}
                      {:name "Corrientes" :id "Corrientes"}))
  (testing "province cities"
    (with-redefs [cities/find-in-province (constantly dummy-cities)]
      (let [response (app (request :get "/provinces/C/cities"))]
        (is (= (:status response) 200))
        (is (= (json/parse-string (:body response) true)
               [{:name "Goya" :id "Goya"}
                {:name "Corrientes" :id "Corrientes"}])))))
  
  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
