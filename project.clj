(defproject rapipago_api "0.1.0-SNAPSHOT"
  :description "JSON API to search rapipago stores"
  :url "https://github.com/nberger/rapipago_api"
  :scm {:name "git"
        :url "https://github.com/nberger/rapipago_api"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.cache "0.6.4"]
                 [ring/ring-json "0.4.0"]
                 [compojure "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring-cors "0.1.7"]
                 [ring-logger "0.7.5"]
                 [clojurewerkz/elastisch "2.2.1"]
                 [rapipago_scraper "0.1.1-SNAPSHOT"]
                 [com.climate/claypoole "1.1.2"]
                 [geocoder-clj "0.2.5"]]
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler rapipago_api.handler/app}
  :main rapipago_api.server
  :min-lein-version "2.4.0"
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
