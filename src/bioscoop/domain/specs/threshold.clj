(ns bioscoop.domain.specs.threshold
  (:require [clojure.spec.alpha :as s]))

(s/def ::planes (s/int-in 0 16))
(s/def ::threshold (s/keys :opt-un [::planes]))
