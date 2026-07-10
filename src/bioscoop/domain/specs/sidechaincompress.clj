(ns bioscoop.domain.specs.sidechaincompress
  (:require [clojure.spec.alpha :as s]))

(s/def ::level_in (s/double-in :min 0.015625 :max 64)) 
(s/def ::mode #{"downward" "upward"})
(s/def ::threshold (s/double-in :min 0.000976563 :max 1))
(s/def ::ratio (s/double-in :min 1 :max 20))
(s/def ::attack (s/double-in :min 0.01 :max 2000))
(s/def ::release (s/double-in :min 0.01 :max 9000))
(s/def ::makeup (s/double-in :min 1 :max 64))
(s/def ::knee (s/double-in :min 1 :max 8))
(s/def ::link #{"average" "maximum"})
(s/def ::detection #{"peak" "rms"})
(s/def ::level_sc (s/double-in :min 0.015625 :max 64))
(s/def ::level_sc (s/double-in :min 0.015625 :max 64))
(s/def ::mix (s/double-in :min 0 :max 1))

(s/def ::sidechaincompress (s/keys :opt-un [::level_in ::mode ::threshold ::ratio ::attack ::release ::makeup ::knee ::link ::detection ::level_sc ::mix ]))
