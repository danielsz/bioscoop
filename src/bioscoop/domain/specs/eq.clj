(ns bioscoop.domain.specs.eq
  (:require [clojure.spec.alpha :as s]))

(s/def ::contrast string?)
(s/def ::brightness string?)
(s/def ::saturation string?)
(s/def ::gamma string?)
(s/def ::gamma_r string?)
(s/def ::gamma_g string?)
(s/def ::gamma_b string?)
(s/def ::gamma_weight string?)
(s/def ::eval #{"init" "frame"})

(s/def ::eq (s/keys :opt-un [::contrast ::brightness ::saturation ::gamma ::gamma_r ::gamma_g ::gamma_b ::gamma_weight ::eval]))
