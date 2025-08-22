(ns bioscoop.domain.specs.trim
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared
             [duration :as duration]]))

(s/def ::start ::duration/duration)
(s/def ::end ::duration/duration)
(s/def ::start_pts int?)
(s/def ::end_pts int?)
(s/def ::start_frame int?)
(s/def ::end_frame int?)

(s/def ::trim
  (s/keys :opt-un [::start ::end ::start_pts ::end_pts ::duration/duration ::start_frame ::end_frame]))
