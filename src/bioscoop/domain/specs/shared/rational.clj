(ns bioscoop.domain.specs.shared.rational
  (:require [clojure.spec.alpha :as s]))

(s/def ::rational
  (s/or :decimal (s/and double? #(not= 0 %))
        :expr string?))

(defn rational->str
  "Convert rational spec to FFmpeg-compatible string"
  [r]
  (let [[type val] (s/conform ::rational r)]
    (case type
      :decimal (format "%.5f" val)  ; Prevent floating-point imprecision
      :expr val)))



