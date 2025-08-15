(ns bioscoop.domain.specs.fps
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared
             [duration :as duration]]))

(s/def ::start_time number?)                         ; Start time in seconds
(s/def ::round #{"zero" "inf" "down" "up" "near"})   ; Timestamp rounding
(s/def ::eof_action #{"round" "pass"})               ; EOF handling

(s/def ::fps
  (s/keys :opt-un [::duration/fps ::start_time ::round ::eof_action]))
