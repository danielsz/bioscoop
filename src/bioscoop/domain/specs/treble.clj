(ns bioscoop.domain.specs.treble
  (:require [clojure.spec.alpha :as s]))

(s/def ::frequency (s/double-in :min 0 :max 999999))
(s/def ::width_type #{"h" "q" "o" "s" "k"})
(s/def ::width (s/double-in :min 0 :max 99999))
(s/def ::gain (s/double-in :min -900 :max 900))
(s/def ::poles #{1 2})
(s/def ::mix (s/double-in :min 0 :max 1))
(s/def ::channels string?)
(s/def ::normalize boolean?)
(s/def ::transform #{"di" "dii" "tdi" "tdii" "latt" "svf" "zdf"})
(s/def ::precision #{"auto" "s16" "s32" "f32" "f64"})
(s/def ::blocksize (s/int-in 0 32769))

(s/def ::treble (s/keys :opt-un [::frequency ::width_type ::width ::gain ::poles
                                 ::mix ::channels ::normalize ::transform
                                 ::precision ::blocksize]))