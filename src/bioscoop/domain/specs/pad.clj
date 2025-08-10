(ns bioscoop.domain.specs.pad
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared
             [color]
             [rational :as rational]]))

(s/def ::width (s/or :int int? :string string?))   ; Output width
(s/def ::height (s/or :int int? :string string?))  ; Output height
(s/def ::x (s/or :int int? :string string?))       ; X offset of input
(s/def ::y (s/or :int int? :string string?))       ; Y offset of input
(s/def ::eval #{"init" "frame"})                   ; Expression evaluation timing
(s/def ::aspect ::rational/rational)               ; Target aspect ratio (e.g., "16:9")

(s/def ::pad
  (s/keys :opt-un [::width ::height ::x ::y :bioscoop.domain.specs.shared.color/color ::eval ::aspect]))
