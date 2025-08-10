(ns bioscoop.domain.specs.setpts
  (:require [clojure.spec.alpha :as s]))

;; constants in expression: FRAME_RATE, PTS, N, NB_CONSUMED_SAMPLES, NB_SAMPLES, SAMPLE_RATE, STARTPTS, STARTT, INTERLACED, T, POS, PREV_INPTS, PREV_INT, PREV_OUTPTS, PREV_OUTT, RTCTIME, RTCSTART, TB, T_CHANGE

(s/def ::expr string?)

(s/def ::asetpts
  (s/keys :req-un [::expr]))

(s/def ::setpts
  (s/keys :req-un [::expr]))
