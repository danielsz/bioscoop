(ns bioscoop.domain.specs.maskedmerge
  (:require [clojure.spec.alpha :as s]))

(s/def ::planes (s/int-in 0 16))

(s/def ::maskedmerge (s/keys :opt-un [::planes]))
