(ns bioscoop.domain.specs.waveform
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.flags :as flags]))

(s/def ::mode #{"row" "column"})
(s/def ::intensity (s/double-in :min 0 :max 1))
(s/def ::mirror boolean?)
(s/def ::display #{"overlay" "stack" "parade"})
(s/def ::components (s/int-in 1 16))
(s/def ::envelope #{"none" "instant" "peak" "peak+instant"})
(s/def ::filter #{"lowpass" "flat" "aflat" "chroma" "color" "acolor" "xflat" "yflat"})
(s/def ::graticule #{"none" "green" "orange" "invert"})
(s/def ::opacity (s/double-in :min 0 :max 1))
(s/def ::flags (flags/flags #{"numbers" "dots"}))
(s/def ::scale #{"digital" "millivolts" "ire"})
(s/def ::bgopacity (s/double-in :min 0 :max 1))
(s/def ::tint0 (s/double-in :min -1 :max 1))
(s/def ::tint1 (s/double-in :min -1 :max 1))
(s/def ::fitmode #{"none" "size"})
(s/def ::input #{"all" "first"})

(s/def ::waveform
  (s/keys :opt-un [::mode ::intensity ::mirror ::display ::components
                   ::envelope ::filter ::graticule ::opacity ::flags
                   ::scale ::bgopacity ::tint0 ::tint1 ::fitmode ::input]))
