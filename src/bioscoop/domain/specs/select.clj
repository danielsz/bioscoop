(ns bioscoop.domain.specs.select
  (:require [clojure.spec.alpha :as s]))

(s/def ::expr string?)
(s/def ::outputs pos-int?)
(s/def ::n pos-int?)

(s/def ::select
  (s/keys :opt-un [::expr ::outputs ::n]))

(s/def ::aselect
  (s/keys :opt-un [::expr ::outputs ::n]))
