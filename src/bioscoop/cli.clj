(ns bioscoop.cli
  (:require [bioscoop.dsl :as dsl]
            [bioscoop.built-in]
            [clojure.tools.cli :refer [parse-opts]]
            [bioscoop.render :refer [to-ffmpeg]]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(def cli-options
 [["-e" "--evaluate" "Evaluate a Bioscoop expression"]
  ["-v" nil "Verbosity level, use as a flag (no arguments), repeat flag to increase verbosity" :id :verbosity :default 0 :update-fn inc]
  ["-h" "--help" "This help screen."]])

(def copyright "Copyright (c) 2026 Daniel Szmulewicz\nLicensed under the MIT License")

(defn version []
  (str "Version: " (slurp (io/resource "version.txt"))))

(defn usage [summary]
  (->> ["Bioscoop. FFmpeg compiler for creative coding"
      (version)
      ""
      "Usage: bioscoop [options] action"
      ""
      "Where [options] is"
      summary
      ""
      copyright]
     (str/join "\n")))

(defn bioscoop [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options :in-order true)]
    (when-not (zero? (:verbosity options)) (println (str " Clojure version" (clojure-version) "\n" arguments)))
    (cond
      (:help options) (println (usage summary))
      (:evaluate options) (println (to-ffmpeg (dsl/compile-dsl (first arguments))))
      (empty? arguments) (println (usage summary))
      (= 1 (count arguments)) (let [code (slurp (first arguments))]
                                (println (to-ffmpeg (dsl/compile-dsl code))))
      :else (println (usage summary)) )))
