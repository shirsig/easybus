(ns clojure-getting-started.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.middleware.json :as middleware]
            [ring.util.response :refer [response]]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]))

(def *state* (ref {}))

(def *metadata* {})

(defn splash []
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hello from Heroku"})

(defn status []
  (dosync
    (response (map (fn [val] (merge val (get *metadata* (:id val)))) (vals (deref *state*))))))

(defn update [req]
  (dosync
    (alter *state* assoc (:id (:body req)) (:body req))
    (response "update successful")))

(defn set-metadata [req]
              (def *metadata* (reduce (fn [map data] (assoc map (:id data) data)) {} (:body req))))

(defroutes routes
           (GET "/" []
             (splash))
           (GET "/status" [] (status))
           (POST "/update" req (update req))
           (POST "/metadata" req (set-metadata req))
           (ANY "*" []
             (route/not-found (slurp (io/resource "404.html")))))

(def app (-> routes
             (middleware/wrap-json-body {:keywords? true :bigdecimals? true})
             (middleware/wrap-json-response)))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty #'app {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
