(ns gifdex.db
  (:require [clojure.edn :as edn]
            [clojure.tools.logging :refer [info warn]]
            [clojure.java.io :as io]
            [fipp.edn :refer [pprint]]
            [com.stuartsierra.component :as component])
  (:import (java.io File)
           (java.nio.file Files
                          Path
                          CopyOption
                          StandardCopyOption
                          OpenOption
                          StandardOpenOption)
           (java.nio.file.attribute FileAttribute
                                    PosixFilePermission
                                    PosixFilePermissions)))

(defn default
  "Like assoc but only replaces missing values."
  [m k v]
  (if (contains? m k)
    m
    (assoc m k v)))

(defn load-db
  "Loads state from file."
  [index-file]
  (-> (try
        (with-open [f (java.io.PushbackReader. (io/reader index-file))]
          (edn/read f))
        (catch java.io.FileNotFoundException e
          {}))
      (default :gifs {})))

(defonce save-lock (Object.))

(defn save-db!
  "Saves db to file"
  [db-file db]
  (let [db-file (io/file db-file)
        db-path (.toAbsolutePath (.toPath db-file))]
    (locking save-lock
      (let [tmp (Files/createTempFile
                  (.getParent db-path) "gifdex" ".edn"
                  (into-array FileAttribute
                              [(PosixFilePermissions/asFileAttribute
                                #{PosixFilePermission/OWNER_READ
                                  PosixFilePermission/OWNER_WRITE})]))]
        (try
          ; Write tmpfile
          (with-open [os (Files/newOutputStream tmp
                           (into-array OpenOption
                                       [StandardOpenOption/CREATE
                                        StandardOpenOption/WRITE]))
                      w  (io/writer os)]
            (binding [*out* w]
              (pprint db)))
          ; Move tmpfile on top of db
          (io/make-parents (.getCanonicalPath db-file))
          (Files/move tmp db-path
                      (into-array CopyOption
                                  [StandardCopyOption/ATOMIC_MOVE
                                   StandardCopyOption/REPLACE_EXISTING]))
          (finally
            ; Clean up
            (Files/deleteIfExists tmp)))))))

(defrecord DB [db-file gif-dir state]
  component/Lifecycle
  (start [component]
    (assoc component :state (atom (assoc (load-db db-file)
                                         :gif-dir gif-dir))))

  (stop [component]
    component))

(defn db
  [db-file gif-dir]
  (DB. db-file gif-dir nil))

(defn transact!
  "Takes a DB record and applies the given function to its state."
  [db f & args]
  (let [db' (apply swap! (:state db) f args)]
    ; (info :db (with-out-str (pprint (apply swap! (:state db) f args))))
    (save-db! (:db-file db) db')))

(defn get-gif
  [db gif-name]
  (-> db :gifs (get gif-name)))

(defn update-gif
  "Takes a gif map with a :name and a partial list of changes. Merges those
  changes into the gif."
  [db gif']
  (update db :gifs update (:name gif') merge (dissoc gif' :name)))

(defn gif
  "Takes a db and a string gif name, and returns a gif map including a :name
  and :tags."
  [db name]
  ;  (let [bytes (Files/readAllBytes(.toPath f))
  ;        data  (b64/encode bytes)]
  ;    {:name (gif-name dir f)
  ;     ;:data (str "data:image/gif;base64," (String. data))
  ;     }))
  (assoc (get-in db [:gifs name])
         :name name))

(defn gif-name
  "Takes a File and relativizes it to the gif directory, returning a relative
  string filename like /gifs/foo"
  [dir ^File f]
  (let [dir-path        (.toPath (io/file dir))
        file-path       (.toPath f)
        relative-path   (.relativize dir-path file-path)]
    (.toString relative-path)))

(defn all-gif-names
  "Finds all gif names in a directory."
  [db]
  (->> db :gif-dir io/file file-seq
       (filter #(.isFile %))
       (map (partial gif-name (:gif-dir db)))))

(defn all-gifs
  "Takes a DB and returns all gifs."
  [db]
  (->> (all-gif-names db)
       shuffle
       (map (partial gif db))))

(defn gifs-tagged
  "Takes a DB and a tag and returns matching gifs. The special tag 'untagged'
  finds nil tags."
  [db tag]
  (->> (all-gifs db)
       (filter (if (= tag "untagged")
                 (comp nil? :tags)
                 (fn [gif] (some #{tag} (:tags gif)))))))

(defn all-tags
  "Fetches all tags from a DB, sorted by frequency."
  [db]
  (->> db :gifs vals
       (map :tags)
       (mapcat (fn [tags] (or (seq tags) ["untagged"])))
       frequencies
       (sort-by val)
       (map key)))

(defn starts-with?
  "Does the given string start with s?"
  [^String s ^String str]
  (and (<= (.length s) (.length str))
       (= s (subs str 0 (.length s)))))

(defn autocomplete-tag
  "Gives suggested autocompletes for a partial tag string, sorted by frequency."
  [db tag]
  (->> (all-tags db)
       (filter (partial starts-with? tag))))
