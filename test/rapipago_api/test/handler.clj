(ns rapipago_api.test.handler
  (:require [rapipago-scraper.core :as rapipago]
            [rapipago-scraper.provinces :as provinces]
            [rapipago-scraper.cities :as cities])
  (:use clojure.test
        ring.mock.request  
        rapipago_api.handler))

(deftest test-app
  (def dummy-stores '({:address "Charcas 2143" :id "108787987" :name "Cool Rapipago"}
                      {:address "Sarandi 6412" :id "543423" :name "Awesome Rapipago"}))

  (testing "search"
    (with-redefs [rapipago/search (constantly dummy-stores)]
      (let [response (app (request :get "/provinces/C/cities/ALMAGRO/stores"))]
        (is (= (:status response) 200))
        (is (= (:body response) dummy-stores)))))

  (def dummy-provinces '({:name "Corrientes" :id "C"}
                         {:name "Salta" :id "S"}))
  (testing "provinces"
    (with-redefs [provinces/find-all (constantly dummy-provinces)]
      (let [response (app (request :get "/provinces"))]
        (is (= (:status response) 200))
        (is (= (:body response) dummy-provinces)))))

  (def dummy-cities '({:name "Goya" :id "Goya"}
                      {:name "Corrientes" :id "Corrientes"}))
  (testing "province cities"
    (with-redefs [cities/find-in-province (constantly dummy-cities)]
      (let [response (app (request :get "/provinces/C/cities"))]
        (is (= (:status response) 200))
        (is (= (:body response) [{:name "Goya" :id "Goya"}
                                 {:name "Corrientes" :id "Corrientes"}])))))
  
  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
