(ns bioscoop.domain.specs.asetrate
  (:require [clojure.spec.alpha :as s]))

(s/def ::sample_rate (s/int-in 1 Integer/MAX_VALUE))

(s/def ::asetrate (s/keys :opt-un [::sample_rate]))
