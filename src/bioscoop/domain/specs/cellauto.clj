(ns bioscoop.domain.specs.cellauto
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.image-size :as image-size]
            [bioscoop.domain.specs.shared.video-rate :as video-rate]))

(s/def ::filename string?)
(s/def ::pattern string?)
(s/def ::size ::image-size/image-size)
(s/def ::rate ::video-rate/video-rate)
(s/def ::rule (s/int-in 0 255))
(s/def ::random_fill_ratio (s/double-in :min 0 :max 1))
(s/def ::random_seed number?)
(s/def ::scroll boolean?)
(s/def ::start_full boolean?)
(s/def ::stitch boolean?)

(s/def ::cellauto
  (s/keys :opt-un [::filename ::pattern ::rate ::size ::rule ::random_fill_ratio ::random_seed ::scroll ::start_full ::stitch]))
