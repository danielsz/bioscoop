(ns bioscoop.domain.specs.settb
  (:require [clojure.spec.alpha :as s]))

(s/def ::expr string?) ; Expression determining the output timebase
(s/def ::tb string?) ; Alias for expr (expression determining the output timebase)

(s/def ::settb
  (s/keys :opt-un [::expr ::tb]))

(s/def ::asettb
  (s/keys :opt-un [::expr ::tb]))