(ns bioscoop.domain.specs.drawtext
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.color :as color]
            [bioscoop.domain.specs.shared.rational :as rational]))


(s/def ::drawtext
  (s/keys :opt-un [::box
                   ::boxborderw
                   ::boxcolor
                   ::line_spacing
                   ::text_align
                   ::text
                   ::textfile
                   ::fontcolor
                   ::fontcolor_expr
                   ::font
                   ::fontfile
                   ::fontsize
                   ::text_shaping
                   ::ft_load_flags
                   ::shadowcolor
                   ::shadowx
                   ::shadowy
                   ::bordercolor
                   ::borderw
                   ::tabsize
                   ::timecode
                   ::rate
                   ::timecode_rate
                   ::reload
                   ::alpha
                   ::x
                   ::y
                   ::fix_bounds
                   ::start_number
                   ::expansion
                   ::basetime
                   ::strftime
                   ::text_expansion
                   ::fontsize_expr
                   ::text_len]))

;; Parameter types
(s/def ::box (s/or :int int? :boolean boolean?))
(s/def ::boxborderw number?)
(s/def ::boxcolor ::color/color)
(s/def ::line_spacing number?)
(s/def ::text_align #{"left" "center" "right"})
(s/def ::text string?)
(s/def ::textfile string?)
(s/def ::fontcolor ::color/color)
(s/def ::fontcolor_expr string?)
(s/def ::font string?)
(s/def ::fontfile string?)
(s/def ::fontsize (s/or :number number? :string string?))
(s/def ::text_shaping (s/or :int int? :boolean boolean?))
(s/def ::ft_load_flags #{"default" "no_scale" "no_hinting" "render" "no_bitmap" "vertical_layout" "force_autohint" "crop_bitmap" "pedantic" "ignore_global_advance_width" "no_recurse" "ignore_transform" "monochrome" "linear_design" "no_autohint"})
(s/def ::shadowcolor ::color/color)
(s/def ::shadowx number?)
(s/def ::shadowy number?)
(s/def ::bordercolor ::color/color)
(s/def ::borderw number?)
(s/def ::tabsize number?)
(s/def ::timecode string?)
(s/def ::rate (s/or :rational ::rational/rational :num number?))
(s/def ::timecode_rate number?)
(s/def ::reload number?)
(s/def ::alpha (s/or :number number? :string string?))
(s/def ::x (s/or :number number? :string string?))
(s/def ::y (s/or :number number? :string string?))
(s/def ::fix_bounds (s/or :int int? :boolean boolean?))
(s/def ::start_number number?)
(s/def ::expansion #{"none" "normal" "strftime"})
(s/def ::basetime number?)
(s/def ::strftime (s/or :int int? :boolean boolean?))
(s/def ::text_expansion #{"none" "normal" "strftime"})
(s/def ::fontsize_expr string?)
(s/def ::text_len number?)


