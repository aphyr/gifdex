(ns gifdex.core
  (:require [aleph.http :as http]
            [clojure.data.codec.base64 :as b64]
            [clojure.edn :as edn]
            [clojure.tools.logging :refer [info warn]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.core.match :refer [match]]
            [ring.util [response :as response]
                       [mime-type :as mime-type]]
            [ring.middleware [gzip :refer [wrap-gzip]]
                             [params :refer [wrap-params]]
                             [not-modified :as nm :refer [wrap-not-modified]]
                             [reload :refer [wrap-reload]]]
            [cheshire.core :as json]
            [fipp.edn :refer [pprint]]
            [unilog.config :refer [start-logging!]]
            [com.stuartsierra.component :as component]
            [gifdex [db :as db]])
  (:import (java.io File)
           (java.nio.file Files Path)))

(defn compile-middleware
  "Takes a sequence of middleware wrappers and composes them into a single
  handler. The first middleware in the sequence is the outermost in the call.
  With no handler, returns a wrapper function."
  ([middleware]
   (fn [handler] (compile-middleware handler middleware)))
  ([handler middleware]
   (reduce (fn [h mw] (mw h)) handler (reverse middleware))))

(def err-404
  {:status 404
   :headers {"Content-Type" "text/plain"}
   :body "Nothing to see here; move along."})

(def err-503
  {:status 503
   :headers {"Content-Type" "text/plain"}
   :body "Welp."})

(defn join-path
  "Joins a sequence of path fragments into a string with /. Optionally appends
  an extension."
  ([fragments]
   (str/join "/" (cons nil fragments)))
  ([fragments extension]
   (if extension
     (str (join-path fragments) "." extension)
     (join-path fragments))))

(defn split-path
  "Splits a path up into a vector of [components extension]."
  [path]
  (let [path (->> (str/split path #"/")
                  (remove str/blank?)
                  vec)]
    (if (empty? path)
      [path nil]
      (let [[_ file _ ext]  (re-find #"^(.*?)(\.([^\d]+))?$" (peek path))
            path            (assoc path (dec (count path)) file)]
        [path ext]))))

(defn static
  "Constructs a ring handler which serves requests for static files, by path
  and extension."
  [static-dir]
  (fn handle [req]
    (let [path      (:path req)
          extension (:ext req)]
      (if-let [res (response/file-response (join-path path extension)
                                           {:root static-dir})]
        ; Assign a content type based on the file extension
        (let [filename (.getName ^File (:body res))]
          (response/content-type res (mime-type/ext-mime-type filename nil)))
        err-404))))

(defn json-response
  [body]
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    (json/generate-string body)})

(defn all-gifs
  [db]
  (json-response (take 200 (db/all-gifs @(:state db)))))

(defn gifs-tagged
  [db tag]
  (json-response (db/gifs-tagged @(:state db) tag)))

(defn tags
  [db]
  (json-response (db/all-tags @(:state db))))

(defn gif
  [db req]
  (info :gif req)
  (case (:request-method req)
    :post (let [gif' (with-open [r (io/reader (:body req))]
                       (json/parse-stream r true))
                _ (info :gif' gif')
                gif' (select-keys gif' [:name :tags])]
            (assert (string? (:name gif')))
            (assert (every? string? (:tags gif')))
            (db/transact! db db/update-gif gif')
            (json-response gif'))))

(defn home
  []
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (slurp (io/resource "index.html"))})

(defn handler
  "Ring handler for an app"
  [app]
  (let [db          (:db app)
        gif-static  (static (:gif-dir app))]
    (fn handle [req]
      (let [uri (java.net.URLDecoder/decode (:uri req) "UTF-8")
            [path ext] (split-path uri)
            req (assoc req :uri uri, :path path, :ext ext)]
        ; (info :req req)
        (match [path ext]
               [[] nil]                   (home)
               [["favicon"] "ico"]        err-404
               [["tags" t "gifs"] nil]    (gifs-tagged db t)
               [["gifs"] nil]             (all-gifs db)

               [["gifs" "meta" & more] _]
               (gif db (assoc req :path more))

               [["gifs" & more] _]
               (gif-static (assoc req :path more)))))))

(defn wrap-exceptions
  "Catch exceptions, log them, and return a short string to the user"
  [handler]
  (fn [req]
    (try (handler req)
         (catch Throwable t
           (warn t "Exception handling\n" (with-out-str (pprint req)))
           err-503))))

(defrecord WebServer [port app server]
  component/Lifecycle
  (start [this]
    (let [middleware [wrap-gzip
                      wrap-exceptions
                      wrap-reload]
          handler (compile-middleware (handler app) middleware)
          server  (http/start-server handler {:port port})]
      (info (str "Server running: http://localhost:" port "/"))
      (assoc this :server server)))

  (stop [this]
    (.close server)
    (dissoc this server)))

(defn web-server
  "Constructs a web server which uses an App."
  [port]
  (WebServer. port nil nil))

(defrecord App [gif-dir db])

(defn app
  [gif-dir]
  (App. gif-dir nil))

(defn system
  [opts]
  (component/system-map
    :db  (db/db (:db-file opts) (:gif-dir opts))
    :app (component/using (app (:gif-dir opts)) [:db])
    :web-server (component/using (web-server (:port opts)) [:app])))

(defn -main
  [gif-dir & args]
  (let [port (Long/parseLong (or (first args) "6549"))
        system (system {:port       port
                        :db-file   "db.edn"
                        :gif-dir    gif-dir})]
    (component/start system)
    (while true
      (Thread/sleep 10000))))
