(ns bioscoop.domain.specs.flanger
  (:require [clojure.spec.alpha :as s]))

(s/def ::delay (s/double-in :min 0 :max 30))
(s/def ::depth (s/double-in :min 0 :max 10))
(s/def ::regen (s/double-in :min -95 :max 95))
(s/def ::width (s/double-in :min 0 :max 100))
(s/def ::speed (s/double-in :min 0.1 :max 10))
(s/def ::shape #{"sinusoidal" "triangular"})
(s/def ::phase (s/double-in :min 0 :max 100))
(s/def ::interp #{"linear" "quadratic"})

(s/def ::flanger (s/keys :opt-un [::delay ::depth ::regen ::width ::speed ::shape ::phase ::interp]))