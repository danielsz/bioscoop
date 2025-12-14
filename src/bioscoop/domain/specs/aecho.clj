(ns bioscoop.domain.specs.aecho
  (:require [clojure.spec.alpha :as s]))

(s/def ::in_gain (s/double-in :min 0 :max 1))
(s/def ::out_gain (s/double-in :min 0 :max 1))
(s/def ::delays string?)
(s/def ::decays string?)

(s/def ::aecho (s/keys :opt-un [::in_gain ::out_gain ::delays ::decays]))
