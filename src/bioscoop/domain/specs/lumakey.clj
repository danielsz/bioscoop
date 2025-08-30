(ns bioscoop.domain.specs.lumakey
  (:require [clojure.spec.alpha :as s]))

(s/def ::threshold float?)
(s/def ::tolerance float?)
(s/def ::softness float?)

(s/def ::lumakey (s/keys :opt-un [::threshold ::tolerance ::softness]))
