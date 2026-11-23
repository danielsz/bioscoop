(ns bioscoop.domain.specs.selectivecolor
  (:require [clojure.spec.alpha :as s]))

(s/def ::correction_method #{"absolute" "relative"})
(s/def ::reds string?)
(s/def ::yellows string?)
(s/def ::greens string?)
(s/def ::cyans string?)
(s/def ::blues string?)
(s/def ::magentas string?)
(s/def ::whites string?)
(s/def ::neutrals string?)
(s/def ::blacks string?)
(s/def ::psfile string?)

(s/def ::selectivecolor
  (s/keys :opt-un [::correction_method
                   ::reds ::yellows ::greens ::cyans ::blues ::magentas
                   ::whites ::neutrals ::blacks ::psfile]))
