(ns bioscoop.domain.specs.shared.duration
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.rational :refer [rational->str]]))

(s/def ::duration-unit #{"s" "ms" "us"})

(s/def ::duration
  (s/or :seconds (s/and double? #(>= % 0))
        :timestamp (s/and string? #(re-matches #"\d+:\d{2}:\d{2}(?:\.\d+)?$" %))
        :expr string?))

(defn duration->str
  "Convert duration spec to FFmpeg-compatible string"
  [d]
  (let [[type val] (s/conform ::duration d)]
    (case type
      :seconds (format "%.3f" val)  ; 3 decimal places for seconds
      :timestamp val
      :expr val)))



