(ns bioscoop.domain.specs.afftdn
  (:require [clojure.spec.alpha :as s]))

(s/def ::noise_reduction (s/double-in :min 0.01 :max 97))
(s/def ::noise_floor (s/double-in :min -80 :max -20))
(s/def ::noise_type #{"white" "vinyl" "shellac" "custom"})
(s/def ::band_noise string?)
(s/def ::residual_floor (s/double-in :min -80 :max -20))
(s/def ::track_noise boolean?)
(s/def ::track_residual boolean?)
(s/def ::output_mode #{"input" "output" "noise"})
(s/def ::adaptivity (s/double-in :min 0 :max 1))
(s/def ::floor_offset (s/double-in :min -2 :max 2))
(s/def ::noise_link #{"none" "min" "max" "average"})
(s/def ::band_multiplier (s/double-in :min 0.2 :max 5))
(s/def ::sample_noise #{"none" "start" "stop"})
(s/def ::gain_smooth (s/int-in 0 50))

(s/def ::afftdn (s/keys :opt-un [::noise_reduction ::noise_floor ::noise_type ::band_noise
                                 ::residual_floor ::track_noise ::track_residual ::output_mode
                                 ::adaptivity ::floor_offset ::noise_link ::band_multiplier
                                 ::sample_noise ::gain_smooth]))