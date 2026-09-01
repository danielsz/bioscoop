(ns bioscoop.domain.specs.edgedetect
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.flags :as flags]))

(s/def ::high number?)
(s/def ::low number?)
(s/def ::mode #{"wires" "colormix" "canny"})
(s/def ::planes (flags/flags #{"y" "u" "v" "r" "g" "b"}))

(s/def ::edgedetect
  (s/keys :opt-un [::high ::low ::mode ::planes]))
