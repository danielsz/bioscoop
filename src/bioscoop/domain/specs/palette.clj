(ns bioscoop.domain.specs.palette
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared
             [color :as color]]))

(s/def ::dither #{"bayer" "heckbert" "floyd_steinberg" "sierra2" "sierra2_4a" "sierra3" "burkes" "atkinson"})
(s/def ::bayer_scale integer?)

(s/def ::diff_mode #{"rectangle"})

(s/def ::new boolean?)

(s/def ::alpha_threshold integer?)

(s/def ::debug_kdtree string?)

(s/def ::paletteuse (s/keys :opt-un [::dither ::bayer_scale ::diff_mode ::new ::alpha_threshold ::debug_kdtree]))


(s/def ::stats_mode #{"full" "diff" "single"})
(s/def ::transparency_color ::color/color)
(s/def ::reserve_transparent boolean?)
(s/def ::max_colors integer?)
(s/def ::palettegen (s/keys :opt-un [::max_colors ::reserve_transparent ::transparency_color ::stats_mode]))
