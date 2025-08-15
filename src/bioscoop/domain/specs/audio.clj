(ns bioscoop.domain.specs.audio
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared
             [duration :as duration]]))


 ;; acrossfade (audio crossfade filter)
(s/def ::overlap boolean?)
(s/def ::curve1 #{"tri" "qsin" "esin" "hsin" "log" "ipar" "qua" "cub" "squ" "cbr"})
(s/def ::curve2 #{"tri" "qsin" "esin" "hsin" "log" "ipar" "qua" "cub" "squ" "cbr"})

(s/def ::acrossfade
  (s/keys :req-un [::duration/duration]
          :opt-un [::overlap ::curve1 ::curve2]))

 ;; acompressor (audio compressor filter)
(s/def ::level_in number?)
(s/def ::mode #{"downward" "upward"})
(s/def ::acompressor-threshold number?)
(s/def ::ratio number?)
(s/def ::attack number?)
(s/def ::release number?)
(s/def ::makeup number?)
(s/def ::knee number?)
(s/def ::link #{"average" "maximum"})
(s/def ::detection #{"peak" "rms"})
(s/def ::level_sc number?)
(s/def ::acompressor-mix number?)

(s/def ::acompressor
  (s/keys :opt-un [::level_in ::mode ::acompressor-threshold ::ratio ::attack ::release
                   ::makeup ::knee ::link ::detection ::level_sc ::acompressor-mix]))

 ;; aecho (audio echo filter)
(s/def ::in_gain number?)
(s/def ::out_gain number?)
(s/def ::delays (s/coll-of number? :min-count 1))
(s/def ::decays (s/coll-of number? :min-count 1))

(s/def ::aecho
  (s/keys :req-un [::in_gain ::out_gain ::delays ::decays]))

