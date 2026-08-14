(ns bioscoop.domain.specs.photosensitivity
  (:require [clojure.spec.alpha :as s]))

(s/def ::frames (s/int-in 2 241))
(s/def ::threshold (s/double-in :min 0.1 :max Float/MAX_VALUE :NaN false :infinite? false))
(s/def ::skip (s/int-in 1 1025))
(s/def ::bypass boolean?)

(s/def ::photosensitivity (s/keys :opt-un [::frames ::threshold ::skip ::bypass]))
