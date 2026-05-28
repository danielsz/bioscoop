(ns bioscoop.domain.specs.aevalsrc
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.duration :as duration]))

(s/def ::exprs string?)
(s/def ::sample_rate string?)
(s/def ::channel_layout string?)
(s/def ::nb_samples (s/int-in 0 Integer/MAX_VALUE))

(s/def ::aevalsrc
  (s/keys :req-un [::exprs]
          :opt-un [::sample_rate ::channel_layout ::nb_samples ::duration/duration]))
