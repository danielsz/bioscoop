(ns bioscoop.domain.specs.shared.video-rate
  (:require [clojure.spec.alpha :as s]))

(s/def ::video-rate
  (s/or :integer pos-int?
        :expr string?
        :variable #{"ntsc" "pal" "qntsc" "qpal" "sntsc" "spal" "film"}))



