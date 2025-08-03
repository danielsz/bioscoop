(ns bioscoop.core
  (:require
   [instaparse.core :as insta]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [bioscoop.dsl :as dsl :refer [dsl-parser compile-dsl]]
   [bioscoop.render :refer [to-ffmpeg]]
   [bioscoop.ffmpeg-parser :as ffmpeg]
   [bioscoop.parseable :refer [parse]]
   [bioscoop.macro :refer [bioscoop]])
  (:gen-class))

(defn -main [& args]
  (log/info "Hello, World daniel"))






