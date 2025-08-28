(ns bioscoop.domain.specs.flip
  (:require [clojure.spec.alpha :as s]))

(s/def ::hflip
  (s/keys :opt-un []))

(s/def ::vflip
  (s/keys :opt-un []))
