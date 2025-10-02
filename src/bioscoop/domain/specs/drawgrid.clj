(ns bioscoop.domain.specs.drawgrid
  (:require [clojure.spec.alpha :as s]))

(s/def ::x string?)
(s/def ::y string?)
(s/def ::width string?)
(s/def ::height string?)
(s/def ::color string?)
(s/def ::thickness string?)
(s/def ::replace boolean?)

(s/def ::drawgrid (s/keys :opt-un [::x ::y ::width ::height ::color ::thickness ::replace]))
