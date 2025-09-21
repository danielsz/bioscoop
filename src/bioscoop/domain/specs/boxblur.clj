(ns bioscoop.domain.specs.boxblur
  (:require [clojure.spec.alpha :as s]))

(s/def ::luma_radius string?)
(s/def ::luma_power (s/or :z zero? :p pos-int?))
(s/def ::chroma_radius string?)
(s/def ::chroma_power (s/int-in -1 Integer/MAX_VALUE))
(s/def ::alpha_radius string?)
(s/def ::alpha_power (s/int-in -1 Integer/MAX_VALUE))

(s/def ::boxblur
  (s/keys :opt-un [::luma_radius ::luma_power ::chroma_radius ::chroma_power ::alpha_radius ::alpha_power]))

