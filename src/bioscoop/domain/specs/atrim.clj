(ns bioscoop.domain.specs.atrim
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.duration :as shared]))

(s/def ::start ::shared/duration)
(s/def ::end ::shared/duration)
(s/def ::start_pts (s/int-in Integer/MIN_VALUE Integer/MAX_VALUE))
(s/def ::end_pts (s/int-in Integer/MIN_VALUE Integer/MAX_VALUE))
(s/def ::start_sample (s/int-in -1 Integer/MAX_VALUE))
(s/def ::end_sample (s/int-in 0 Integer/MAX_VALUE))

(s/def ::atrim (s/keys :opt-un [::start ::end ::start_pts ::end_pts ::shared/duration ::start_sample ::end_sample]))
