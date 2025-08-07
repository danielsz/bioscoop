(ns bioscoop.domain.specs.shared.rational
  (:require [clojure.spec.alpha :as s]))

(s/def ::rational
  (s/or :ratio ratio?
        :fraction (s/tuple int? pos-int?)
        :decimal (s/and double? #(not= 0 %))
        :expr string?
        :keyword #{"ntsc" "pal"}))

(defn rational->str
  "Convert rational spec to FFmpeg-compatible string"
  [r]
  (let [[type val] (s/conform ::rational r)]
    (case type
      :ratio (str (numerator val) "/" (denominator val))
      :fraction (str (first val) "/" (second val))
      :decimal (format "%.5f" val)  ; Prevent floating-point imprecision
      :expr val
      :keyword (name val))))



