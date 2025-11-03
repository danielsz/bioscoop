(ns bioscoop.domain.specs.tinterlace
  (:require [clojure.spec.alpha :as s]))

(s/def ::mode #{"merge" "drop_even" "drop_odd" "pad" "interleave_top" "interleave_bottom" "interlacex2" "mergex2"})

(s/def ::tinterlace (s/keys :opt-un [::mode]))
