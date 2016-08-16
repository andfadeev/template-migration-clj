(ns template-migration-clj.core
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [clojure.edn :as edn]
            [clj-http.client :as c]
            [clojure.string :as str])
  (:gen-class))

(def test-config {:from-url "https://hh.ru"
                  :to-url "http://localhost:9091"
                  :file-path "/home/afadeev/Dropbox/github/template-migration-clj/resources/templates.csv"
                  :proto-session-path "/home/afadeev/Dropbox/github/template-migration-clj/resources/proto-session"})

(def langs [{:value 1
             :name "RU"}
            {:value 2
             :name "EN"}
            {:value 3
             :name "AZ"}
            {:value 4
             :name "KZ"}
            {:value 5
             :name "UA"}])

(defn- value-by-name [name col]
  (:value (some #(when (= name (:name %)) %) col)))

(defn get-template-data [{file-path :file-path}]
  (with-open [in-file (io/reader file-path)]
    (doall (csv/read-csv in-file :separator \;))))

(defn get-and-load-template [[ext type id site-id lang]
                             {to-url :to-url
                              from-url :from-url
                              proto-session-path :proto-session-path}]
  (let [ext (str/trim ext)]
    (println "Processing template:" ext type id site-id lang)
    (let [filename (str id "." ext)
          post-url (str to-url "/admin/billing/template/upload.mvc")
          get-url (str from-url "/file/" filename)
          _ (println "Loading file from" get-url)
          body (:body (c/get get-url {:as :byte-array}))]
      (c/post post-url
              {:headers {"Hh-Proto-Session" (slurp proto-session-path)}
               :multipart [{:name "lang" :content lang}
                           {:name "type" :content type}
                           {:name "siteId" :content site-id}
                           {:name "file.odt" :part-name "file" :content body}]}))))

(defn migrate [config]
  (println "Config:")
  (clojure.pprint/pprint config)
  (println "=====================")
  (let [templates (get-template-data config)]
    (doall (map (fn [t] (get-and-load-template t config)) templates))
    (println "=====================")
    (println "Done. Loaded" (count templates) "files.")))

(defn -main [& args]
  (let [config (edn/read-string (slurp "config.edn"))]
    (migrate config)))
