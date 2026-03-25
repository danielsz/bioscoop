(ns bioscoop.domain.specs.nlmeans
  (:require [clojure.spec.alpha :as s]))

(s/def ::s (s/double-in :min 1 :max 30))
(s/def ::p (s/int-in 0 100))
(s/def ::pc (s/int-in 0 100))
(s/def ::r (s/int-in 0 100))
(s/def ::rc (s/int-in 0 100))

(s/def ::nlmeans (s/keys :opt-un [::s ::p ::pc ::r ::rc]))