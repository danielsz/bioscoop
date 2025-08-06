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

 ;; Additional drawtext parameters
(s/def ::font string?)
(s/def ::fontcolor_expr string?)
(s/def ::borderw number?)
(s/def ::bordercolor string?)
(s/def ::tab_size number?)
(s/def ::expansion #{"none" "strftime" "normal"})
(s/def ::basetime number?)
(s/def ::reload number?)
(s/def ::fix_bounds boolean?)
(s/def ::start_number number?)
(s/def ::y_align number?)

(s/def ::drawtext
  (s/keys :req-un [::text]
          :opt-un [::fontfile ::textfile ::x ::y ::fontsize ::fontcolor
                   ::box ::boxcolor ::boxborderw ::line_spacing ::shadowcolor
                   ::shadowx ::shadowy ::enable ::text_align ::font ::fontcolor_expr
                   ::borderw ::bordercolor ::tab_size ::expansion ::basetime
                   ::reload ::fix_bounds ::start_number ::y_align]))

 ;; acrossfade (audio crossfade filter)
(s/def ::overlap boolean?)
(s/def ::curve1 #{"tri" "qsin" "esin" "hsin" "log" "ipar" "qua" "cub" "squ" "cbr"})
(s/def ::curve2 #{"tri" "qsin" "esin" "hsin" "log" "ipar" "qua" "cub" "squ" "cbr"})

(s/def ::acrossfade
  (s/keys :req-un [::duration]
          :opt-un [::overlap ::curve1 ::curve2]))

 ;; acompressor (audio compressor filter)
(s/def ::level_in number?)
(s/def ::mode #{"downward" "upward"})
(s/def ::threshold number?)
(s/def ::ratio number?)
(s/def ::attack number?)
(s/def ::release number?)
(s/def ::makeup number?)
(s/def ::knee number?)
(s/def ::link #{"average" "maximum"})
(s/def ::detection #{"peak" "rms"})
(s/def ::level_sc number?)
(s/def ::mix number?)

(s/def ::acompressor
  (s/keys :opt-un [::level_in ::mode ::threshold ::ratio ::attack ::release
                   ::makeup ::knee ::link ::detection ::level_sc ::mix]))

 ;; aecho (audio echo filter)
(s/def ::in_gain number?)
(s/def ::out_gain number?)
(s/def ::delays (s/coll-of number? :min-count 1))
(s/def ::decays (s/coll-of number? :min-count 1))

(s/def ::aecho
  (s/keys :req-un [::in_gain ::out_gain ::delays ::decays]))

;; overlay (video overlay filter)
(s/def ::eof_action #{"repeat" "endall" "pass"})
(s/def ::format #{"yuv420" "yuv422" "yuv444" "yuv410" "yuv411" "yuv420p" "yuv422p" "yuv444p" "yuv420p10" "yuv422p10" "yuv444p10" "yuv420p16" "yuv422p16" "yuv444p16" "rgb24" "bgr24" "argb" "rgba" "abgr" "bgra" "gray" "gray16"})
(s/def ::repeatlast boolean?)
(s/def ::shortest boolean?)

(s/def ::overlay
  (s/keys :opt-un [::x ::y ::eof_action ::eval ::format ::repeatlast ::shortest]))

;; concat (concatenate filter)
(s/def ::n pos-int?)
(s/def ::v pos-int?)
(s/def ::a pos-int?)
(s/def ::unsafe boolean?)

(s/def ::concat
  (s/keys :opt-un [::n ::v ::a ::unsafe]))

;; hflip (horizontal flip filter)
(s/def ::hflip
  (s/keys))

;; vflip (vertical flip filter)
(s/def ::vflip
  (s/keys))

;; rotate (rotate filter)
(s/def ::angle string?)
(s/def ::out_w string?) ; already defined but reused for context
(s/def ::out_h string?) ; already defined but reused for context
(s/def ::bilinear boolean?)
(s/def ::fillcolor string?)

(s/def ::rotate
  (s/keys :opt-un [::angle ::out_w ::out_h ::bilinear ::fillcolor]))

;; transpose (transpose filter)
(s/def ::dir #{0 1 2 3 "cclock_flip" "clock" "cclock" "clock_flip"})
(s/def ::passthrough boolean?)

(s/def ::transpose
  (s/keys :opt-un [::dir ::passthrough]))

;; pad (pad filter)
(s/def ::width string?) ; already defined
(s/def ::height string?) ; already defined
(s/def ::x string?) ; already defined  
(s/def ::y string?) ; already defined
(s/def ::color string?) ; already defined
(s/def ::aspect string?)
(s/def ::eval #{"init" "frame"}) ; already defined

(s/def ::pad
  (s/keys :opt-un [::width ::height ::x ::y ::color ::aspect ::eval]))

;; trim (trim filter)
(s/def ::start string?)
(s/def ::end string?)
(s/def ::start_pts string?)
(s/def ::end_pts string?)
(s/def ::duration string?) ; already defined as number, but trim uses string expressions

(s/def ::trim
  (s/keys :opt-un [::start ::end ::start_pts ::end_pts ::duration]))

;; setpts (set presentation timestamps filter)
(s/def ::expr string?)

(s/def ::setpts
  (s/keys :req-un [::expr]))

;; fps (fps filter)
(s/def ::fps string?)
(s/def ::start_time number?) ; already defined
(s/def ::round #{"zero" "inf" "down" "up" "near"})
(s/def ::eof_action #{"round" "pass"}) ; already defined with different values, need fps-specific

(s/def ::fps
  (s/keys :opt-un [::fps ::start_time ::round ::eof_action]))

;; format (format filter)
(s/def ::pix_fmts string?)

(s/def ::format
  (s/keys :req-un [::pix_fmts]))

;; colorkey (color key filter)
(s/def ::color string?) ; already defined
(s/def ::similarity number?)
(s/def ::blend number?)

(s/def ::colorkey
  (s/keys :req-un [::color]
          :opt-un [::similarity ::blend]))

;; chromakey (chroma key filter)
(s/def ::color string?) ; already defined
(s/def ::similarity number?) ; already defined
(s/def ::blend number?) ; already defined
(s/def ::yuv boolean?)

(s/def ::chromakey
  (s/keys :req-un [::color]
          :opt-un [::similarity ::blend ::yuv]))

;; split (split filter)
(s/def ::outputs pos-int?)

(s/def ::split
  (s/keys :opt-un [::outputs]))

;; asplit (audio split filter)
(s/def ::outputs pos-int?) ; already defined

(s/def ::asplit
  (s/keys :opt-un [::outputs]))

;; amix (audio mixing filter)
(s/def ::inputs pos-int?)
(s/def ::duration #{"longest" "shortest" "first"})
(s/def ::dropout_transition number?)
(s/def ::weights string?)

(s/def ::amix
  (s/keys :opt-un [::inputs ::duration ::dropout_transition ::weights]))

;; volume (volume filter)
(s/def ::volume string?)
(s/def ::precision #{"fixed" "float" "double"})
(s/def ::replaygain #{"drop" "ignore" "track" "album"})
(s/def ::replaygain_preamp number?)
(s/def ::replaygain_noclip boolean?)
(s/def ::eval #{"once" "frame"}) ; already defined with different values

(s/def ::volume
  (s/keys :opt-un [::volume ::precision ::replaygain ::replaygain_preamp ::replaygain_noclip ::eval]))

;; pan (audio panning filter)
(s/def ::args string?) ; pan uses complex expression syntax
(s/def ::gain number?)

(s/def ::pan
  (s/keys :opt-un [::args ::gain]))

;; channelmap (channel mapping filter)
(s/def ::map string?)
(s/def ::channel_layout string?)

(s/def ::channelmap
  (s/keys :opt-un [::map ::channel_layout]))

;; aresample (audio resampling filter)
(s/def ::sample_rate pos-int?)
(s/def ::resampler #{"swr" "soxr"})
(s/def ::precision number?)
(s/def ::cheby boolean?)
(s/def ::async number?)
(s/def ::first_pts pos-int?)

(s/def ::aresample
  (s/keys :opt-un [::sample_rate ::resampler ::precision ::cheby ::async ::first_pts]))

;; aformat (audio format filter)
(s/def ::sample_fmts string?)
(s/def ::sample_rates string?)
(s/def ::channel_layouts string?)

(s/def ::aformat
  (s/keys :opt-un [::sample_fmts ::sample_rates ::channel_layouts]))

;; anull (audio null filter - pass through)
(s/def ::anull
  (s/keys))

;; null (video null filter - pass through)
(s/def ::null
  (s/keys))

;; buffersink (buffer sink)
(s/def ::pix_fmts string?) ; already defined
(s/def ::color_spaces string?)
(s/def ::color_ranges string?)

(s/def ::buffersink
  (s/keys :opt-un [::pix_fmts ::color_spaces ::color_ranges]))

;; abuffersink (audio buffer sink)
(s/def ::sample_fmts string?) ; already defined
(s/def ::sample_rates string?) ; already defined
(s/def ::channel_layouts string?) ; already defined
(s/def ::channel_counts string?)
(s/def ::all_channel_counts boolean?)

(s/def ::abuffersink
  (s/keys :opt-un [::sample_fmts ::sample_rates ::channel_layouts ::channel_counts ::all_channel_counts]))

;; buffer (video buffer source)
(s/def ::video_size string?)
(s/def ::pix_fmt string?)
(s/def ::time_base string?)
(s/def ::frame_rate string?)
(s/def ::sar string?)

(s/def ::buffer
  (s/keys :req-un [::video_size ::pix_fmt ::time_base ::frame_rate]
          :opt-un [::sar]))

;; abuffer (audio buffer source)
(s/def ::sample_rate pos-int?) ; already defined
(s/def ::sample_fmt string?)
(s/def ::channel_layout string?) ; already defined
(s/def ::channels pos-int?)

(s/def ::abuffer
  (s/keys :req-un [::sample_rate ::sample_fmt ::channel_layout]
          :opt-un [::channels]))

;; movie (movie source)
(s/def ::filename string?)
(s/def ::format_name string?)
(s/def ::seek_point number?)
(s/def ::streams string?)
(s/def ::stream_index int?)
(s/def ::loop boolean?)
(s/def ::discontinuity number?)
(s/def ::dec_threads int?)

(s/def ::movie
  (s/keys :opt-un [::filename ::format_name ::seek_point ::streams ::stream_index ::loop ::discontinuity ::dec_threads]))

;; amovie (audio movie source)
(s/def ::filename string?) ; already defined
(s/def ::format_name string?) ; already defined
(s/def ::seek_point number?) ; already defined
(s/def ::streams string?) ; already defined
(s/def ::stream_index int?) ; already defined
(s/def ::loop boolean?) ; already defined
(s/def ::discontinuity number?) ; already defined

(s/def ::amovie
  (s/keys :opt-un [::filename ::format_name ::seek_point ::streams ::stream_index ::loop ::discontinuity]))

;; testsrc (test video source)
(s/def ::size string?) ; already defined as ::s
(s/def ::rate string?)
(s/def ::duration string?) ; already defined but as number, testsrc uses string
(s/def ::sar string?) ; already defined
(s/def ::decimals int?)

(s/def ::testsrc
  (s/keys :opt-un [::size ::rate ::duration ::sar ::decimals]))

;; sine (sine wave audio generator)
(s/def ::frequency number?)
(s/def ::beep_factor number?)
(s/def ::sample_rate pos-int?) ; already defined
(s/def ::duration string?) ; already defined
(s/def ::samples_per_frame int?)

(s/def ::sine
  (s/keys :opt-un [::frequency ::beep_factor ::sample_rate ::duration ::samples_per_frame]))

;; anoisesrc (audio noise generator)
(s/def ::sample_rate pos-int?) ; already defined
(s/def ::amplitude number?)
(s/def ::duration string?) ; already defined
(s/def ::color #{"white" "pink" "brown" "blue" "violet"})
(s/def ::seed int?)
(s/def ::nb_samples int?)

(s/def ::anoisesrc
  (s/keys :opt-un [::sample_rate ::amplitude ::duration ::color ::seed ::nb_samples]))

;; color (color source)
(s/def ::color string?) ; already defined
(s/def ::size string?) ; already defined
(s/def ::rate string?) ; already defined
(s/def ::duration string?) ; already defined
(s/def ::sar string?) ; already defined

(s/def ::color
  (s/keys :opt-un [::color ::size ::rate ::duration ::sar]))
