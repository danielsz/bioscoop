(ns bioscoop.domain.specs.tmix
  (:require [clojure.spec.alpha :as s]))

(s/def ::frames int?)
(s/def ::weights string?)
(s/def ::scale float?)
(s/def ::planes (s/int-in 0 15))

(s/def ::tmix (s/keys :opt-un [::frames ::weights ::scale ::planes]))
