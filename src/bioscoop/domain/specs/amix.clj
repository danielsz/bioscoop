(ns bioscoop.domain.specs.amix
  (:require [clojure.spec.alpha :as s]))

(s/def ::inputs (s/int-in 1 32767))
(s/def ::duration #{"longest" "shortest" "first"})
(s/def ::dropout_transition (s/int-in 0 Integer/MAX_VALUE))
(s/def ::weights string?)
(s/def ::normalize boolean?)

(s/def ::amix (s/keys :opt-un [::inputs ::duration ::dropout_transition ::weights ::normalize]))
