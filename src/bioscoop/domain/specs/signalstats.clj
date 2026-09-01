(ns bioscoop.domain.specs.signalstats
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.flags :as flags]
            [bioscoop.domain.specs.shared.color :as color]))

(s/def ::stat (flags/flags #{"tout" "vrep" "brng"}))
(s/def ::out #{"tout" "vrep" "brng"})
(s/def ::c ::color/color)
(s/def ::color ::color/color)

(s/def ::signalstats (s/keys :opt-un [::stat ::out ::c ::color]))
