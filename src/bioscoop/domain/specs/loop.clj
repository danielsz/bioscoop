(ns bioscoop.domain.specs.loop
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared
             [duration :as duration]] ))

(s/def ::loop
  (s/keys :opt-un [::duration/loop ::size ::start ::time]))

(s/def ::size int?)    ; Max frames in loop (default: 0)
(s/def ::start int?)   ; First frame of loop (default: 0)
(s/def ::time ::duration/duration) ; loop start time
