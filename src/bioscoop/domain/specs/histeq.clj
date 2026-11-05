(ns bioscoop.domain.specs.histeq
  (:require [clojure.spec.alpha :as s]))

(s/def ::strength (s/double-in :min 0 :max 1 :NaN false :infinite? false))
(s/def ::intensity (s/double-in :min 0 :max 1 :NaN false :infinite? false))
(s/def ::antibanding #{"none" "weak" "strong"})

(s/def ::histeq (s/keys :opt-un [::strength ::intensity ::antibanding]))
