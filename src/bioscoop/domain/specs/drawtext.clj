(ns bioscoop.domain.specs.drawtext
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.color :as color]
            [bioscoop.domain.specs.shared.rational :as rational]))

;; Either text, a valid file, a timecode or text source must be provided


;; Parameter types
(s/def ::box (s/or :int int? :boolean boolean?))
(s/def ::boxborderw number?)
(s/def ::boxcolor ::color/color)
(s/def ::line_spacing number?)
(s/def ::text_align #{"left" "center" "right" "bottom" "middle"})
(s/def ::y_align #{"text" "baseline" "font"})
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
(s/def ::expansion #{"none" "normal" "strftime"})

(s/def ::drawtext
  (s/keys :opt-un [::fontfile
                   ::text
                   ::textfile
                   ::fontcolor
                   ::fontcolor_expr
                   ::boxcolor
                   ::bordercolor
                   ::shadowcolor
                   ::box
                   ::boxborderw
                   ::line_spacing
                   ::fontsize
                   ::text_align
                   ::x
                   ::y
                   ::boxw
                   ::boxh
                   ::shadowx
                   ::shadowy
                   ::borderw
                   ::tabsize
                   ::basetime
                   ::font
                   ::expansion
                   ::y_align
                   ::timecode
                   ::tc24hmax
                   ::timecode_rate
                   ::rate
                   ::reload
                   ::alpha
                   ::fix_bounds
                   ::start_number
                   ::text_source
                   ::text_shaping
                   ::ft_load_flags]))
