(ns bioscoop.core
  (:require [bioscoop.dsl :as dsl]
            [bioscoop.built-in]
            [bioscoop.render :refer [to-ffmpeg]])
  (:gen-class))

(defn -main [& args]
  (println (to-ffmpeg (dsl/compile-dsl (first args)))))
