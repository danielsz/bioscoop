(ns bioscoop.domain.specs.rubberband
  (:require [clojure.spec.alpha :as s]))

(s/def ::tempo (s/double-in :min 0.01 :max 100))
(s/def ::pitch (s/double-in :min 0.01 :max 100))
(s/def ::transients #{"crisp" "mixed" "smooth"})
(s/def ::detector #{"compound" "percussive" "soft"})
(s/def ::phase #{"laminar" "independent"})
(s/def ::window #{"standard" "short" "long"})
(s/def ::smoothing #{"off" "on"})
(s/def ::formant #{"shifted" "preserved"})
(s/def ::pitchq #{"quality" "speed" "consistency"})
(s/def ::channels #{"apart" "together"})

(s/def ::rubberband (s/keys :opt-un [::tempo ::pitch ::transients ::detector ::phase ::window ::smoothing ::formant ::pitchq ::channels]))
