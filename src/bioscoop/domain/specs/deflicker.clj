(ns bioscoop.domain.specs.deflicker
  (:require [clojure.spec.alpha :as s]))

(s/def ::size (s/int-in 2 130))
(s/def ::mode #{"am" "gm" "hm" "qm" "cm" "pm" "median"})
(s/def ::bypass boolean?)

(s/def ::deflicker (s/keys :opt-un [::size ::mode ::bypass]))
