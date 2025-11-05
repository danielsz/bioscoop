(ns bioscoop.domain.specs.random
  (:require [clojure.spec.alpha :as s]))

(s/def ::frames (s/int-in 2 513))
(s/def ::seed (s/int-in -1 Integer/MAX_VALUE))

(s/def ::random
  (s/keys :opt-un [::frames ::seed]))
