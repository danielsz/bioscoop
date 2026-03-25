(ns bioscoop.domain.specs.hqdn3d
  (:require [clojure.spec.alpha :as s]))

(s/def ::luma_spatial (s/double-in :min 0))
(s/def ::chroma_spatial (s/double-in :min 0))
(s/def ::luma_tmp (s/double-in :min 0))
(s/def ::chroma_tmp (s/double-in :min 0))

(s/def ::hqdn3d (s/keys :opt-un [::luma_spatial ::chroma_spatial ::luma_tmp ::chroma_tmp]))