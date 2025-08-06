(ns bioscoop.domain.spec
  (:require [clojure.spec.alpha :as s]))

;; Data structure specifications
(s/def ::name (s/and string? #(re-matches #"[a-zA-Z0-9_]+" %)))
(s/def ::filter-name (s/and string? #(re-matches #"[a-zA-Z0-9_]+(@[a-zA-Z0-9_]+)?" %)))
(s/def ::label (s/and string? #(re-matches #"[a-zA-Z0-9_]+" %)))

(s/def ::filter (s/keys :req-un [::filter-name]
                        :opt-un [::args]))
(s/def ::filterchain (s/coll-of ::filter))
(s/def ::filtergraph (s/coll-of ::filterchain))

;; Filter arguments

;; crop
(s/def ::out_w string?)
(s/def ::w string?)
(s/def ::out_h string?)
(s/def ::h string?)
(s/def ::x string?)
(s/def ::y string?)
(s/def ::keep_aspect boolean?)
(s/def ::exact boolean?)
(s/def ::crop (s/keys :opt-un [::out_w ::w ::out_h ::h ::x ::y ::keep_aspect ::exact]))

;; fade
(s/def ::type #{"in" "out"})
(s/def ::start_frame int?)
(s/def ::nb_frames int?)
(s/def ::alpha boolean?)
(s/def ::start_time number?)
(s/def ::duration number?)
(s/def ::color string?)
(s/def ::fade (s/keys :opt-un [::type ::start_frame ::nb_frames ::alpha ::start_time ::duration ::color]))

;; scale
;; scale filter parameters
(s/def ::width string?) ; alias for ::w
(s/def ::height string?) ; alias for ::h
(s/def ::eval #{"init" "frame"})
(s/def ::interl #{-1 0 1})
(s/def ::flags string?)
(s/def ::param0 string?)
(s/def ::param1 string?)
(s/def ::size string?)
(s/def ::s string?) ; alias for ::size
(s/def ::in_color_matrix #{"auto" "bt709" "fcc" "bt601" "bt470" "smpte170m" "smpte240m" "bt2020"})
(s/def ::out_color_matrix #{"auto" "bt709" "fcc" "bt601" "bt470" "smpte170m" "smpte240m" "bt2020"})
(s/def ::in_range #{"auto" "unknown" "jpeg" "full" "pc" "mpeg" "limited" "tv"})
(s/def ::out_range #{"auto" "unknown" "jpeg" "full" "pc" "mpeg" "limited" "tv"})
(s/def ::in_chroma_loc #{"auto" "unknown" "left" "center" "topleft" "top" "bottomleft" "bottom"})
(s/def ::out_chroma_loc #{"auto" "unknown" "left" "center" "topleft" "top" "bottomleft" "bottom"})
(s/def ::force_original_aspect_ratio #{"disable" "decrease" "increase"})
(s/def ::force_divisible_by pos-int?)

(s/def ::scale (s/keys :opt-un [::w ::h ::width ::height ::eval ::interl ::flags ::param0 ::param1
                                ::size ::s ::in_color_matrix ::out_color_matrix ::in_range ::out_range
                                ::in_chroma_loc ::out_chroma_loc ::force_original_aspect_ratio ::force_divisible_by]))

;; drawtext
(s/def ::fontfile string?)
(s/def ::text string?)
(s/def ::textfile string?)
(s/def ::fontcolor string?)
(s/def ::fontsize number?)
(s/def ::box boolean?)
(s/def ::boxcolor string?)
(s/def ::boxborderw number?)
(s/def ::line_spacing number?)
(s/def ::shadowcolor string?)
(s/def ::shadowx number?)
(s/def ::shadowy number?)
;; The enable option is a string, but it's used for timeline editing, which can be complex.
;; For now, a simple string spec is sufficient.
(s/def ::enable string?)
(s/def ::text_align
  (s/spec #{"L" "C" "R" "T" "B"
            "LT" "LC" "LB"
            "CT" "CC" "CB"
            "RT" "RC" "RB"}))

(s/def ::drawtext
  (s/keys :req-un [::text]
          :opt-un [::fontfile ::textfile ::x ::y ::fontsize ::fontcolor
                   ::box ::boxcolor ::boxborderw ::line_spacing ::shadowcolor
                   ::shadowx ::shadowy ::enable ::text_align]))




