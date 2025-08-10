(ns bioscoop.domain.specs.layout
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.image-size :as image-size]
            [bioscoop.domain.specs.shared.color :as color]))

(s/def ::inputs pos-int?)
(s/def ::shortest boolean?)
(s/def ::fill string?)
(s/def ::layout ::image-size/image-size)
(s/def ::grid ::image-size/image-size)
(s/def ::nb_frames int?)
(s/def ::margin (s/int-in 0 1025))
(s/def ::padding (s/int-in 0 1025))

(s/def ::hstack
  (s/keys :opt-un [::inputs ::shortest]))

(s/def ::vstack
  (s/keys :opt-un [::inputs ::shortest]))

(s/def ::xstack
  (s/keys :opt-un [::inputs ::layout ::grid ::shortest ::fill]))

(s/def ::tile (s/keys :opt-un [::layout ::nb_frames ::margin ::padding ::color/color ::overlap ::init_padding]))
