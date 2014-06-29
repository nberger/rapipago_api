(defproject rapipago_api "0.1.0-SNAPSHOT"
  :description "JSON API to search rapipago stores"
  :url "https://github.com/nberger/rapipago_api"
  :scm {:name "git"
        :url "https://github.com/nberger/rapipago_api"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.cache "0.6.3"]
                 [ring/ring-json "0.3.1"]
                 [compojure "1.1.8"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [ring-cors "0.1.4"]
                 [rapipago_scraper "0.1.0"]
                 [geocoder-clj "0.2.4"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler rapipago_api.handler/app}
  :main rapipago_api.server
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}})
