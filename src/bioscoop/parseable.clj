(ns bioscoop.parseable
  (:require [bioscoop.dsl :refer [compile-dsl]]
            [bioscoop.ffmpeg-parser :refer [parse-ffmpeg-filter]]
            [clojure.string :as str]))

(defprotocol Parseable
  (parse [input] "Parse input into standardized Clojure records"))

(extend-protocol Parseable
  String
  (parse [dsl-or-ffmpeg]
    (cond
      ;; Detect DSL syntax (starts with parentheses)
      (str/starts-with? (str/trim dsl-or-ffmpeg) "(")
      (compile-dsl dsl-or-ffmpeg)
      
      ;; Otherwise treat as FFmpeg syntax
      :else
      [(parse-ffmpeg-filter dsl-or-ffmpeg)])))
