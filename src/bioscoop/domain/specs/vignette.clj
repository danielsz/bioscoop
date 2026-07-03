(ns bioscoop.domain.specs.vignette
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared
             [rational :as rational]]))


(s/def ::angle string?)
(s/def ::x0 string?)
(s/def ::y0 string?)
(s/def ::mode #{"forward" "backward"})
(s/def ::eval #{"init" "frame"})
(s/def ::dither boolean?)
(s/def ::aspect ::rational/rational)

(s/def ::vignette (s/keys :opt-un [::angle ::x0 ::y0 ::mode ::eval ::dither ::aspect]))
