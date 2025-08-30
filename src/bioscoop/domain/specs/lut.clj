(ns bioscoop.domain.specs.lut
  (:require [clojure.spec.alpha :as s]))

(s/def ::lut1d (s/keys :opt-un []))
(s/def ::lut3d (s/keys :opt-un []))
(s/def ::lut (s/keys :opt-un []))
(s/def ::lutrgb (s/keys :opt-un []))
(s/def ::lutyuv (s/keys :opt-un []))
