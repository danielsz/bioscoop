(ns bioscoop.domain.specs.blend
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared
             [duration :as duration]
             [rational :as rational]
             [color :as color]]))

(s/def ::mode #{"addition" "addition128" "and" "average" "bleach" "burn" "darken" "difference"
                "divide" "dodge" "exclusion" "glow" "grainextract" "grainmerge" "hardlight" 
                "hardmix" "heat" "lighten" "linearlight" "multiply" "negation" "normal" 
                "or" "overlay" "phoenix" "pinlight" "reflect" "screen" "softlight" "subtract" 
                "vividlight" "xor" "softdifference" "geometric" "harmonic" "stain" "interpolate" "hardoverlay"})

(s/def ::c0_mode ::mode)
(s/def ::c1_mode ::mode)
(s/def ::c2_mode ::mode)
(s/def ::c3_mode ::mode)
(s/def ::all_mode ::mode)

(s/def ::c0_expr string?)
(s/def ::c1_expr string?)
(s/def ::c2_expr string?)
(s/def ::c3_expr string?)
(s/def ::all_expr string?)

(s/def ::opacity-value (s/and number? #(>= % 0) #(<= % 1)))

(s/def ::c0_opacity ::opacity-value)
(s/def ::c1_opacity ::opacity-value)
(s/def ::c2_opacity ::opacity-value)
(s/def ::c3_opacity ::opacity-value)
(s/def ::all_opacity ::opacity-value)

;; Blend filter specification
(s/def ::blend
  (s/keys :opt-un [::c0_mode ::c1_mode ::c2_mode ::c3_mode ::all_mode ::c0_expr ::c1_expr ::c2_expr ::c3_expr ::all_expr ::c0_opacity ::c1_opacity ::c2_opacity ::c3_opacity ::all_opacity]))
