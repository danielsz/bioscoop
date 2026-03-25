(ns bioscoop.domain.specs.tpad
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.color :as color]
            [bioscoop.domain.specs.shared.rational :as rational]))

(s/def ::start int?) ; Number of frames to delay input
(s/def ::stop int?) ; Number of frames to add after input finished
(s/def ::start_mode #{:add :clone}) ; Mode of added frames to start
(s/def ::stop_mode #{:add :clone}) ; Mode of added frames to end
(s/def ::start_duration ::rational/rational) ; Duration to delay input
(s/def ::stop_duration ::rational/rational) ; Duration to pad input
(s/def ::color ::color/color) ; Color of the added frames

(s/def ::tpad
  (s/keys :opt-un [::start ::stop ::start_mode ::stop_mode ::start_duration ::stop_duration ::color]))
