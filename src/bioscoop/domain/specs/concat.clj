(ns bioscoop.domain.specs.concat
  (:require [clojure.spec.alpha :as s]))


;; Parameter types
(s/def ::n int?)       ; Number of input segments (required)
(s/def ::v int?)       ; Number of video streams per segment (default: 1)
(s/def ::a int?)       ; Number of audio streams per segment (default: 0)
(s/def ::unsafe (s/or :int int? :boolean boolean?))  ; Enable unsafe mode (default: 0/false)

(s/def ::concat
  (s/keys :opt-un [::n ::v ::a ::unsafe]))
