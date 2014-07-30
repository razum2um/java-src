(ns java-src.core
  (:require [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [clojure.string :as s])
  (:gen-class))

(def suffix "-java")

;; helpers

(defn- pwd
  []
  (io/file (System/getProperty "user.dir")))

(defn- existing-java-file?
  [file]
  (and (.exists file)
       (re-find #"\.java$" (.getName file))))

(defn- get-classname
  "Strips classname for jar"
  [file]
  (when-let [[m g] (re-find #"(.*)\.java$" (.getAbsolutePath file))]
    (str g ".class")))


(defn- find-manifest
  [files]
  (when-first [manifest (filter #(.exists %)
                                (map #(io/file (.getParent %) "MANIFEST.MF") files))]
    manifest))

;; steps

(defn- compile-java
  [file]
  (sh "javac" (.getAbsolutePath file))
  (println "Compiled:" (.getAbsolutePath file))
  file)

(defn- jarify
  "Compiles *.java and packs it to a .jar"
  [jar-name files]
  (let [params (if-let [m (find-manifest files)]
                 (str "cfm " jar-name " " (.getAbsolutePath m))
                 (str "cf " jar-name))]
    (sh "jar" params (s/join " " (map get-classname files)))
    (println "Jar created:" jar-name)
    (io/file jar-name)))

(defn- install
  [project-dir repo-name jar]
  (let [repo (io/file project-dir repo-name)
        _ (.mkdir repo)
        cmd (sh "mvn"
                "install:install-file"
                "-DcreateChecksum=true"
                "-Dpackaging=jar"
                (str "-Dfile=" (.getAbsolutePath jar))
                "-Dversion=0.0.1"
                "-DgroupId=local"
                (str "-DartifactId=" (.getName project-dir) (str suffix))
                (str "-DlocalRepositoryPath=" (.getAbsolutePath repo)))]
    (println "Jar installed.")))

(defn- print-instructions
  [project-dir]
  (println (str "Add :repositories {\"local\" \"file:repo\"} and :dependencies [local/"
                (.getName project-dir)
                (str suffix)
                " \"0.0.1\"]")))

(defn source-java
  [& file-names]
  (let [project-dir (pwd)
        files (map io/file file-names)
        repo-name (or (first (remove existing-java-file? files)) "repo")
        java-files (map compile-java (filter existing-java-file? files))
        jar-name (str (.getAbsolutePath project-dir) "/" (.getName project-dir) ".jar")
        jar (jarify jar-name java-files)]
    (install project-dir repo-name jar)
    (print-instructions project-dir)))

(defn -main
  [file-names]
  (source-java file-names)
  (System/exit 0))

