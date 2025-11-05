(ns bioscoop.domain.specs.shuffleplanes
    (:require [clojure.spec.alpha :as s]))

(s/def ::map0 (s/int-in 0 4))
(s/def ::map1 (s/int-in 0 4))
(s/def ::map2 (s/int-in 0 4))
(s/def ::map3 (s/int-in 0 4))

(s/def ::shuffleplanes
  (s/keys :opt-un [::map0 ::map1 ::map2 ::map3]))
