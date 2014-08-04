(ns java-src.core
  (:require [clojure.java.shell :as shell]
            [clojure.java.io :as io]
            [clojure.string :as s])
  (:gen-class))

(def suffix "-java")

;; helpers

(defn- sh
  [& args]
  (let [cmd (apply shell/sh args)
        err (:err cmd)
        out (:out cmd)
        exit (:exit cmd)]
    (if (and (not (= 0 exit)) (not (s/blank? err)))
      (do
        (println "Tried and exited:" exit)
        (println "Launched with args:" args)
        (throw (Exception. err)))
      cmd)))

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
  (when-let [[m g] (re-find #"(.*)\.java$" (.getName file))]
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

(defn- insert-into-jar
  [jar-name file]
  (let [base-name (get-classname file)
        dir-name (.getParent file)]
    (sh "jar" "-uf" jar-name "-C" dir-name base-name)
    (println "Inserted:" base-name)))

(defn- jarify
  "Compiles *.java and packs it to a .jar"
  [jar-name files]
  (let [params (if-let [m (find-manifest files)]
                 ["-cfm" jar-name (.getAbsolutePath m)]
                 ["-cf" jar-name])
        cmd (apply sh (flatten (conj ["jar"] params)))
        jar (io/file jar-name)]
    (doall (map #(insert-into-jar jar-name %) files))
    (println "Jar created:" (.getName jar))
    jar))

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
    (.delete jar)
    (println "Jar installed.")))

(defn- print-instructions
  [project-dir repo-name]
  (println (str "Add :repositories {\"local\" \"file:"
                (str repo-name)
                "\"} and :dependencies [local/"
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
    (print-instructions project-dir repo-name)))

(defn -main
  [file-names]
  (source-java file-names)
  (System/exit 0))

