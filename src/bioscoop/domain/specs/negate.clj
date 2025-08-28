(ns bioscoop.domain.specs.negate
  (:require [clojure.spec.alpha :as s]))

(s/def ::negate_alpha boolean?)
(s/def ::components #"^[yuvrgba+]+$")

(s/def ::negate
  (s/keys :opt-un [::components ::negate_alpha]))
