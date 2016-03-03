(ns rapipago-api.import-stores
  (:require
    [rapipago_api.es-store :as es-store]))

(defn import-province [province]
  (println "importing province " province)
  (es-store/refresh-province province 10))

(defn -main [& [province & more]]
  (if province
    (import-province province)
    (doseq [province (keys (es-store/provinces es-store/db))]
      (import-province province))))
