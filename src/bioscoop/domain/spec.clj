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

(s/def ::string string?)
(s/def ::int integer?)
(s/def ::float float? )

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

(s/def ::overlay-x ::string)
(s/def ::overlay-y ::string)
(s/def ::overlay-format (s/nilable #{"yuv420" "yuv422" "yuv444" "yuv410" "yuv411" "yuvj420" "yuvj422" "yuvj444" "auto"}))
(s/def ::overlay-rgb (s/nilable ::boolean))
(s/def ::overlay-shortest (s/nilable ::boolean))
(s/def ::overlay-repeatlast (s/nilable ::boolean))
(s/def ::overlay (s/keys :opt-un [::overlay-x ::overlay-y ::overlay-format ::overlay-rgb ::overlay-shortest ::overlay-repeatlast]))

(s/def ::concat-n ::int)
(s/def ::concat-v ::int)
(s/def ::concat-a ::int)
(s/def ::concat-unsafe (s/nilable ::boolean))
(s/def ::concat (s/keys :opt-un [::concat-n ::concat-v ::concat-a ::concat-unsafe]))

(s/def ::hflip (s/keys :opt-un []))

(s/def ::vflip (s/keys :opt-un []))

(s/def ::rotate-angle ::string)
(s/def ::rotate-out_w ::string)
(s/def ::rotate-out_h ::string)
(s/def ::rotate-fillcolor ::string)
(s/def ::rotate (s/keys :opt-un [::rotate-angle ::rotate-out_w ::rotate-out_h ::rotate-fillcolor]))

(s/def ::transpose-dir (s/nilable #{"0" "1" "2" "3" "cclock_flip" "clock" "cclock" "clock_flip"}))
(s/def ::transpose-passthrough (s/nilable #{"none" "portrait" "landscape"}))
(s/def ::transpose (s/keys :opt-un [::transpose-dir ::transpose-passthrough]))

(s/def ::pad-width ::string)
(s/def ::pad-height ::string)
(s/def ::pad-x ::string)
(s/def ::pad-y ::string)
(s/def ::pad-color ::string)
(s/def ::pad-eval (s/nilable #{"init" "frame"}))
(s/def ::pad-aspect ::string)
(s/def ::pad (s/keys :opt-un [::pad-width ::pad-height ::pad-x ::pad-y ::pad-color ::pad-eval ::pad-aspect]))

(s/def ::trim-start ::string)
(s/def ::trim-end ::string)
(s/def ::trim-start_pts ::string)
(s/def ::trim-end_pts ::string)
(s/def ::trim-duration ::string)
(s/def ::trim-start_frame ::int)
(s/def ::trim-end_frame ::int)
(s/def ::trim (s/keys :opt-un [::trim-start ::trim-end ::trim-start_pts ::trim-end_pts ::trim-duration ::trim-start_frame ::trim-end_frame]))

(s/def ::setpts-expr ::string)
(s/def ::setpts (s/keys :req-un [::setpts-expr]))

(s/def ::fps-fps ::string)
(s/def ::fps-start_time ::string)
(s/def ::fps-round (s/nilable #{"zero" "inf" "down" "up" "near"}))
(s/def ::fps-eof_action (s/nilable #{"round" "pass"}))
(s/def ::fps (s/keys :opt-un [::fps-fps ::fps-start_time ::fps-round ::fps-eof_action]))

(s/def ::format-pix_fmts ::string)
(s/def ::format (s/keys :req-un [::format-pix_fmts]))

(s/def ::colorkey-color ::string)
(s/def ::colorkey-similarity ::float)
(s/def ::colorkey-blend ::float)
(s/def ::colorkey (s/keys :req-un [::colorkey-color] :opt-un [::colorkey-similarity ::colorkey-blend]))

(s/def ::chromakey-color ::string)
(s/def ::chromakey-similarity ::float)
(s/def ::chromakey-blend ::float)
(s/def ::chromakey-yuv (s/nilable ::boolean))
(s/def ::chromakey (s/keys :req-un [::chromakey-color] :opt-un [::chromakey-similarity ::chromakey-blend ::chromakey-yuv]))

(s/def ::split-outputs ::int)
(s/def ::split (s/keys :opt-un [::split-outputs]))

(s/def ::null (s/keys :opt-un []))

(s/def ::buffersink (s/keys :opt-un []))

;; Audio Processing Filters
(s/def ::asplit-outputs ::int)
(s/def ::asplit (s/keys :opt-un [::asplit-outputs]))

(s/def ::amix-inputs ::int)
(s/def ::amix-duration (s/nilable #{"longest" "shortest" "first"}))
(s/def ::amix-dropout_transition ::float)
(s/def ::amix-weights ::string)
(s/def ::amix-normalize (s/nilable ::boolean))
(s/def ::amix (s/keys :opt-un [::amix-inputs ::amix-duration ::amix-dropout_transition ::amix-weights ::amix-normalize]))

(s/def ::volume-volume ::string)
(s/def ::volume-precision (s/nilable #{"fixed" "float" "double"}))
(s/def ::volume-eval (s/nilable #{"once" "frame"}))
(s/def ::volume-replaygain (s/nilable #{"drop" "ignore" "track" "album"}))
(s/def ::volume-replaygain_preamp ::float)
(s/def ::volume-replaygain_noclip (s/nilable ::boolean))
(s/def ::volume (s/keys :req-un [::volume-volume] :opt-un [::volume-precision ::volume-eval ::volume-replaygain ::volume-replaygain_preamp ::volume-replaygain_noclip]))

(s/def ::pan-args ::string)
(s/def ::pan (s/keys :req-un [::pan-args]))

(s/def ::channelmap-map ::string)
(s/def ::channelmap-channel_layout ::string)
(s/def ::channelmap (s/keys :opt-un [::channelmap-map ::channelmap-channel_layout]))

(s/def ::aresample-sample_rate ::int)
(s/def ::aresample-resampler (s/nilable #{"swr" "soxr"}))
(s/def ::aresample-precision ::float)
(s/def ::aresample-cheby (s/nilable ::boolean))
(s/def ::aresample-beta ::float)
(s/def ::aresample (s/keys :opt-un [::aresample-sample_rate ::aresample-resampler ::aresample-precision ::aresample-cheby ::aresample-beta]))

(s/def ::aformat-sample_fmts ::string)
(s/def ::aformat-sample_rates ::string)
(s/def ::aformat-channel_layouts ::string)
(s/def ::aformat (s/keys :opt-un [::aformat-sample_fmts ::aformat-sample_rates ::aformat-channel_layouts]))

(s/def ::anull (s/keys :opt-un []))

(s/def ::abuffersink (s/keys :opt-un []))

;; Sources
(s/def ::buffer-video_size ::string)
(s/def ::buffer-pix_fmt ::string)
(s/def ::buffer-time_base ::string)
(s/def ::buffer-pixel_aspect ::string)
(s/def ::buffer-sws_param ::string)
(s/def ::buffer-frame_rate ::string)
(s/def ::buffer (s/keys :req-un [::buffer-video_size ::buffer-pix_fmt ::buffer-time_base] :opt-un [::buffer-pixel_aspect ::buffer-sws_param ::buffer-frame_rate]))

(s/def ::abuffer-time_base ::string)
(s/def ::abuffer-sample_rate ::int)
(s/def ::abuffer-sample_fmt ::string)
(s/def ::abuffer-channel_layout ::string)
(s/def ::abuffer-channels ::int)
(s/def ::abuffer (s/keys :req-un [::abuffer-time_base ::abuffer-sample_rate ::abuffer-sample_fmt ::abuffer-channel_layout] :opt-un [::abuffer-channels]))

(s/def ::movie-filename ::string)
(s/def ::movie-format_name ::string)
(s/def ::movie-seek_point ::float)
(s/def ::movie-streams ::string)
(s/def ::movie-stream_index ::int)
(s/def ::movie-loop ::int)
(s/def ::movie-discontinuity ::float)
(s/def ::movie (s/keys :req-un [::movie-filename] :opt-un [::movie-format_name ::movie-seek_point ::movie-streams ::movie-stream_index ::movie-loop ::movie-discontinuity]))

(s/def ::amovie-filename ::string)
(s/def ::amovie-format_name ::string)
(s/def ::amovie-seek_point ::float)
(s/def ::amovie-streams ::string)
(s/def ::amovie-stream_index ::int)
(s/def ::amovie-loop ::int)
(s/def ::amovie-discontinuity ::float)
(s/def ::amovie (s/keys :req-un [::amovie-filename] :opt-un [::amovie-format_name ::amovie-seek_point ::amovie-streams ::amovie-stream_index ::amovie-loop ::amovie-discontinuity]))

(s/def ::sine-frequency ::float)
(s/def ::sine-beep_factor ::float)
(s/def ::sine-sample_rate ::int)
(s/def ::sine-duration ::float)
(s/def ::sine-samples_per_frame ::int)
(s/def ::sine (s/keys :opt-un [::sine-frequency ::sine-beep_factor ::sine-sample_rate ::sine-duration ::sine-samples_per_frame]))

(s/def ::anoisesrc-sample_rate ::int)
(s/def ::anoisesrc-amplitude ::float)
(s/def ::anoisesrc-duration ::float)
(s/def ::anoisesrc-color (s/nilable #{"white" "pink" "brown" "blue" "violet"}))
(s/def ::anoisesrc-seed ::int)
(s/def ::anoisesrc-nb_samples ::int)
(s/def ::anoisesrc (s/keys :opt-un [::anoisesrc-sample_rate ::anoisesrc-amplitude ::anoisesrc-duration ::anoisesrc-color ::anoisesrc-seed ::anoisesrc-nb_samples]))

;; Video Sources
(s/def ::allrgb-rate ::string)
(s/def ::allrgb-duration ::string)
(s/def ::allrgb (s/keys :opt-un [::allrgb-rate ::allrgb-duration]))

(s/def ::allyuv-rate ::string)
(s/def ::allyuv-duration ::string)
(s/def ::allyuv (s/keys :opt-un [::allyuv-rate ::allyuv-duration]))

(s/def ::colorchart-rate ::string)
(s/def ::colorchart-duration ::string)
(s/def ::colorchart-sar ::string)
(s/def ::colorchart-patch_size ::string)
(s/def ::colorchart-preset (s/nilable #{"reference" "skintones"}))
(s/def ::colorchart (s/keys :opt-un [::colorchart-rate ::colorchart-duration ::colorchart-sar ::colorchart-patch_size ::colorchart-preset]))

(s/def ::colorspectrum-size ::string)
(s/def ::colorspectrum-rate ::string)
(s/def ::colorspectrum-duration ::string)
(s/def ::colorspectrum-sar ::string)
(s/def ::colorspectrum-type (s/nilable #{"black" "white"}))
(s/def ::colorspectrum (s/keys :opt-un [::colorspectrum-size ::colorspectrum-rate ::colorspectrum-duration ::colorspectrum-sar ::colorspectrum-type]))

(s/def ::haldclutsrc-level ::int)
(s/def ::haldclutsrc-rate ::string)
(s/def ::haldclutsrc-duration ::string)
(s/def ::haldclutsrc-sar ::string)
(s/def ::haldclutsrc (s/keys :opt-un [::haldclutsrc-level ::haldclutsrc-rate ::haldclutsrc-duration ::haldclutsrc-sar]))

(s/def ::nullsrc-size ::string)
(s/def ::nullsrc-rate ::string)
(s/def ::nullsrc-duration ::string)
(s/def ::nullsrc-sar ::string)
(s/def ::nullsrc (s/keys :opt-un [::nullsrc-size ::nullsrc-rate ::nullsrc-duration ::nullsrc-sar]))

(s/def ::pal75bars-size ::string)
(s/def ::pal75bars-rate ::string)
(s/def ::pal75bars-duration ::string)
(s/def ::pal75bars-sar ::string)
(s/def ::pal75bars (s/keys :opt-un [::pal75bars-size ::pal75bars-rate ::pal75bars-duration ::pal75bars-sar]))

(s/def ::pal100bars-size ::string)
(s/def ::pal100bars-rate ::string)
(s/def ::pal100bars-duration ::string)
(s/def ::pal100bars-sar ::string)
(s/def ::pal100bars (s/keys :opt-un [::pal100bars-size ::pal100bars-rate ::pal100bars-duration ::pal100bars-sar]))

(s/def ::rgbtestsrc-size ::string)
(s/def ::rgbtestsrc-rate ::string)
(s/def ::rgbtestsrc-duration ::string)
(s/def ::rgbtestsrc-sar ::string)
(s/def ::rgbtestsrc-complement (s/nilable ::boolean))
(s/def ::rgbtestsrc (s/keys :opt-un [::rgbtestsrc-size ::rgbtestsrc-rate ::rgbtestsrc-duration ::rgbtestsrc-sar ::rgbtestsrc-complement]))

(s/def ::smptebars-size ::string)
(s/def ::smptebars-rate ::string)
(s/def ::smptebars-duration ::string)
(s/def ::smptebars-sar ::string)
(s/def ::smptebars (s/keys :opt-un [::smptebars-size ::smptebars-rate ::smptebars-duration ::smptebars-sar]))

(s/def ::smptehdbars-size ::string)
(s/def ::smptehdbars-rate ::string)
(s/def ::smptehdbars-duration ::string)
(s/def ::smptehdbars-sar ::string)
(s/def ::smptehdbars (s/keys :opt-un [::smptehdbars-size ::smptehdbars-rate ::smptehdbars-duration ::smptehdbars-sar]))

(s/def ::testsrc2-size ::string)
(s/def ::testsrc2-rate ::string)
(s/def ::testsrc2-duration ::string)
(s/def ::testsrc2-sar ::string)
(s/def ::testsrc2-alpha ::int)
(s/def ::testsrc2 (s/keys :opt-un [::testsrc2-size ::testsrc2-rate ::testsrc2-duration ::testsrc2-sar ::testsrc2-alpha]))

(s/def ::yuvtestsrc-size ::string)
(s/def ::yuvtestsrc-rate ::string)
(s/def ::yuvtestsrc-duration ::string)
(s/def ::yuvtestsrc-sar ::string)
(s/def ::yuvtestsrc (s/keys :opt-un [::yuvtestsrc-size ::yuvtestsrc-rate ::yuvtestsrc-duration ::yuvtestsrc-sar]))

(s/def ::sierpinski-size ::string)
(s/def ::sierpinski-rate ::string)
(s/def ::sierpinski-seed ::int)
(s/def ::sierpinski-jump ::int)
(s/def ::sierpinski-type (s/nilable #{"carpet" "triangle"}))
(s/def ::sierpinski (s/keys :opt-un [::sierpinski-size ::sierpinski-rate ::sierpinski-seed ::sierpinski-jump ::sierpinski-type]))

(s/def ::zoneplate-size ::string)
(s/def ::zoneplate-rate ::string)
(s/def ::zoneplate-duration ::string)
(s/def ::zoneplate-sar ::string)
(s/def ::zoneplate-precision ::int)
(s/def ::zoneplate-xo ::int)
(s/def ::zoneplate-yo ::int)
(s/def ::zoneplate-to ::int)
(s/def ::zoneplate-k0 ::int)
(s/def ::zoneplate-kx ::int)
(s/def ::zoneplate-ky ::int)
(s/def ::zoneplate-kt ::int)
(s/def ::zoneplate-kxt ::int)
(s/def ::zoneplate-kyt ::int)
(s/def ::zoneplate-kxy ::int)
(s/def ::zoneplate (s/keys :opt-un [::zoneplate-size ::zoneplate-rate ::zoneplate-duration ::zoneplate-sar ::zoneplate-precision ::zoneplate-xo ::zoneplate-yo ::zoneplate-to ::zoneplate-k0 ::zoneplate-kx ::zoneplate-ky ::zoneplate-kt ::zoneplate-kxt ::zoneplate-kyt ::zoneplate-kxy]))

(s/def ::mandelbrot-size ::string)
(s/def ::mandelbrot-rate ::string)
(s/def ::mandelbrot-maxiter ::int)
(s/def ::mandelbrot-start_scale ::float)
(s/def ::mandelbrot-end_scale ::float)
(s/def ::mandelbrot-end_pts ::float)
(s/def ::mandelbrot-bailout ::float)
(s/def ::mandelbrot-morphxf ::float)
(s/def ::mandelbrot-morphyf ::float)
(s/def ::mandelbrot-morphamp ::float)
(s/def ::mandelbrot-outer (s/nilable #{"iteration_count" "normalized_iteration_count" "white" "outz"}))
(s/def ::mandelbrot-inner (s/nilable #{"black" "period" "convergence" "mincol"}))
(s/def ::mandelbrot (s/keys :opt-un [::mandelbrot-size ::mandelbrot-rate ::mandelbrot-maxiter ::mandelbrot-start_scale ::mandelbrot-end_scale ::mandelbrot-end_pts ::mandelbrot-bailout ::mandelbrot-morphxf ::mandelbrot-morphyf ::mandelbrot-morphamp ::mandelbrot-outer ::mandelbrot-inner]))

(s/def ::life-filename ::string)
(s/def ::life-size ::string)
(s/def ::life-rate ::string)
(s/def ::life-rule ::string)
(s/def ::life-random_fill_ratio ::float)
(s/def ::life-random_seed ::int)
(s/def ::life-stitch (s/nilable ::boolean))
(s/def ::life-mold ::int)
(s/def ::life-life_color ::string)
(s/def ::life-death_color ::string)
(s/def ::life-mold_color ::string)
(s/def ::life (s/keys :opt-un [::life-filename ::life-size ::life-rate ::life-rule ::life-random_fill_ratio ::life-random_seed ::life-stitch ::life-mold ::life-life_color ::life-death_color ::life-mold_color]))

;; Video Sinks
(s/def ::nullsink (s/keys :opt-un []))

(s/def ::rawvideo-filename ::string)
(s/def ::rawvideo (s/keys :req-un [::rawvideo-filename]))

;; Video Effects
(s/def ::blur-luma_radius ::float)
(s/def ::blur-luma_power ::int)
(s/def ::blur-chroma_radius ::float)
(s/def ::blur-chroma_power ::int)
(s/def ::blur-alpha_radius ::float)
(s/def ::blur-alpha_power ::int)
(s/def ::blur (s/keys :opt-un [::blur-luma_radius ::blur-luma_power ::blur-chroma_radius ::blur-chroma_power ::blur-alpha_radius ::blur-alpha_power]))

(s/def ::gblur-sigma ::float)
(s/def ::gblur-steps ::int)
(s/def ::gblur-planes ::int)
(s/def ::gblur-sigmaV ::float)
(s/def ::gblur (s/keys :opt-un [::gblur-sigma ::gblur-steps ::gblur-planes ::gblur-sigmaV]))

(s/def ::convolution-0m ::string)
(s/def ::convolution-1m ::string)
(s/def ::convolution-2m ::string)
(s/def ::convolution-3m ::string)
(s/def ::convolution-0rdiv ::float)
(s/def ::convolution-1rdiv ::float)
(s/def ::convolution-2rdiv ::float)
(s/def ::convolution-3rdiv ::float)
(s/def ::convolution-0bias ::float)
(s/def ::convolution-1bias ::float)
(s/def ::convolution-2bias ::float)
(s/def ::convolution-3bias ::float)
(s/def ::convolution-0mode (s/nilable #{"square" "row" "column"}))
(s/def ::convolution-1mode (s/nilable #{"square" "row" "column"}))
(s/def ::convolution-2mode (s/nilable #{"square" "row" "column"}))
(s/def ::convolution-3mode (s/nilable #{"square" "row" "column"}))
(s/def ::convolution (s/keys :opt-un [::convolution-0m ::convolution-1m ::convolution-2m ::convolution-3m ::convolution-0rdiv ::convolution-1rdiv ::convolution-2rdiv ::convolution-3rdiv ::convolution-0bias ::convolution-1bias ::convolution-2bias ::convolution-3bias ::convolution-0mode ::convolution-1mode ::convolution-2mode ::convolution-3mode]))

(s/def ::curves-preset (s/nilable #{"none" "color_negative" "cross_process" "darker" "increase_contrast" "lighter" "linear_contrast" "medium_contrast" "negative" "strong_contrast" "vintage"}))
(s/def ::curves-master ::string)
(s/def ::curves-red ::string)
(s/def ::curves-green ::string)
(s/def ::curves-blue ::string)
(s/def ::curves-all ::string)
(s/def ::curves-psfile ::string)
(s/def ::curves-plot ::string)
(s/def ::curves (s/keys :opt-un [::curves-preset ::curves-master ::curves-red ::curves-green ::curves-blue ::curves-all ::curves-psfile ::curves-plot]))

(s/def ::eq-contrast ::string)
(s/def ::eq-brightness ::string)
(s/def ::eq-saturation ::string)
(s/def ::eq-gamma ::string)
(s/def ::eq-gamma_r ::string)
(s/def ::eq-gamma_g ::string)
(s/def ::eq-gamma_b ::string)
(s/def ::eq-gamma_weight ::string)
(s/def ::eq-eval (s/nilable #{"init" "frame"}))
(s/def ::eq (s/keys :opt-un [::eq-contrast ::eq-brightness ::eq-saturation ::eq-gamma ::eq-gamma_r ::eq-gamma_g ::eq-gamma_b ::eq-gamma_weight ::eq-eval]))

(s/def ::hue-h ::string)
(s/def ::hue-s ::string)
(s/def ::hue-H ::string)
(s/def ::hue-b ::string)
(s/def ::hue (s/keys :opt-un [::hue-h ::hue-s ::hue-H ::hue-b]))

(s/def ::lenscorrection-cx ::float)
(s/def ::lenscorrection-cy ::float)
(s/def ::lenscorrection-k1 ::float)
(s/def ::lenscorrection-k2 ::float)
(s/def ::lenscorrection-i ::int)
(s/def ::lenscorrection-fc ::string)
(s/def ::lenscorrection (s/keys :opt-un [::lenscorrection-cx ::lenscorrection-cy ::lenscorrection-k1 ::lenscorrection-k2 ::lenscorrection-i ::lenscorrection-fc]))

(s/def ::perspective-x0 ::string)
(s/def ::perspective-y0 ::string)
(s/def ::perspective-x1 ::string)
(s/def ::perspective-y1 ::string)
(s/def ::perspective-x2 ::string)
(s/def ::perspective-y2 ::string)
(s/def ::perspective-x3 ::string)
(s/def ::perspective-y3 ::string)
(s/def ::perspective-interpolation (s/nilable #{"linear" "cubic"}))
(s/def ::perspective-sense (s/nilable #{"source" "destination"}))
(s/def ::perspective-eval (s/nilable #{"init" "frame"}))
(s/def ::perspective (s/keys :opt-un [::perspective-x0 ::perspective-y0 ::perspective-x1 ::perspective-y1 ::perspective-x2 ::perspective-y2 ::perspective-x3 ::perspective-y3 ::perspective-interpolation ::perspective-sense ::perspective-eval]))

(s/def ::unsharp-luma_msize_x ::int)
(s/def ::unsharp-luma_msize_y ::int)
(s/def ::unsharp-luma_amount ::float)
(s/def ::unsharp-chroma_msize_x ::int)
(s/def ::unsharp-chroma_msize_y ::int)
(s/def ::unsharp-chroma_amount ::float)
(s/def ::unsharp-alpha_msize_x ::int)
(s/def ::unsharp-alpha_msize_y ::int)
(s/def ::unsharp-alpha_amount ::float)
(s/def ::unsharp (s/keys :opt-un [::unsharp-luma_msize_x ::unsharp-luma_msize_y ::unsharp-luma_amount ::unsharp-chroma_msize_x ::unsharp-chroma_msize_y ::unsharp-chroma_amount ::unsharp-alpha_msize_x ::unsharp-alpha_msize_y ::unsharp-alpha_amount]))

(s/def ::delogo-x ::int)
(s/def ::delogo-y ::int)
(s/def ::delogo-w ::int)
(s/def ::delogo-h ::int)
(s/def ::delogo-show (s/nilable ::boolean))
(s/def ::delogo (s/keys :opt-un [::delogo-x ::delogo-y ::delogo-w ::delogo-h ::delogo-show]))

(s/def ::deshake-x ::int)
(s/def ::deshake-y ::int)
(s/def ::deshake-w ::int)
(s/def ::deshake-h ::int)
(s/def ::deshake-rx ::int)
(s/def ::deshake-ry ::int)
(s/def ::deshake-edge (s/nilable #{"blank" "original" "clamp" "mirror"}))
(s/def ::deshake-blocksize ::int)
(s/def ::deshake-contrast ::float)
(s/def ::deshake-search ::int)
(s/def ::deshake-filename ::string)
(s/def ::deshake (s/keys :opt-un [::deshake-x ::deshake-y ::deshake-w ::deshake-h ::deshake-rx ::deshake-ry ::deshake-edge ::deshake-blocksize ::deshake-contrast ::deshake-search ::deshake-filename]))

(s/def ::deflicker-size ::int)
(s/def ::deflicker-mode (s/nilable #{"am" "gm" "hm" "qm" "cm" "pm" "median"}))
(s/def ::deflicker-bypass (s/nilable ::boolean))
(s/def ::deflicker (s/keys :opt-un [::deflicker-size ::deflicker-mode ::deflicker-bypass]))

(s/def ::yadif-mode (s/nilable #{"0" "1" "2" "3" "send_frame" "send_field" "send_frame_nospatial" "send_field_nospatial"}))
(s/def ::yadif-parity (s/nilable #{"-1" "0" "1" "auto" "tff" "bff"}))
(s/def ::yadif-deint (s/nilable #{"0" "1" "all" "interlaced"}))
(s/def ::yadif (s/keys :opt-un [::yadif-mode ::yadif-parity ::yadif-deint]))

(s/def ::bwdif-mode (s/nilable #{"0" "1" "send_frame" "send_field"}))
(s/def ::bwdif-parity (s/nilable #{"-1" "0" "1" "auto" "tff" "bff"}))
(s/def ::bwdif-deint (s/nilable #{"0" "1" "all" "interlaced"}))
(s/def ::bwdif (s/keys :opt-un [::bwdif-mode ::bwdif-parity ::bwdif-deint]))

(s/def ::noise-all_seed ::int)
(s/def ::noise-all_strength ::int)
(s/def ::noise-alls ::int)
(s/def ::noise-allf (s/nilable #{"t" "u" "p"}))
(s/def ::noise-c0_seed ::int)
(s/def ::noise-c0_strength ::int)
(s/def ::noise-c0s ::int)
(s/def ::noise-c0f (s/nilable #{"t" "u" "p"}))
(s/def ::noise-c1_seed ::int)
(s/def ::noise-c1_strength ::int)
(s/def ::noise-c1s ::int)
(s/def ::noise-c1f (s/nilable #{"t" "u" "p"}))
(s/def ::noise-c2_seed ::int)
(s/def ::noise-c2_strength ::int)
(s/def ::noise-c2s ::int)
(s/def ::noise-c2f (s/nilable #{"t" "u" "p"}))
(s/def ::noise-c3_seed ::int)
(s/def ::noise-c3_strength ::int)
(s/def ::noise-c3s ::int)
(s/def ::noise-c3f (s/nilable #{"t" "u" "p"}))
(s/def ::noise (s/keys :opt-un [::noise-all_seed ::noise-all_strength ::noise-alls ::noise-allf ::noise-c0_seed ::noise-c0_strength ::noise-c0s ::noise-c0f ::noise-c1_seed ::noise-c1_strength ::noise-c1s ::noise-c1f ::noise-c2_seed ::noise-c2_strength ::noise-c2s ::noise-c2f ::noise-c3_seed ::noise-c3_strength ::noise-c3s ::noise-c3f]))

(s/def ::vignette-angle ::string)
(s/def ::vignette-x0 ::string)
(s/def ::vignette-y0 ::string)
(s/def ::vignette-mode (s/nilable #{"forward" "backward"}))
(s/def ::vignette-eval (s/nilable #{"init" "frame"}))
(s/def ::vignette-dither (s/nilable ::boolean))
(s/def ::vignette-aspect ::string)
(s/def ::vignette (s/keys :opt-un [::vignette-angle ::vignette-x0 ::vignette-y0 ::vignette-mode ::vignette-eval ::vignette-dither ::vignette-aspect]))

(s/def ::zoompan-zoom ::string)
(s/def ::zoompan-x ::string)
(s/def ::zoompan-y ::string)
(s/def ::zoompan-d ::string)
(s/def ::zoompan-s ::string)
(s/def ::zoompan-fps ::string)
(s/def ::zoompan (s/keys :opt-un [::zoompan-zoom ::zoompan-x ::zoompan-y ::zoompan-d ::zoompan-s ::zoompan-fps]))

(s/def ::blackdetect-black_min_duration ::float)
(s/def ::blackdetect-picture_black_ratio_th ::float)
(s/def ::blackdetect-pixel_black_th ::float)
(s/def ::blackdetect (s/keys :opt-un [::blackdetect-black_min_duration ::blackdetect-picture_black_ratio_th ::blackdetect-pixel_black_th]))

(s/def ::cropdetect-limit ::float)
(s/def ::cropdetect-round ::int)
(s/def ::cropdetect-skip ::int)
(s/def ::cropdetect-reset_count ::int)
(s/def ::cropdetect-max_outliers ::int)
(s/def ::cropdetect (s/keys :opt-un [::cropdetect-limit ::cropdetect-round ::cropdetect-skip ::cropdetect-reset_count ::cropdetect-max_outliers]))

(s/def ::scenedetect-threshold ::float)
(s/def ::scenedetect-min_scene_len ::float)
(s/def ::scenedetect (s/keys :opt-un [::scenedetect-threshold ::scenedetect-min_scene_len]))

(s/def ::mpdecimate-max ::int)
(s/def ::mpdecimate-keep ::float)
(s/def ::mpdecimate-frac ::float)
(s/def ::mpdecimate-hi ::int)
(s/def ::mpdecimate-lo ::int)
(s/def ::mpdecimate (s/keys :opt-un [::mpdecimate-max ::mpdecimate-keep ::mpdecimate-frac ::mpdecimate-hi ::mpdecimate-lo]))

(s/def ::fieldmatch-order (s/nilable #{"auto" "bff" "tff"}))
(s/def ::fieldmatch-mode (s/nilable #{"pc" "pc_n" "pc_u" "pc_n_ub" "pcn" "pcn_ub"}))
(s/def ::fieldmatch-ppsrc (s/nilable ::boolean))
(s/def ::fieldmatch-field (s/nilable #{"auto" "bottom" "top"}))
(s/def ::fieldmatch-mchroma (s/nilable ::boolean))
(s/def ::fieldmatch-y0 ::int)
(s/def ::fieldmatch-y1 ::int)
(s/def ::fieldmatch-scthresh ::float)
(s/def ::fieldmatch-combmatch (s/nilable #{"none" "sc" "full"}))
(s/def ::fieldmatch-combdbg (s/nilable #{"none" "pcn" "pcnub"}))
(s/def ::fieldmatch-cthresh ::int)
(s/def ::fieldmatch-chroma (s/nilable ::boolean))
(s/def ::fieldmatch-blockx ::int)
(s/def ::fieldmatch-blocky ::int)
(s/def ::fieldmatch-combpel ::int)
(s/def ::fieldmatch (s/keys :opt-un [::fieldmatch-order ::fieldmatch-mode ::fieldmatch-ppsrc ::fieldmatch-field ::fieldmatch-mchroma ::fieldmatch-y0 ::fieldmatch-y1 ::fieldmatch-scthresh ::fieldmatch-combmatch ::fieldmatch-combdbg ::fieldmatch-cthresh ::fieldmatch-chroma ::fieldmatch-blockx ::fieldmatch-blocky ::fieldmatch-combpel]))

(s/def ::pullup-jl ::int)
(s/def ::pullup-jr ::int)
(s/def ::pullup-jt ::int)
(s/def ::pullup-jb ::int)
(s/def ::pullup-sb (s/nilable ::boolean))
(s/def ::pullup-mp (s/nilable #{"u" "p"}))
(s/def ::pullup (s/keys :opt-un [::pullup-jl ::pullup-jr ::pullup-jt ::pullup-jb ::pullup-sb ::pullup-mp]))

(s/def ::subtitles-filename ::string)
(s/def ::subtitles-original_size ::string)
(s/def ::subtitles-fontsdir ::string)
(s/def ::subtitles-alpha (s/nilable ::boolean))
(s/def ::subtitles-charenc ::string)
(s/def ::subtitles-stream_index ::int)
(s/def ::subtitles-force_style ::string)
(s/def ::subtitles (s/keys :req-un [::subtitles-filename] :opt-un [::subtitles-original_size ::subtitles-fontsdir ::subtitles-alpha ::subtitles-charenc ::subtitles-stream_index ::subtitles-force_style]))

(s/def ::ass-filename ::string)
(s/def ::ass-original_size ::string)
(s/def ::ass-fontsdir ::string)
(s/def ::ass-alpha (s/nilable ::boolean))
(s/def ::ass-shaping (s/nilable #{"auto" "simple" "complex"}))
(s/def ::ass (s/keys :req-un [::ass-filename] :opt-un [::ass-original_size ::ass-fontsdir ::ass-alpha ::ass-shaping]))

;; Audio Analysis/Visualization Filters
(s/def ::a3dscope-rate ::string)
(s/def ::a3dscope-size ::string)
(s/def ::a3dscope-fov ::float)
(s/def ::a3dscope-roll ::float)
(s/def ::a3dscope-pitch ::float)
(s/def ::a3dscope-yaw ::float)
(s/def ::a3dscope-xzoom ::float)
(s/def ::a3dscope-yzoom ::float)
(s/def ::a3dscope-zzoom ::float)
(s/def ::a3dscope-xpos ::float)
(s/def ::a3dscope-ypos ::float)
(s/def ::a3dscope-zpos ::float)
(s/def ::a3dscope-length ::int)
(s/def ::a3dscope (s/keys :opt-un [::a3dscope-rate ::a3dscope-size ::a3dscope-fov ::a3dscope-roll ::a3dscope-pitch ::a3dscope-yaw ::a3dscope-xzoom ::a3dscope-yzoom ::a3dscope-zzoom ::a3dscope-xpos ::a3dscope-ypos ::a3dscope-zpos ::a3dscope-length]))

(s/def ::bitscope-rate ::string)
(s/def ::bitscope-size ::string)
(s/def ::bitscope-colors ::string)
(s/def ::bitscope-mode (s/nilable #{"bars" "trace"}))
(s/def ::bitscope (s/keys :opt-un [::bitscope-rate ::bitscope-size ::bitscope-colors ::bitscope-mode]))

(s/def ::adrawgraph-m1 ::string)
(s/def ::adrawgraph-m2 ::string)
(s/def ::adrawgraph-m3 ::string)
(s/def ::adrawgraph-m4 ::string)
(s/def ::adrawgraph-fg1 ::string)
(s/def ::adrawgraph-fg2 ::string)
(s/def ::adrawgraph-fg3 ::string)
(s/def ::adrawgraph-fg4 ::string)
(s/def ::adrawgraph-min ::float)
(s/def ::adrawgraph-max ::float)
(s/def ::adrawgraph-bg ::string)
(s/def ::adrawgraph-mode (s/nilable #{"bar" "dot" "line"}))
(s/def ::adrawgraph-slide (s/nilable #{"frame" "replace" "scroll" "rscroll" "picture"}))
(s/def ::adrawgraph-size ::string)
(s/def ::adrawgraph-rate ::string)
(s/def ::adrawgraph (s/keys :opt-un [::adrawgraph-m1 ::adrawgraph-m2 ::adrawgraph-m3 ::adrawgraph-m4 ::adrawgraph-fg1 ::adrawgraph-fg2 ::adrawgraph-fg3 ::adrawgraph-fg4 ::adrawgraph-min ::adrawgraph-max ::adrawgraph-bg ::adrawgraph-mode ::adrawgraph-slide ::adrawgraph-size ::adrawgraph-rate]))

(s/def ::agraphmonitor-size ::string)
(s/def ::agraphmonitor-opacity ::float)
(s/def ::agraphmonitor-mode (s/nilable #{"full" "compact"}))
(s/def ::agraphmonitor-flags (s/nilable #{"queue" "frame_count_in" "frame_count_out" "pts" "time" "timebase" "format" "size" "rate"}))
(s/def ::agraphmonitor-rate ::string)
(s/def ::agraphmonitor (s/keys :opt-un [::agraphmonitor-size ::agraphmonitor-opacity ::agraphmonitor-mode ::agraphmonitor-flags ::agraphmonitor-rate]))

(s/def ::ahistogram-dmode (s/nilable #{"single" "separate"}))
(s/def ::ahistogram-rate ::string)
(s/def ::ahistogram-size ::string)
(s/def ::ahistogram-scale (s/nilable #{"log" "sqrt" "cbrt" "lin" "rlog"}))
(s/def ::ahistogram-ascale (s/nilable #{"log" "lin"}))
(s/def ::ahistogram-acount ::int)
(s/def ::ahistogram-rheight ::float)
(s/def ::ahistogram-slide (s/nilable #{"replace" "scroll"}))
(s/def ::ahistogram-hmode (s/nilable #{"abs" "sign"}))
(s/def ::ahistogram (s/keys :opt-un [::ahistogram-dmode ::ahistogram-rate ::ahistogram-size ::ahistogram-scale ::ahistogram-ascale ::ahistogram-acount ::ahistogram-rheight ::ahistogram-slide ::ahistogram-hmode]))

