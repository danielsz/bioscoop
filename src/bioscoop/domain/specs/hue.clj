(ns bioscoop.domain.specs.hue
  (:require [clojure.spec.alpha :as s]))

(s/def ::h string?)
(s/def ::s string?)
(s/def ::H string?)
(s/def ::b string?)

(s/def ::hue
  (s/keys :opt-un [::h ::s ::H ::b]))
