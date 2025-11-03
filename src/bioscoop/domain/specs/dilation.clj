(ns bioscoop.domain.specs.dilation
  (:require [clojure.spec.alpha :as s]))

(s/def ::coordinates (s/int-in 0 255))
(s/def ::threshold0 (s/int-in 0 65535))
(s/def ::threshold1 (s/int-in 0 65535))
(s/def ::threshold2 (s/int-in 0 65535))
(s/def ::threshold3 (s/int-in 0 65535))

(s/def ::dilation (s/keys :opt-un [::coordinates ::threshold0 ::threshold1  ::threshold2 ::threshold3]))
(s/def ::erosion (s/keys :opt-un [::coordinates ::threshold0 ::threshold1  ::threshold2 ::threshold3]))
