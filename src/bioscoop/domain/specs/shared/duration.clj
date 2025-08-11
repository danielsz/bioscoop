(ns bioscoop.domain.specs.shared.duration
  (:require [clojure.spec.alpha :as s]))

(s/def ::duration-unit #{"s" "ms" "us"})

(s/def ::duration
  (s/or
   :frames pos-int?
   :seconds (s/and double? #(>= % 0))
   :timestamp (s/and string? #(re-matches #"\d+:\d{2}:\d{2}(?:\.\d+)?$" %))
   :expr string?))




