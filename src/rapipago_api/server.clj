(ns rapipago_api.server
  (:require [ring.adapter.jetty :as jetty]
           [rapipago_api.handler :as handler]))


(defn -main []
  (let [port (Integer/parseInt (get (System/getenv) "PORT" "5000"))]
    (jetty/run-jetty handler/app {:port port})))
