(ns bioscoop.parseable
  (:require [bioscoop.dsl :refer [compile-dsl]]
            [bioscoop.ffmpeg-parser :as ffmpeg]
            [clojure.string :as str]))

(defprotocol Parseable
  (parse [input] "Parse input into standardized Clojure records"))

(extend-protocol Parseable
  String
  (parse [s]
    (cond
      ;; Detect DSL syntax (starts with parentheses)
      (str/starts-with? (str/trim s) "(")
      (compile-dsl s)
      
      ;; Otherwise treat as FFmpeg syntax
      :else
      (ffmpeg/parse s))))
