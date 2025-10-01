(ns bioscoop.domain.specs.life  
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.image-size :as image-size]
            [bioscoop.domain.specs.shared.video-rate :as video-rate]
            [bioscoop.domain.specs.shared.color :as color]))

(s/def ::filename string?)
(s/def ::size ::image-size/image-size)
(s/def ::rate ::video-rate/video-rate)
(s/def ::rule string?)
(s/def ::random_fill_ratio (s/double-in :min 0 :max 1))
(s/def ::random_seed number?)
(s/def ::mold (s/int-in 0 255 ))
(s/def ::life_color ::color/color)
(s/def ::death_color ::color/color)
(s/def ::mold_color ::color/color)
(s/def ::stitch boolean?)
(s/def ::life
  (s/keys :opt-un [::filename ::size ::rate ::rule ::random_fill_ratio ::random_seed ::stitch ::mold ::life_color ::death_color ::mold_color]))
