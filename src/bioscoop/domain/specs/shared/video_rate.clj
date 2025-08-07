(ns bioscoop.domain.specs.shared.video-rate
  (:require [clojure.spec.alpha :as s]))

(s/def ::video-rate
  (s/or :integer pos-int?
        :fraction (s/tuple pos-int? pos-int?)
        :decimal (s/and double? #(> % 0))
        :expr string?
        :variable #{"ntsc" "pal" "qntsc" "qpal" "sntsc" "spal" "film"}))


(defn video-rate->str
  "Convert video rate spec to FFmpeg string format"
  [rate]
  (let [[type val] (s/conform ::video-rate rate)]
    (case type
      :integer (str val)
      :fraction (str (first val) "/" (second val))
      :decimal (format "%.2f" val)
      :expr val
      :variable val)))

