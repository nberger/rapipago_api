(ns rapipago-api.import-stores
  (:require
    [rapipago_api.es-store :as es-store]))

(defn -main [& args]
  (doseq [province (keys (es-store/provinces es-store/db))]
    (println "importing province " province)
    (es-store/refresh-province province 10)))
