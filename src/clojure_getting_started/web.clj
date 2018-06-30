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

(defn allow-cross-origin
  "middleware function to allow cross origin"
  [handler]
  (fn [request]
    (let [response (handler request)]
      (-> response
        (assoc-in [:headers "Access-Control-Allow-Origin"]  "*")
        (assoc-in [:headers "Access-Control-Allow-Methods"] "GET,PUT,POST,DELETE,OPTIONS")
        (assoc-in [:headers "Access-Control-Allow-Headers"] "X-Requested-With,Content-Type,Cache-Control")))))

(def app (-> routes
             (middleware/wrap-json-body {:keywords? true :bigdecimals? true})
             (middleware/wrap-json-response)
             allow-cross-origin))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty #'app {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
