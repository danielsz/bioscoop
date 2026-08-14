(ns bioscoop.domain.specs.normalize
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.color :as shared]))

(s/def ::blackpt ::shared/color)
(s/def ::whitept ::shared/color)
(s/def ::smoothing (s/int-in 0 268435456))
(s/def ::independence (s/double-in :min 0 :max 1 :NaN false :infinite? false))
(s/def ::strength (s/double-in :min 0 :max 1 :NaN false :infinite? false))

(s/def ::normalize (s/keys :opt-un [::blackpt ::whitept ::smoothing ::independence ::strength]))
