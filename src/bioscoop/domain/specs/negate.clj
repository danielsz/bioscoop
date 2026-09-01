(ns bioscoop.domain.specs.negate
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.flags :as flags]))

(s/def ::negate_alpha boolean?)
(s/def ::components (flags/flags #{"y" "u" "v" "r" "g" "b" "a"}))

(s/def ::negate
  (s/keys :opt-un [::components ::negate_alpha]))
