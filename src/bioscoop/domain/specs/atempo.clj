(ns bioscoop.domain.specs.atempo
  (:require [clojure.spec.alpha :as s]))

(s/def ::tempo (s/double-in :min 0.5 :max 100))

(s/def ::atempo (s/keys :opt-un [::tempo]))
