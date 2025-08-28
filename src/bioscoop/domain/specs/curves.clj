(ns bioscoop.domain.specs.curves
  (:require [clojure.spec.alpha :as s]))

(s/def ::preset #{"none" "color_negative" "cross_process" "darker" "increase_contrast" "lighter" "linear_contrast" "medium_contrast" "negative" "strong_contrast" "vintage"})
(s/def ::interp #{"natural" "pchip"})

(s/def ::master string?)
(s/def ::red string?)
(s/def ::green string?)
(s/def ::blue string?)
(s/def ::all string?)
(s/def ::psfile string?)
(s/def ::plot string?)

(s/def ::curves (s/keys :opt-un [::preset ::master ::red ::green ::blue ::all ::psfile ::plot ::interp]))
