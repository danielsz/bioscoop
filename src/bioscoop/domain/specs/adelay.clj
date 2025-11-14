(ns bioscoop.domain.specs.adelay
  (:require [clojure.spec.alpha :as s]))

(s/def ::delays string?)
(s/def ::all boolean?)

(s/def ::adelay (s/keys :opt-un [::delays ::all]))
