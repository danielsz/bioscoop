(ns bioscoop.domain.specs.frei0r
  (:require [clojure.spec.alpha :as s]))

(s/def ::filter_name string?)
(s/def ::filter_params string?)

(s/def ::frei0r
  (s/keys :opt-un [::filter_name ::filter_params]))
