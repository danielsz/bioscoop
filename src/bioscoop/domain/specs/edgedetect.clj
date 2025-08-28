(ns bioscoop.domain.specs.edgedetect
  (:require [clojure.spec.alpha :as s]))

(s/def ::high number?)
(s/def ::low number?)
(s/def ::mode #{"wires" "colormix" "canny"})
(s/def ::planes #"^[yuvrgb+]+$")

(s/def ::edgedetect
  (s/keys :opt-un [::high ::low ::mode ::planes]))
