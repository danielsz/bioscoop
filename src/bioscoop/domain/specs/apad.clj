(ns bioscoop.domain.specs.apad
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.rational :as rational]))

(s/def ::packet-size int?) ; Set silence packet size
(s/def ::pad-len int?) ; Set number of samples of silence to add
(s/def ::whole-len int?) ; Set minimum target number of samples in the audio stream
(s/def ::pad-dur ::rational/rational) ; Set duration of silence to add
(s/def ::whole-dur ::rational/rational) ; Set minimum target duration in the audio stream

(s/def ::apad
  (s/keys :opt-un [::packet-size ::pad-len ::whole-len ::pad-dur ::whole-dur]))