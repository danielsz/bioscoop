(ns bioscoop.domain.specs.gradients
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared
             [duration :as duration]
             [rational :as rational]
             [video-rate :as video-rate]
             [image-size :as image-size]
             [color :as color]]))

(s/def ::size ::image-size/image-size)
(s/def ::rate ::video-rate/video-rate)
(s/def ::c0 ::color/color)
(s/def ::c1 ::color/color)
(s/def ::c2 ::color/color)
(s/def ::c3 ::color/color)
(s/def ::c4 ::color/color)
(s/def ::c5 ::color/color)
(s/def ::c6 ::color/color)
(s/def ::c7 ::color/color)
(s/def ::x0 integer?)
(s/def ::x1 integer?)
(s/def ::y0 integer?)
(s/def ::y1 integer?)
(s/def ::nb_colors number?)
(s/def ::speed float?)
(s/def ::type #{"linear" "radial" "circular" "spiral" "square"})

(s/def ::gradients (s/keys :opt-un [::size ::rate ::c0 ::c1 ::c2 ::c3 ::c4 ::c5 ::c6 ::c7 ::x0 ::y0 ::x1 ::y1 ::nb_colors ::seed ::duration/duration ::speed ::type]))
