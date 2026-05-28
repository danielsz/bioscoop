(ns bioscoop.domain.specs.anullsrc
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.duration :as duration]))

(s/def ::channel_layout string?)
(s/def ::sample_rate (s/int-in 1 Integer/MAX_VALUE))
(s/def ::nb_samples (s/int-in 1 65535))

(s/def ::anullsrc
  (s/keys :opt-un [::channel_layout ::sample_rate ::nb_samples ::duration/duration]))
