(ns bioscoop.domain.spec
  (:require [clojure.spec.alpha :as s]
            [lang-utils.core :refer [seek]]))

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


(defn spec-aware-namespace-keyword [spec unqualified-kw]
  (let [spec-map (apply hash-map (rest (s/form spec)))
        opt-un-specs (get spec-map :opt-un [])]
    (seek (fn [kw] (= (name kw) (name unqualified-kw))) opt-un-specs)))
;; to process a map: (into {} (map (fn [[k v]] [(spec-aware-namespace-keyword ::scale k) v]) {:width 1920 :height 1080})) or like below
 
(defn spec-aware-namespace-map
  "Convert unnamespaced map to properly namespaced map based on spec registry.
   Looks up where each spec is actually defined and uses that namespace."
  [spec-keyword unnamespaced-map]
  (when-not (s/get-spec spec-keyword)
    (throw (ex-info "Spec not found in registry" {:spec spec-keyword})))
  
  (let [spec-form (s/form spec-keyword)]
    (if (and (sequential? spec-form) 
             (= 'clojure.spec.alpha/keys (first spec-form)))
      (let [spec-map (apply hash-map (rest spec-form))
            opt-un-specs (get spec-map :opt-un [])
            req-un-specs (get spec-map :req-un [])
            all-un-specs (concat opt-un-specs req-un-specs)
            ;; Create mapping from unqualified key to its actual qualified spec
            key-mapping (into {} 
                           (map (fn [qualified-spec-kw]
                                  (let [unqual-key (keyword (name qualified-spec-kw))]
                                    [unqual-key qualified-spec-kw]))
                                all-un-specs))]
        ;; Transform the map using the spec-derived key mapping
        (into {} 
              (map (fn [[k v]]
                     (if-let [qualified-kw (get key-mapping k)]
                       [qualified-kw v]
                       ;; Keep unmapped keys as-is (or could warn/error)
                       [k v]))
                   unnamespaced-map)))
      ;; If not a keys spec, return original map
      unnamespaced-map)))



;; Below is temporary, requires reorganization (ongoing)

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

;; hflip (horizontal flip filter)
(s/def ::hflip
  (s/keys :opt-un []))

;; vflip (vertical flip filter)
(s/def ::vflip
  (s/keys :opt-un []))

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

;; fps (fps filter)
(s/def ::fps string?)
(s/def ::start_time number?) ; already defined
(s/def ::round #{"zero" "inf" "down" "up" "near"})
(s/def ::eof_action #{"round" "pass"}) ; already defined with different values, need fps-specific

(s/def ::fps
  (s/keys :opt-un [::fps ::start_time ::round ::eof_action]))


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

;; Phase 1: Essential Audio Effects

;; Core Dynamics Processing
(s/def ::adelay-delays ::string)
(s/def ::adelay-all (s/nilable ::boolean))
(s/def ::adelay (s/keys :opt-un [::adelay-delays ::adelay-all]))

(s/def ::agate-level_in ::float)
(s/def ::agate-mode (s/nilable #{"downward" "upward"}))
(s/def ::agate-range ::float)
(s/def ::agate-threshold ::float)
(s/def ::agate-ratio ::float)
(s/def ::agate-attack ::float)
(s/def ::agate-release ::float)
(s/def ::agate-makeup ::float)
(s/def ::agate-knee ::float)
(s/def ::agate-detection (s/nilable #{"peak" "rms"}))
(s/def ::agate-link (s/nilable #{"average" "maximum"}))
(s/def ::agate (s/keys :opt-un [::agate-level_in ::agate-mode ::agate-range ::agate-threshold ::agate-ratio ::agate-attack ::agate-release ::agate-makeup ::agate-knee ::agate-detection ::agate-link]))

(s/def ::alimiter-level_in ::float)
(s/def ::alimiter-level_out ::float)
(s/def ::alimiter-limit ::float)
(s/def ::alimiter-attack ::float)
(s/def ::alimiter-release ::float)
(s/def ::alimiter-asc (s/nilable ::boolean))
(s/def ::alimiter-asc_level ::float)
(s/def ::alimiter-level (s/nilable ::boolean))
(s/def ::alimiter (s/keys :opt-un [::alimiter-level_in ::alimiter-level_out ::alimiter-limit ::alimiter-attack ::alimiter-release ::alimiter-asc ::alimiter-asc_level ::alimiter-level]))

;; Essential Frequency Filters
(s/def ::highpass-frequency ::float)
(s/def ::highpass-poles ::int)
(s/def ::highpass-width_type (s/nilable #{"h" "q" "o" "s" "k"}))
(s/def ::highpass-width ::float)
(s/def ::highpass-mix ::float)
(s/def ::highpass-channels ::string)
(s/def ::highpass-normalize (s/nilable ::boolean))
(s/def ::highpass-transform (s/nilable #{"di" "dii" "tdi" "tdii" "latt" "svf" "zdf"}))
(s/def ::highpass-precision (s/nilable #{"auto" "s16" "s32" "f32" "f64"}))
(s/def ::highpass-blocksize ::int)
(s/def ::highpass (s/keys :req-un [::highpass-frequency] :opt-un [::highpass-poles ::highpass-width_type ::highpass-width ::highpass-mix ::highpass-channels ::highpass-normalize ::highpass-transform ::highpass-precision ::highpass-blocksize]))

(s/def ::lowpass-frequency ::float)
(s/def ::lowpass-poles ::int)
(s/def ::lowpass-width_type (s/nilable #{"h" "q" "o" "s" "k"}))
(s/def ::lowpass-width ::float)
(s/def ::lowpass-mix ::float)
(s/def ::lowpass-channels ::string)
(s/def ::lowpass-normalize (s/nilable ::boolean))
(s/def ::lowpass-transform (s/nilable #{"di" "dii" "tdi" "tdii" "latt" "svf" "zdf"}))
(s/def ::lowpass-precision (s/nilable #{"auto" "s16" "s32" "f32" "f64"}))
(s/def ::lowpass-blocksize ::int)
(s/def ::lowpass (s/keys :req-un [::lowpass-frequency] :opt-un [::lowpass-poles ::lowpass-width_type ::lowpass-width ::lowpass-mix ::lowpass-channels ::lowpass-normalize ::lowpass-transform ::lowpass-precision ::lowpass-blocksize]))

(s/def ::bandpass-frequency ::float)
(s/def ::bandpass-width_type (s/nilable #{"h" "q" "o" "s" "k"}))
(s/def ::bandpass-width ::float)
(s/def ::bandpass-csg (s/nilable ::boolean))
(s/def ::bandpass-mix ::float)
(s/def ::bandpass-channels ::string)
(s/def ::bandpass-normalize (s/nilable ::boolean))
(s/def ::bandpass-transform (s/nilable #{"di" "dii" "tdi" "tdii" "latt" "svf" "zdf"}))
(s/def ::bandpass-precision (s/nilable #{"auto" "s16" "s32" "f32" "f64"}))
(s/def ::bandpass-blocksize ::int)
(s/def ::bandpass (s/keys :req-un [::bandpass-frequency] :opt-un [::bandpass-width_type ::bandpass-width ::bandpass-csg ::bandpass-mix ::bandpass-channels ::bandpass-normalize ::bandpass-transform ::bandpass-precision ::bandpass-blocksize]))

;; Standard Modulation Effects
(s/def ::aphaser-in_gain ::float)
(s/def ::aphaser-out_gain ::float)
(s/def ::aphaser-delay ::float)
(s/def ::aphaser-decay ::float)
(s/def ::aphaser-speed ::float)
(s/def ::aphaser-type (s/nilable #{"triangular" "sinusoidal"}))
(s/def ::aphaser (s/keys :opt-un [::aphaser-in_gain ::aphaser-out_gain ::aphaser-delay ::aphaser-decay ::aphaser-speed ::aphaser-type]))

(s/def ::chorus-in_gain ::float)
(s/def ::chorus-out_gain ::float)
(s/def ::chorus-delays ::string)
(s/def ::chorus-decays ::string)
(s/def ::chorus-speeds ::string)
(s/def ::chorus-depths ::string)
(s/def ::chorus (s/keys :opt-un [::chorus-in_gain ::chorus-out_gain ::chorus-delays ::chorus-decays ::chorus-speeds ::chorus-depths]))

(s/def ::flanger-delay ::float)
(s/def ::flanger-depth ::float)
(s/def ::flanger-regen ::float)
(s/def ::flanger-width ::float)
(s/def ::flanger-speed ::float)
(s/def ::flanger-shape (s/nilable #{"triangular" "sinusoidal"}))
(s/def ::flanger-phase ::float)
(s/def ::flanger-interp (s/nilable #{"linear" "quadratic"}))
(s/def ::flanger (s/keys :opt-un [::flanger-delay ::flanger-depth ::flanger-regen ::flanger-width ::flanger-speed ::flanger-shape ::flanger-phase ::flanger-interp]))

;; Analysis Tools
(s/def ::astats-length ::float)
(s/def ::astats-metadata (s/nilable ::boolean))
(s/def ::astats-reset ::int)
(s/def ::astats-measure_perchannel (s/nilable #{"none" "all" "Bit_depth" "Crest_factor" "DC_offset" "Dynamic_range" "Entropy" "Flat_factor" "Max_difference" "Max_level" "Mean_difference" "Min_difference" "Min_level" "Noise_floor" "Noise_floor_count" "Number_of_Infs" "Number_of_NaNs" "Number_of_denormals" "Peak_count" "Peak_level" "RMS_difference" "RMS_level" "RMS_peak" "RMS_trough" "Zero_crossings" "Zero_crossings_rate"}))
(s/def ::astats-measure_overall (s/nilable #{"none" "all" "Bit_depth" "Crest_factor" "DC_offset" "Dynamic_range" "Entropy" "Flat_factor" "Max_difference" "Max_level" "Mean_difference" "Min_difference" "Min_level" "Noise_floor" "Noise_floor_count" "Number_of_Infs" "Number_of_NaNs" "Number_of_denormals" "Peak_count" "Peak_level" "RMS_difference" "RMS_level" "RMS_peak" "RMS_trough" "Zero_crossings" "Zero_crossings_rate"}))
(s/def ::astats (s/keys :opt-un [::astats-length ::astats-metadata ::astats-reset ::astats-measure_perchannel ::astats-measure_overall]))

(s/def ::volumedetect-high ::float)
(s/def ::volumedetect-low ::float)
(s/def ::volumedetect (s/keys :opt-un [::volumedetect-high ::volumedetect-low]))

(s/def ::silencedetect-noise ::float)
(s/def ::silencedetect-duration ::float)
(s/def ::silencedetect-mono (s/nilable ::boolean))
(s/def ::silencedetect (s/keys :opt-un [::silencedetect-noise ::silencedetect-duration ::silencedetect-mono]))

;; Phase 2: Professional Video Processing

;; Color Grading
(s/def ::colorbalance-rs ::float)
(s/def ::colorbalance-gs ::float)
(s/def ::colorbalance-bs ::float)
(s/def ::colorbalance-rm ::float)
(s/def ::colorbalance-gm ::float)
(s/def ::colorbalance-bm ::float)
(s/def ::colorbalance-rh ::float)
(s/def ::colorbalance-gh ::float)
(s/def ::colorbalance-bh ::float)
(s/def ::colorbalance-pl (s/nilable ::boolean))
(s/def ::colorbalance (s/keys :opt-un [::colorbalance-rs ::colorbalance-gs ::colorbalance-bs ::colorbalance-rm ::colorbalance-gm ::colorbalance-bm ::colorbalance-rh ::colorbalance-gh ::colorbalance-bh ::colorbalance-pl]))

(s/def ::colorspace-all (s/nilable #{"bt470m" "bt470bg" "bt601-6-525" "bt601-6-625" "bt709" "smpte170m" "smpte240m" "bt2020"}))
(s/def ::colorspace-space (s/nilable #{"bt470m" "bt470bg" "bt601-6-525" "bt601-6-625" "bt709" "smpte170m" "smpte240m" "bt2020"}))
(s/def ::colorspace-range (s/nilable #{"tv" "mpeg" "pc" "jpeg"}))
(s/def ::colorspace-primaries (s/nilable #{"bt470m" "bt470bg" "bt601-6-525" "bt601-6-625" "bt709" "smpte170m" "smpte240m" "bt2020" "film" "smpte431" "smpte432" "jedec-p22"}))
(s/def ::colorspace-trc (s/nilable #{"bt709" "bt470m" "bt470bg" "gamma22" "gamma28" "smpte170m" "smpte240m" "linear" "log" "log-sqrt" "iec61966-2-4" "bt1361e" "iec61966-2-1" "bt2020-10" "bt2020-12" "smpte2084" "smpte428" "arib-std-b67"}))
(s/def ::colorspace-format (s/nilable #{"yuv420p" "yuv422p" "yuv444p" "yuv420p10le" "yuv422p10le" "yuv444p10le" "yuv420p12le" "yuv422p12le" "yuv444p12le"}))
(s/def ::colorspace-fast (s/nilable ::boolean))
(s/def ::colorspace-dither (s/nilable #{"none" "fsb"}))
(s/def ::colorspace-wpadapt (s/nilable #{"bradford" "vonkries" "identity"}))
(s/def ::colorspace-iall (s/nilable #{"bt470m" "bt470bg" "bt601-6-525" "bt601-6-625" "bt709" "smpte170m" "smpte240m" "bt2020"}))
(s/def ::colorspace-ispace (s/nilable #{"bt470m" "bt470bg" "bt601-6-525" "bt601-6-625" "bt709" "smpte170m" "smpte240m" "bt2020"}))
(s/def ::colorspace-irange (s/nilable #{"tv" "mpeg" "pc" "jpeg"}))
(s/def ::colorspace-iprimaries (s/nilable #{"bt470m" "bt470bg" "bt601-6-525" "bt601-6-625" "bt709" "smpte170m" "smpte240m" "bt2020" "film" "smpte431" "smpte432" "jedec-p22"}))
(s/def ::colorspace-itrc (s/nilable #{"bt709" "bt470m" "bt470bg" "gamma22" "gamma28" "smpte170m" "smpte240m" "linear" "log" "log-sqrt" "iec61966-2-4" "bt1361e" "iec61966-2-1" "bt2020-10" "bt2020-12" "smpte2084" "smpte428" "arib-std-b67"}))
(s/def ::colorspace (s/keys :opt-un [::colorspace-all ::colorspace-space ::colorspace-range ::colorspace-primaries ::colorspace-trc ::colorspace-format ::colorspace-fast ::colorspace-dither ::colorspace-wpadapt ::colorspace-iall ::colorspace-ispace ::colorspace-irange ::colorspace-iprimaries ::colorspace-itrc]))

(s/def ::lut-c0 ::string)
(s/def ::lut-c1 ::string)
(s/def ::lut-c2 ::string)
(s/def ::lut-c3 ::string)
(s/def ::lut-r ::string)
(s/def ::lut-g ::string)
(s/def ::lut-b ::string)
(s/def ::lut-a ::string)
(s/def ::lut-y ::string)
(s/def ::lut-u ::string)
(s/def ::lut-v ::string)
(s/def ::lut (s/keys :opt-un [::lut-c0 ::lut-c1 ::lut-c2 ::lut-c3 ::lut-r ::lut-g ::lut-b ::lut-a ::lut-y ::lut-u ::lut-v]))

(s/def ::lut3d-file ::string)
(s/def ::lut3d-clut ::int)
(s/def ::lut3d-interp (s/nilable #{"nearest" "trilinear" "tetrahedral"}))
(s/def ::lut3d (s/keys :opt-un [::lut3d-file ::lut3d-clut ::lut3d-interp]))

;; Stabilization
(s/def ::vidstabdetect-result ::string)
(s/def ::vidstabdetect-shakiness ::int)
(s/def ::vidstabdetect-accuracy ::int)
(s/def ::vidstabdetect-stepsize ::int)
(s/def ::vidstabdetect-mincontrast ::float)
(s/def ::vidstabdetect-tripod ::int)
(s/def ::vidstabdetect-show ::int)
(s/def ::vidstabdetect (s/keys :req-un [::vidstabdetect-result] :opt-un [::vidstabdetect-shakiness ::vidstabdetect-accuracy ::vidstabdetect-stepsize ::vidstabdetect-mincontrast ::vidstabdetect-tripod ::vidstabdetect-show]))

(s/def ::vidstabtransform-input ::string)
(s/def ::vidstabtransform-smoothing ::int)
(s/def ::vidstabtransform-optalgo (s/nilable #{"opt" "gauss" "avg"}))
(s/def ::vidstabtransform-maxshift ::int)
(s/def ::vidstabtransform-maxangle ::float)
(s/def ::vidstabtransform-crop (s/nilable #{"keep" "black"}))
(s/def ::vidstabtransform-invert ::int)
(s/def ::vidstabtransform-relative ::int)
(s/def ::vidstabtransform-zoom ::float)
(s/def ::vidstabtransform-optzoom ::int)
(s/def ::vidstabtransform-zoomspeed ::float)
(s/def ::vidstabtransform-interpol (s/nilable #{"no" "linear" "bilinear" "bicubic"}))
(s/def ::vidstabtransform-tripod ::int)
(s/def ::vidstabtransform-debug ::int)
(s/def ::vidstabtransform (s/keys :req-un [::vidstabtransform-input] :opt-un [::vidstabtransform-smoothing ::vidstabtransform-optalgo ::vidstabtransform-maxshift ::vidstabtransform-maxangle ::vidstabtransform-crop ::vidstabtransform-invert ::vidstabtransform-relative ::vidstabtransform-zoom ::vidstabtransform-optzoom ::vidstabtransform-zoomspeed ::vidstabtransform-interpol ::vidstabtransform-tripod ::vidstabtransform-debug]))

;; Motion Interpolation
(s/def ::minterpolate-fps ::string)
(s/def ::minterpolate-mi_mode (s/nilable #{"dup" "blend" "mci"}))
(s/def ::minterpolate-mc_mode (s/nilable #{"obmc" "aobmc"}))
(s/def ::minterpolate-me_mode (s/nilable #{"bidir" "bilat"}))
(s/def ::minterpolate-me (s/nilable #{"esa" "tss" "tdls" "ntss" "fss" "ds" "hexbs" "epzs" "umh"}))
(s/def ::minterpolate-mb_size ::int)
(s/def ::minterpolate-search_param ::int)
(s/def ::minterpolate-vsbmc ::int)
(s/def ::minterpolate-scd (s/nilable #{"none" "fdiff"}))
(s/def ::minterpolate-scd_threshold ::float)
(s/def ::minterpolate (s/keys :opt-un [::minterpolate-fps ::minterpolate-mi_mode ::minterpolate-mc_mode ::minterpolate-me_mode ::minterpolate-me ::minterpolate-mb_size ::minterpolate-search_param ::minterpolate-vsbmc ::minterpolate-scd ::minterpolate-scd_threshold]))

(s/def ::framerate-fps ::string)
(s/def ::framerate-interp_start ::int)
(s/def ::framerate-interp_end ::int)
(s/def ::framerate-scene ::float)
(s/def ::framerate-flags (s/nilable #{"scene_change_detect" "scd"}))
(s/def ::framerate (s/keys :opt-un [::framerate-fps ::framerate-interp_start ::framerate-interp_end ::framerate-scene ::framerate-flags]))

;; HDR Processing
(s/def ::tonemap-tonemap (s/nilable #{"none" "linear" "gamma" "clip" "reinhard" "hable" "mobius"}))
(s/def ::tonemap-param ::float)
(s/def ::tonemap-desat ::float)
(s/def ::tonemap-peak ::float)
(s/def ::tonemap (s/keys :opt-un [::tonemap-tonemap ::tonemap-param ::tonemap-desat ::tonemap-peak]))

;; Phase 3: Broadcast & Format Support

;; Broadcast Standards
(s/def ::telecine-pattern ::string)
(s/def ::telecine-start_frame ::int)
(s/def ::telecine (s/keys :opt-un [::telecine-pattern ::telecine-start_frame]))

(s/def ::fieldorder-order (s/nilable #{"tff" "bff"}))
(s/def ::fieldorder (s/keys :opt-un [::fieldorder-order]))

(s/def ::interlace-scan (s/nilable #{"tff" "bff"}))
(s/def ::interlace-lowpass (s/nilable #{"off" "linear" "complex"}))
(s/def ::interlace (s/keys :opt-un [::interlace-scan ::interlace-lowpass]))

(s/def ::tinterlace-mode (s/nilable #{"merge" "drop_even" "drop_odd" "pad" "interleave_top" "interleave_bottom" "interlacex2" "mergex2"}))
(s/def ::tinterlace-flags (s/nilable #{"low_pass_filter" "complex_filter" "exact_tb" "bypass_il"}))
(s/def ::tinterlace (s/keys :opt-un [::tinterlace-mode ::tinterlace-flags]))

(s/def ::separatefields (s/keys :opt-un []))

(s/def ::weave (s/keys :opt-un []))

(s/def ::doubleweave (s/keys :opt-un []))

(s/def ::framestep-step ::int)
(s/def ::framestep (s/keys :opt-un [::framestep-step]))

(s/def ::decimate-cycle ::int)
(s/def ::decimate-dupthresh ::float)
(s/def ::decimate-scthresh ::float)
(s/def ::decimate-blockx ::int)
(s/def ::decimate-blocky ::int)
(s/def ::decimate-ppsrc (s/nilable ::boolean))
(s/def ::decimate-chroma (s/nilable ::boolean))
(s/def ::decimate-mixed (s/nilable ::boolean))
(s/def ::decimate (s/keys :opt-un [::decimate-cycle ::decimate-dupthresh ::decimate-scthresh ::decimate-blockx ::decimate-blocky ::decimate-ppsrc ::decimate-chroma ::decimate-mixed]))

(s/def ::showinfo-checksum (s/nilable ::boolean))
(s/def ::showinfo (s/keys :opt-un [::showinfo-checksum]))

(s/def ::signalstats-stat (s/nilable #{"tout" "vrep" "brng"}))
(s/def ::signalstats-out (s/nilable #{"tout" "vrep" "brng"}))
(s/def ::signalstats-color (s/nilable #{"red" "green" "blue" "yellow"}))
(s/def ::signalstats (s/keys :opt-un [::signalstats-stat ::signalstats-out ::signalstats-color]))

(s/def ::idet-intl_thres ::float)
(s/def ::idet-prog_thres ::float)
(s/def ::idet-rep_thres ::float)
(s/def ::idet-half_life ::float)
(s/def ::idet-analyze_interlaced_flag ::int)
(s/def ::idet (s/keys :opt-un [::idet-intl_thres ::idet-prog_thres ::idet-rep_thres ::idet-half_life ::idet-analyze_interlaced_flag]))

;; Format Conversion & Hardware Acceleration
(s/def ::swscale-flags (s/nilable #{"fast_bilinear" "bilinear" "bicubic" "experimental" "neighbor" "area" "bicublin" "gauss" "sinc" "lanczos" "spline"}))
(s/def ::swscale-srcw ::int)
(s/def ::swscale-srch ::int)
(s/def ::swscale-dstw ::int)
(s/def ::swscale-dsth ::int)
(s/def ::swscale-src_format ::string)
(s/def ::swscale-dst_format ::string)
(s/def ::swscale-src_range (s/nilable #{"0" "1"}))
(s/def ::swscale-dst_range (s/nilable #{"0" "1"}))
(s/def ::swscale-param0 ::float)
(s/def ::swscale-param1 ::float)
(s/def ::swscale (s/keys :opt-un [::swscale-flags ::swscale-srcw ::swscale-srch ::swscale-dstw ::swscale-dsth ::swscale-src_format ::swscale-dst_format ::swscale-src_range ::swscale-dst_range ::swscale-param0 ::swscale-param1]))

(s/def ::zscale-width ::int)
(s/def ::zscale-height ::int)
(s/def ::zscale-size ::string)
(s/def ::zscale-dither (s/nilable #{"none" "ordered" "random" "error_diffusion"}))
(s/def ::zscale-filter (s/nilable #{"point" "bilinear" "bicubic" "spline16" "spline36" "lanczos"}))
(s/def ::zscale-colorspace (s/nilable #{"input" "709" "unspec" "170m" "240m" "2020_ncl" "2020_cl"}))
(s/def ::zscale-transfer (s/nilable #{"input" "709" "unspec" "601" "linear" "2020_10" "2020_12" "smpte2084" "arib-std-b67"}))
(s/def ::zscale-primaries (s/nilable #{"input" "709" "unspec" "170m" "240m" "2020" "unknown" "film" "smpte428" "smpte431" "smpte432" "jedec-p22"}))
(s/def ::zscale-range (s/nilable #{"input" "tv" "pc"}))
(s/def ::zscale-chromal (s/nilable #{"input" "left" "center" "topleft" "top" "bottomleft" "bottom"}))
(s/def ::zscale-colorspace_in (s/nilable #{"709" "unspec" "170m" "240m" "2020_ncl" "2020_cl"}))
(s/def ::zscale-transfer_in (s/nilable #{"709" "unspec" "601" "linear" "2020_10" "2020_12" "smpte2084" "arib-std-b67"}))
(s/def ::zscale-primaries_in (s/nilable #{"709" "unspec" "170m" "240m" "2020" "unknown" "film" "smpte428" "smpte431" "smpte432" "jedec-p22"}))
(s/def ::zscale-range_in (s/nilable #{"tv" "pc"}))
(s/def ::zscale-chromal_in (s/nilable #{"left" "center" "topleft" "top" "bottomleft" "bottom"}))
(s/def ::zscale-npl ::float)
(s/def ::zscale (s/keys :opt-un [::zscale-width ::zscale-height ::zscale-size ::zscale-dither ::zscale-filter ::zscale-colorspace ::zscale-transfer ::zscale-primaries ::zscale-range ::zscale-chromal ::zscale-colorspace_in ::zscale-transfer_in ::zscale-primaries_in ::zscale-range_in ::zscale-chromal_in ::zscale-npl]))

(s/def ::hwupload-derive_device ::string)
(s/def ::hwupload (s/keys :opt-un [::hwupload-derive_device]))

(s/def ::hwdownload (s/keys :opt-un []))

(s/def ::hwmap-derive_device ::string)
(s/def ::hwmap-reverse (s/nilable ::boolean))
(s/def ::hwmap (s/keys :opt-un [::hwmap-derive_device ::hwmap-reverse]))

;; Phase 4: Specialized Effects

;; Artistic Effects
(s/def ::edgedetect-low ::float)
(s/def ::edgedetect-high ::float)
(s/def ::edgedetect-mode (s/nilable #{"wires" "colormix" "canny"}))
(s/def ::edgedetect-planes ::int)
(s/def ::edgedetect (s/keys :opt-un [::edgedetect-low ::edgedetect-high ::edgedetect-mode ::edgedetect-planes]))

(s/def ::emboss-strength ::float)
(s/def ::emboss-bias ::float)
(s/def ::emboss (s/keys :opt-un [::emboss-strength ::emboss-bias]))

(s/def ::sobel-planes ::int)
(s/def ::sobel-scale ::float)
(s/def ::sobel-delta ::float)
(s/def ::sobel (s/keys :opt-un [::sobel-planes ::sobel-scale ::sobel-delta]))

(s/def ::prewitt-planes ::int)
(s/def ::prewitt-scale ::float)
(s/def ::prewitt-delta ::float)
(s/def ::prewitt (s/keys :opt-un [::prewitt-planes ::prewitt-scale ::prewitt-delta]))

(s/def ::roberts-planes ::int)
(s/def ::roberts-scale ::float)
(s/def ::roberts-delta ::float)
(s/def ::roberts (s/keys :opt-un [::roberts-planes ::roberts-scale ::roberts-delta]))

(s/def ::kirsch-planes ::int)
(s/def ::kirsch-scale ::float)
(s/def ::kirsch-delta ::float)
(s/def ::kirsch (s/keys :opt-un [::kirsch-planes ::kirsch-scale ::kirsch-delta]))

(s/def ::scharr-planes ::int)
(s/def ::scharr-scale ::float)
(s/def ::scharr-delta ::float)
(s/def ::scharr (s/keys :opt-un [::scharr-planes ::scharr-scale ::scharr-delta]))

(s/def ::bilateral-sigmaS ::float)
(s/def ::bilateral-sigmaR ::float)
(s/def ::bilateral-planes ::int)
(s/def ::bilateral (s/keys :opt-un [::bilateral-sigmaS ::bilateral-sigmaR ::bilateral-planes]))

;; Graphics Overlays
(s/def ::drawbox-x ::string)
(s/def ::drawbox-y ::string)
(s/def ::drawbox-width ::string)
(s/def ::drawbox-height ::string)
(s/def ::drawbox-color ::string)
(s/def ::drawbox-thickness ::string)
(s/def ::drawbox-replace (s/nilable ::boolean))
(s/def ::drawbox (s/keys :opt-un [::drawbox-x ::drawbox-y ::drawbox-width ::drawbox-height ::drawbox-color ::drawbox-thickness ::drawbox-replace]))

(s/def ::drawgrid-x ::string)
(s/def ::drawgrid-y ::string)
(s/def ::drawgrid-width ::string)
(s/def ::drawgrid-height ::string)
(s/def ::drawgrid-color ::string)
(s/def ::drawgrid-thickness ::string)
(s/def ::drawgrid-replace (s/nilable ::boolean))
(s/def ::drawgrid (s/keys :opt-un [::drawgrid-x ::drawgrid-y ::drawgrid-width ::drawgrid-height ::drawgrid-color ::drawgrid-thickness ::drawgrid-replace]))

(s/def ::fillborders-left ::int)
(s/def ::fillborders-right ::int)
(s/def ::fillborders-top ::int)
(s/def ::fillborders-bottom ::int)
(s/def ::fillborders-mode (s/nilable #{"smear" "mirror" "fixed" "reflect" "wrap" "fade" "margins"}))
(s/def ::fillborders-color ::string)
(s/def ::fillborders (s/keys :opt-un [::fillborders-left ::fillborders-right ::fillborders-top ::fillborders-bottom ::fillborders-mode ::fillborders-color]))

(s/def ::geq-lum_expr ::string)
(s/def ::geq-cb_expr ::string)
(s/def ::geq-cr_expr ::string)
(s/def ::geq-alpha_expr ::string)
(s/def ::geq-red_expr ::string)
(s/def ::geq-green_expr ::string)
(s/def ::geq-blue_expr ::string)
(s/def ::geq-interpolation (s/nilable #{"nearest" "bilinear"}))
(s/def ::geq (s/keys :opt-un [::geq-lum_expr ::geq-cb_expr ::geq-cr_expr ::geq-alpha_expr ::geq-red_expr ::geq-green_expr ::geq-blue_expr ::geq-interpolation]))

(s/def ::lutrgb-r ::string)
(s/def ::lutrgb-g ::string)
(s/def ::lutrgb-b ::string)
(s/def ::lutrgb (s/keys :opt-un [::lutrgb-r ::lutrgb-g ::lutrgb-b]))

(s/def ::lutyuv-y ::string)
(s/def ::lutyuv-u ::string)
(s/def ::lutyuv-v ::string)
(s/def ::lutyuv (s/keys :opt-un [::lutyuv-y ::lutyuv-u ::lutyuv-v]))

;; Scopes and Analysis
(s/def ::histogram-level_height ::int)
(s/def ::histogram-scale_height ::int)
(s/def ::histogram-display_mode (s/nilable #{"overlay" "parade" "stack"}))
(s/def ::histogram-levels_mode (s/nilable #{"linear" "logarithmic"}))
(s/def ::histogram-components ::int)
(s/def ::histogram-fgopacity ::float)
(s/def ::histogram-bgopacity ::float)
(s/def ::histogram (s/keys :opt-un [::histogram-level_height ::histogram-scale_height ::histogram-display_mode ::histogram-levels_mode ::histogram-components ::histogram-fgopacity ::histogram-bgopacity]))

(s/def ::waveform-mode (s/nilable #{"row" "column"}))
(s/def ::waveform-intensity ::float)
(s/def ::waveform-mirror (s/nilable ::boolean))
(s/def ::waveform-display (s/nilable #{"overlay" "stack" "parade"}))
(s/def ::waveform-components ::int)
(s/def ::waveform-envelope (s/nilable #{"none" "instant" "peak" "peak+instant"}))
(s/def ::waveform-filter (s/nilable #{"lowpass" "flat" "aflat" "chroma" "color" "acolor"}))
(s/def ::waveform-graticule (s/nilable #{"none" "green" "orange" "invert"}))
(s/def ::waveform-opacity ::float)
(s/def ::waveform-flags (s/nilable #{"numbers" "dots"}))
(s/def ::waveform-scale (s/nilable #{"digital" "millivolts" "ire"}))
(s/def ::waveform-bgopacity ::float)
(s/def ::waveform (s/keys :opt-un [::waveform-mode ::waveform-intensity ::waveform-mirror ::waveform-display ::waveform-components ::waveform-envelope ::waveform-filter ::waveform-graticule ::waveform-opacity ::waveform-flags ::waveform-scale ::waveform-bgopacity]))

(s/def ::vectorscope-mode (s/nilable #{"gray" "color" "color2" "color3" "color4" "color5"}))
(s/def ::vectorscope-x ::int)
(s/def ::vectorscope-y ::int)
(s/def ::vectorscope-intensity ::float)
(s/def ::vectorscope-envelope (s/nilable #{"none" "instant" "peak" "peak+instant"}))
(s/def ::vectorscope-graticule (s/nilable #{"none" "green" "color" "invert"}))
(s/def ::vectorscope-opacity ::float)
(s/def ::vectorscope-flags (s/nilable #{"white" "black" "name"}))
(s/def ::vectorscope-bgopacity ::float)
(s/def ::vectorscope-colorspace (s/nilable #{"auto" "601" "709" "2020"}))
(s/def ::vectorscope (s/keys :opt-un [::vectorscope-mode ::vectorscope-x ::vectorscope-y ::vectorscope-intensity ::vectorscope-envelope ::vectorscope-graticule ::vectorscope-opacity ::vectorscope-flags ::vectorscope-bgopacity ::vectorscope-colorspace]))

(s/def ::datascope-size ::string)
(s/def ::datascope-x ::int)
(s/def ::datascope-y ::int)
(s/def ::datascope-mode (s/nilable #{"mono" "color" "color2"}))
(s/def ::datascope-axis (s/nilable ::boolean))
(s/def ::datascope-opacity ::float)
(s/def ::datascope-format (s/nilable #{"hex" "dec"}))
(s/def ::datascope-components ::int)
(s/def ::datascope (s/keys :opt-un [::datascope-size ::datascope-x ::datascope-y ::datascope-mode ::datascope-axis ::datascope-opacity ::datascope-format ::datascope-components]))

(s/def ::pixscope-x ::float)
(s/def ::pixscope-y ::float)
(s/def ::pixscope-w ::int)
(s/def ::pixscope-h ::int)
(s/def ::pixscope-o ::float)
(s/def ::pixscope-wx ::float)
(s/def ::pixscope-wy ::float)
(s/def ::pixscope (s/keys :opt-un [::pixscope-x ::pixscope-y ::pixscope-w ::pixscope-h ::pixscope-o ::pixscope-wx ::pixscope-wy]))

;; Phase 5: Advanced Audio Processing

;; Essential Temporal Audio Processing
(s/def ::atempo-tempo ::float)
(s/def ::atempo (s/keys :req-un [::atempo-tempo]))

(s/def ::asetrate-sample_rate ::int)
(s/def ::asetrate (s/keys :req-un [::asetrate-sample_rate]))

(s/def ::apad-packet_size ::int)
(s/def ::apad-pad_len ::int)
(s/def ::apad-whole_len ::int)
(s/def ::apad-pad_dur ::string)
(s/def ::apad-whole_dur ::string)
(s/def ::apad (s/keys :opt-un [::apad-packet_size ::apad-pad_len ::apad-whole_len ::apad-pad_dur ::apad-whole_dur]))

(s/def ::atrim-start ::string)
(s/def ::atrim-end ::string)
(s/def ::atrim-start_pts ::string)
(s/def ::atrim-end_pts ::string)
(s/def ::atrim-duration ::string)
(s/def ::atrim-start_sample ::int)
(s/def ::atrim-end_sample ::int)
(s/def ::atrim (s/keys :opt-un [::atrim-start ::atrim-end ::atrim-start_pts ::atrim-end_pts ::atrim-duration ::atrim-start_sample ::atrim-end_sample]))

(s/def ::areverse (s/keys :opt-un []))

;; Professional Audio Dynamics
(s/def ::compand-attacks ::string)
(s/def ::compand-decays ::string)
(s/def ::compand-points ::string)
(s/def ::compand-soft-knee ::float)
(s/def ::compand-gain ::float)
(s/def ::compand-volume ::float)
(s/def ::compand-delay ::float)
(s/def ::compand (s/keys :opt-un [::compand-attacks ::compand-decays ::compand-points ::compand-soft-knee ::compand-gain ::compand-volume ::compand-delay]))

(s/def ::loudnorm-I ::float)
(s/def ::loudnorm-LRA ::float)
(s/def ::loudnorm-TP ::float)
(s/def ::loudnorm-measured_I ::float)
(s/def ::loudnorm-measured_LRA ::float)
(s/def ::loudnorm-measured_TP ::float)
(s/def ::loudnorm-measured_thresh ::float)
(s/def ::loudnorm-offset ::float)
(s/def ::loudnorm-linear (s/nilable ::boolean))
(s/def ::loudnorm-dual_mono (s/nilable ::boolean))
(s/def ::loudnorm-print_format (s/nilable #{"none" "json" "summary"}))
(s/def ::loudnorm (s/keys :opt-un [::loudnorm-I ::loudnorm-LRA ::loudnorm-TP ::loudnorm-measured_I ::loudnorm-measured_LRA ::loudnorm-measured_TP ::loudnorm-measured_thresh ::loudnorm-offset ::loudnorm-linear ::loudnorm-dual_mono ::loudnorm-print_format]))

(s/def ::dynaudnorm-framelen ::int)
(s/def ::dynaudnorm-gausssize ::int)
(s/def ::dynaudnorm-peak ::float)
(s/def ::dynaudnorm-maxgain ::float)
(s/def ::dynaudnorm-targetrms ::float)
(s/def ::dynaudnorm-compressfactor ::float)
(s/def ::dynaudnorm-channelcoupling (s/nilable ::boolean))
(s/def ::dynaudnorm-dc (s/nilable ::boolean))
(s/def ::dynaudnorm-altboundary (s/nilable ::boolean))
(s/def ::dynaudnorm-compress (s/nilable ::boolean))
(s/def ::dynaudnorm-threshold ::float)
(s/def ::dynaudnorm (s/keys :opt-un [::dynaudnorm-framelen ::dynaudnorm-gausssize ::dynaudnorm-peak ::dynaudnorm-maxgain ::dynaudnorm-targetrms ::dynaudnorm-compressfactor ::dynaudnorm-channelcoupling ::dynaudnorm-dc ::dynaudnorm-altboundary ::dynaudnorm-compress ::dynaudnorm-threshold]))

(s/def ::sidechaincompress-level_in ::float)
(s/def ::sidechaincompress-mode (s/nilable #{"downward" "upward"}))
(s/def ::sidechaincompress-threshold ::float)
(s/def ::sidechaincompress-ratio ::float)
(s/def ::sidechaincompress-attack ::float)
(s/def ::sidechaincompress-release ::float)
(s/def ::sidechaincompress-makeup ::float)
(s/def ::sidechaincompress-knee ::float)
(s/def ::sidechaincompress-link (s/nilable #{"average" "maximum"}))
(s/def ::sidechaincompress-detection (s/nilable #{"peak" "rms"}))
(s/def ::sidechaincompress-level_sc ::float)
(s/def ::sidechaincompress-mix ::float)
(s/def ::sidechaincompress (s/keys :opt-un [::sidechaincompress-level_in ::sidechaincompress-mode ::sidechaincompress-threshold ::sidechaincompress-ratio ::sidechaincompress-attack ::sidechaincompress-release ::sidechaincompress-makeup ::sidechaincompress-knee ::sidechaincompress-link ::sidechaincompress-detection ::sidechaincompress-level_sc ::sidechaincompress-mix]))

(s/def ::sidechaingate-level_in ::float)
(s/def ::sidechaingate-mode (s/nilable #{"downward" "upward"}))
(s/def ::sidechaingate-range ::float)
(s/def ::sidechaingate-threshold ::float)
(s/def ::sidechaingate-ratio ::float)
(s/def ::sidechaingate-attack ::float)
(s/def ::sidechaingate-release ::float)
(s/def ::sidechaingate-makeup ::float)
(s/def ::sidechaingate-knee ::float)
(s/def ::sidechaingate-detection (s/nilable #{"peak" "rms"}))
(s/def ::sidechaingate-link (s/nilable #{"average" "maximum"}))
(s/def ::sidechaingate-level_sc ::float)
(s/def ::sidechaingate (s/keys :opt-un [::sidechaingate-level_in ::sidechaingate-mode ::sidechaingate-range ::sidechaingate-threshold ::sidechaingate-ratio ::sidechaingate-attack ::sidechaingate-release ::sidechaingate-makeup ::sidechaingate-knee ::sidechaingate-detection ::sidechaingate-link ::sidechaingate-level_sc]))

;; Audio Restoration Tools
(s/def ::afftdn-nr ::float)
(s/def ::afftdn-nf ::float)
(s/def ::afftdn-nt (s/nilable #{"w" "s"}))
(s/def ::afftdn-bn (s/nilable ::boolean))
(s/def ::afftdn-rf ::float)
(s/def ::afftdn-tn (s/nilable ::boolean))
(s/def ::afftdn-tr (s/nilable ::boolean))
(s/def ::afftdn-om (s/nilable #{"i" "o" "n"}))
(s/def ::afftdn (s/keys :opt-un [::afftdn-nr ::afftdn-nf ::afftdn-nt ::afftdn-bn ::afftdn-rf ::afftdn-tn ::afftdn-tr ::afftdn-om]))

(s/def ::adeclick-w ::float)
(s/def ::adeclick-o ::float)
(s/def ::adeclick-a (s/nilable ::boolean))
(s/def ::adeclick-f (s/nilable ::boolean))
(s/def ::adeclick-m (s/nilable #{"a" "p"}))
(s/def ::adeclick (s/keys :opt-un [::adeclick-w ::adeclick-o ::adeclick-a ::adeclick-f ::adeclick-m]))

(s/def ::adeclip-w ::float)
(s/def ::adeclip-o ::float)
(s/def ::adeclip-a (s/nilable ::boolean))
(s/def ::adeclip-f (s/nilable ::boolean))
(s/def ::adeclip-m (s/nilable #{"a" "p"}))
(s/def ::adeclip-h (s/nilable ::boolean))
(s/def ::adeclip-s ::float)
(s/def ::adeclip-p ::float)
(s/def ::adeclip (s/keys :opt-un [::adeclip-w ::adeclip-o ::adeclip-a ::adeclip-f ::adeclip-m ::adeclip-h ::adeclip-s ::adeclip-p]))

(s/def ::silenceremove-start_periods ::int)
(s/def ::silenceremove-start_duration ::float)
(s/def ::silenceremove-start_threshold ::float)
(s/def ::silenceremove-start_silence ::float)
(s/def ::silenceremove-start_mode (s/nilable #{"any" "all"}))
(s/def ::silenceremove-stop_periods ::int)
(s/def ::silenceremove-stop_duration ::float)
(s/def ::silenceremove-stop_threshold ::float)
(s/def ::silenceremove-stop_silence ::float)
(s/def ::silenceremove-stop_mode (s/nilable #{"any" "all"}))
(s/def ::silenceremove-detection (s/nilable #{"peak" "rms"}))
(s/def ::silenceremove-window ::float)
(s/def ::silenceremove (s/keys :opt-un [::silenceremove-start_periods ::silenceremove-start_duration ::silenceremove-start_threshold ::silenceremove-start_silence ::silenceremove-start_mode ::silenceremove-stop_periods ::silenceremove-stop_duration ::silenceremove-stop_threshold ::silenceremove-stop_silence ::silenceremove-stop_mode ::silenceremove-detection ::silenceremove-window]))

;; Audio Visualization
(s/def ::showfreqs-size ::string)
(s/def ::showfreqs-rate ::string)
(s/def ::showfreqs-mode (s/nilable #{"line" "bar" "dot"}))
(s/def ::showfreqs-ascale (s/nilable #{"lin" "sqrt" "cbrt" "log"}))
(s/def ::showfreqs-fscale (s/nilable #{"lin" "log"}))
(s/def ::showfreqs-win_size ::int)
(s/def ::showfreqs-win_func (s/nilable #{"rect" "bartlett" "hann" "hanning" "hamming" "blackman" "welch" "flattop" "bharris" "bnuttall" "bhann" "sine" "nuttall" "lanczos" "gauss" "tukey" "dolph" "cauchy" "parzen" "poisson" "bohman"}))
(s/def ::showfreqs-overlap ::float)
(s/def ::showfreqs-averaging ::int)
(s/def ::showfreqs-colors ::string)
(s/def ::showfreqs-cmode (s/nilable #{"combined" "separate"}))
(s/def ::showfreqs-minamp ::float)
(s/def ::showfreqs (s/keys :opt-un [::showfreqs-size ::showfreqs-rate ::showfreqs-mode ::showfreqs-ascale ::showfreqs-fscale ::showfreqs-win_size ::showfreqs-win_func ::showfreqs-overlap ::showfreqs-averaging ::showfreqs-colors ::showfreqs-cmode ::showfreqs-minamp]))

(s/def ::showspectrum-size ::string)
(s/def ::showspectrum-slide (s/nilable #{"replace" "scroll" "fullframe" "rscroll"}))
(s/def ::showspectrum-mode (s/nilable #{"combined" "separate"}))
(s/def ::showspectrum-color (s/nilable #{"channel" "intensity" "rainbow" "moreland" "nebulae" "fire" "fiery" "fruit" "cool" "magma" "green" "viridis" "plasma" "cividis" "terrain"}))
(s/def ::showspectrum-scale (s/nilable #{"lin" "sqrt" "cbrt" "log" "4thrt" "5thrt"}))
(s/def ::showspectrum-fscale (s/nilable #{"lin" "log"}))
(s/def ::showspectrum-saturation ::float)
(s/def ::showspectrum-win_func (s/nilable #{"rect" "bartlett" "hann" "hanning" "hamming" "blackman" "welch" "flattop" "bharris" "bnuttall" "bhann" "sine" "nuttall" "lanczos" "gauss" "tukey" "dolph" "cauchy" "parzen" "poisson" "bohman"}))
(s/def ::showspectrum-orientation (s/nilable #{"vertical" "horizontal"}))
(s/def ::showspectrum-overlap ::float)
(s/def ::showspectrum-gain ::float)
(s/def ::showspectrum-data (s/nilable #{"magnitude" "phase" "uphase"}))
(s/def ::showspectrum-rotation ::float)
(s/def ::showspectrum-start ::int)
(s/def ::showspectrum-stop ::int)
(s/def ::showspectrum-fps ::string)
(s/def ::showspectrum-legend (s/nilable ::boolean))
(s/def ::showspectrum-drange ::float)
(s/def ::showspectrum-limit ::float)
(s/def ::showspectrum-opacity ::float)
(s/def ::showspectrum (s/keys :opt-un [::showspectrum-size ::showspectrum-slide ::showspectrum-mode ::showspectrum-color ::showspectrum-scale ::showspectrum-fscale ::showspectrum-saturation ::showspectrum-win_func ::showspectrum-orientation ::showspectrum-overlap ::showspectrum-gain ::showspectrum-data ::showspectrum-rotation ::showspectrum-start ::showspectrum-stop ::showspectrum-fps ::showspectrum-legend ::showspectrum-drange ::showspectrum-limit ::showspectrum-opacity]))

(s/def ::showvolume-rate ::string)
(s/def ::showvolume-b ::int)
(s/def ::showvolume-w ::int)
(s/def ::showvolume-h ::int)
(s/def ::showvolume-f ::float)
(s/def ::showvolume-c ::string)
(s/def ::showvolume-t (s/nilable ::boolean))
(s/def ::showvolume-v (s/nilable ::boolean))
(s/def ::showvolume-dm ::float)
(s/def ::showvolume-dmc ::string)
(s/def ::showvolume-o (s/nilable #{"h" "v"}))
(s/def ::showvolume-s ::int)
(s/def ::showvolume-p ::float)
(s/def ::showvolume-m (s/nilable #{"p" "r"}))
(s/def ::showvolume-ds (s/nilable #{"lin" "log"}))
(s/def ::showvolume (s/keys :opt-un [::showvolume-rate ::showvolume-b ::showvolume-w ::showvolume-h ::showvolume-f ::showvolume-c ::showvolume-t ::showvolume-v ::showvolume-dm ::showvolume-dmc ::showvolume-o ::showvolume-s ::showvolume-p ::showvolume-m ::showvolume-ds]))

(s/def ::showwaves-size ::string)
(s/def ::showwaves-mode (s/nilable #{"point" "line" "p2p" "cline"}))
(s/def ::showwaves-n ::int)
(s/def ::showwaves-rate ::string)
(s/def ::showwaves-split_channels (s/nilable ::boolean))
(s/def ::showwaves-colors ::string)
(s/def ::showwaves-scale (s/nilable #{"lin" "log" "sqrt" "cbrt"}))
(s/def ::showwaves-draw (s/nilable #{"scale" "full"}))
(s/def ::showwaves (s/keys :opt-un [::showwaves-size ::showwaves-mode ::showwaves-n ::showwaves-rate ::showwaves-split_channels ::showwaves-colors ::showwaves-scale ::showwaves-draw]))

(s/def ::avectorscope-mode (s/nilable #{"lissajous" "lissajous_xy" "polar"}))
(s/def ::avectorscope-size ::string)
(s/def ::avectorscope-rate ::string)
(s/def ::avectorscope-rc ::int)
(s/def ::avectorscope-gc ::int)
(s/def ::avectorscope-bc ::int)
(s/def ::avectorscope-ac ::int)
(s/def ::avectorscope-rf ::int)
(s/def ::avectorscope-gf ::int)
(s/def ::avectorscope-bf ::int)
(s/def ::avectorscope-af ::int)
(s/def ::avectorscope-zoom ::float)
(s/def ::avectorscope-draw (s/nilable #{"dot" "line"}))
(s/def ::avectorscope-scale (s/nilable #{"lin" "sqrt" "cbrt" "log"}))
(s/def ::avectorscope-swap (s/nilable ::boolean))
(s/def ::avectorscope-mirror (s/nilable #{"none" "x" "y" "xy"}))
(s/def ::avectorscope (s/keys :opt-un [::avectorscope-mode ::avectorscope-size ::avectorscope-rate ::avectorscope-rc ::avectorscope-gc ::avectorscope-bc ::avectorscope-ac ::avectorscope-rf ::avectorscope-gf ::avectorscope-bf ::avectorscope-af ::avectorscope-zoom ::avectorscope-draw ::avectorscope-scale ::avectorscope-swap ::avectorscope-mirror]))

(s/def ::aphasemeter-rate ::string)
(s/def ::aphasemeter-size ::string)
(s/def ::aphasemeter-rc ::int)
(s/def ::aphasemeter-gc ::int)
(s/def ::aphasemeter-bc ::int)
(s/def ::aphasemeter-mpc ::string)
(s/def ::aphasemeter-video (s/nilable ::boolean))
(s/def ::aphasemeter-phasing (s/nilable ::boolean))
(s/def ::aphasemeter-tolerance ::float)
(s/def ::aphasemeter-angle ::float)
(s/def ::aphasemeter-duration ::float)
(s/def ::aphasemeter (s/keys :opt-un [::aphasemeter-rate ::aphasemeter-size ::aphasemeter-rc ::aphasemeter-gc ::aphasemeter-bc ::aphasemeter-mpc ::aphasemeter-video ::aphasemeter-phasing ::aphasemeter-tolerance ::aphasemeter-angle ::aphasemeter-duration]))

(s/def ::ebur128-video (s/nilable ::boolean))
(s/def ::ebur128-size ::string)
(s/def ::ebur128-meter ::int)
(s/def ::ebur128-metadata (s/nilable ::boolean))
(s/def ::ebur128-framelog (s/nilable #{"info" "verbose"}))
(s/def ::ebur128-peak (s/nilable #{"none" "sample" "true" "all"}))
(s/def ::ebur128-dualmono (s/nilable ::boolean))
(s/def ::ebur128-panlaw ::float)
(s/def ::ebur128-target ::int)
(s/def ::ebur128-gauge (s/nilable #{"momentary" "m" "shortterm" "s" "integrated" "i" "lra" "range" "r" "sample" "peak" "true" "all"}))
(s/def ::ebur128-scale (s/nilable #{"absolute" "LUFS" "relative" "LU"}))
(s/def ::ebur128 (s/keys :opt-un [::ebur128-video ::ebur128-size ::ebur128-meter ::ebur128-metadata ::ebur128-framelog ::ebur128-peak ::ebur128-dualmono ::ebur128-panlaw ::ebur128-target ::ebur128-gauge ::ebur128-scale]))

;; Advanced Spatial and Enhancement
(s/def ::afreqshift-shift ::float)
(s/def ::afreqshift-level ::float)
(s/def ::afreqshift-order ::int)
(s/def ::afreqshift (s/keys :opt-un [::afreqshift-shift ::afreqshift-level ::afreqshift-order]))

(s/def ::aphaseshift-shift ::float)
(s/def ::aphaseshift-level ::float)
(s/def ::aphaseshift-order ::int)
(s/def ::aphaseshift (s/keys :opt-un [::aphaseshift-shift ::aphaseshift-level ::aphaseshift-order]))

(s/def ::stereotools-level_in ::float)
(s/def ::stereotools-level_out ::float)
(s/def ::stereotools-balance_in ::float)
(s/def ::stereotools-balance_out ::float)
(s/def ::stereotools-softclip (s/nilable ::boolean))
(s/def ::stereotools-mutel (s/nilable ::boolean))
(s/def ::stereotools-muter (s/nilable ::boolean))
(s/def ::stereotools-phasel (s/nilable ::boolean))
(s/def ::stereotools-phaser (s/nilable ::boolean))
(s/def ::stereotools-mode (s/nilable #{"lr>lr" "lr>ms" "ms>lr" "lr>ll" "lr>rr" "lr>l+r" "lr>l-r" "ms>ll" "ms>rr" "ms>l+r" "ms>l-r"}))
(s/def ::stereotools-slev ::float)
(s/def ::stereotools-sbal ::float)
(s/def ::stereotools-mlev ::float)
(s/def ::stereotools-mpan ::float)
(s/def ::stereotools-base ::float)
(s/def ::stereotools-delay ::float)
(s/def ::stereotools-sclevel ::float)
(s/def ::stereotools-phase ::float)
(s/def ::stereotools-bmode_in (s/nilable #{"balance" "amplitude" "power"}))
(s/def ::stereotools-bmode_out (s/nilable #{"balance" "amplitude" "power"}))
(s/def ::stereotools (s/keys :opt-un [::stereotools-level_in ::stereotools-level_out ::stereotools-balance_in ::stereotools-balance_out ::stereotools-softclip ::stereotools-mutel ::stereotools-muter ::stereotools-phasel ::stereotools-phaser ::stereotools-mode ::stereotools-slev ::stereotools-sbal ::stereotools-mlev ::stereotools-mpan ::stereotools-base ::stereotools-delay ::stereotools-sclevel ::stereotools-phase ::stereotools-bmode_in ::stereotools-bmode_out]))

(s/def ::stereowiden-delay ::float)
(s/def ::stereowiden-feedback ::float)
(s/def ::stereowiden-crossfeed ::float)
(s/def ::stereowiden-drymix ::float)
(s/def ::stereowiden (s/keys :opt-un [::stereowiden-delay ::stereowiden-feedback ::stereowiden-crossfeed ::stereowiden-drymix]))

(s/def ::extrastereo-m ::float)
(s/def ::extrastereo-c (s/nilable ::boolean))
(s/def ::extrastereo (s/keys :opt-un [::extrastereo-m ::extrastereo-c]))

(s/def ::crystalizer-i ::float)
(s/def ::crystalizer-c (s/nilable ::boolean))
(s/def ::crystalizer (s/keys :opt-un [::crystalizer-i ::crystalizer-c]))
;; Phase 6: Advanced Video Effects

;; Advanced Color Processing
(s/def ::colorchannelmixer-rr ::float)
(s/def ::colorchannelmixer-rg ::float)
(s/def ::colorchannelmixer-rb ::float)
(s/def ::colorchannelmixer-ra ::float)
(s/def ::colorchannelmixer-gr ::float)
(s/def ::colorchannelmixer-gg ::float)
(s/def ::colorchannelmixer-gb ::float)
(s/def ::colorchannelmixer-ga ::float)
(s/def ::colorchannelmixer-br ::float)
(s/def ::colorchannelmixer-bg ::float)
(s/def ::colorchannelmixer-bb ::float)
(s/def ::colorchannelmixer-ba ::float)
(s/def ::colorchannelmixer-ar ::float)
(s/def ::colorchannelmixer-ag ::float)
(s/def ::colorchannelmixer-ab ::float)
(s/def ::colorchannelmixer-aa ::float)
(s/def ::colorchannelmixer-pc (s/nilable #{"none" "lum" "max" "avg" "sum" "nrm" "pwr"}))
(s/def ::colorchannelmixer-pa ::float)
(s/def ::colorchannelmixer (s/keys :opt-un [::colorchannelmixer-rr ::colorchannelmixer-rg ::colorchannelmixer-rb ::colorchannelmixer-ra ::colorchannelmixer-gr ::colorchannelmixer-gg ::colorchannelmixer-gb ::colorchannelmixer-ga ::colorchannelmixer-br ::colorchannelmixer-bg ::colorchannelmixer-bb ::colorchannelmixer-ba ::colorchannelmixer-ar ::colorchannelmixer-ag ::colorchannelmixer-ab ::colorchannelmixer-aa ::colorchannelmixer-pc ::colorchannelmixer-pa]))

(s/def ::selectivecolor-correction_method (s/nilable #{"absolute" "relative"}))
(s/def ::selectivecolor-reds ::string)
(s/def ::selectivecolor-yellows ::string)
(s/def ::selectivecolor-greens ::string)
(s/def ::selectivecolor-cyans ::string)
(s/def ::selectivecolor-blues ::string)
(s/def ::selectivecolor-magentas ::string)
(s/def ::selectivecolor-whites ::string)
(s/def ::selectivecolor-neutrals ::string)
(s/def ::selectivecolor-blacks ::string)
(s/def ::selectivecolor-psfile ::string)
(s/def ::selectivecolor (s/keys :opt-un [::selectivecolor-correction_method ::selectivecolor-reds ::selectivecolor-yellows ::selectivecolor-greens ::selectivecolor-cyans ::selectivecolor-blues ::selectivecolor-magentas ::selectivecolor-whites ::selectivecolor-neutrals ::selectivecolor-blacks ::selectivecolor-psfile]))

(s/def ::white_balance-temperature ::float)
(s/def ::white_balance-tint ::float)
(s/def ::white_balance-preserve_lightness ::float)
(s/def ::white_balance (s/keys :opt-un [::white_balance-temperature ::white_balance-tint ::white_balance-preserve_lightness]))

(s/def ::colorize-hue ::float)
(s/def ::colorize-saturation ::float)
(s/def ::colorize-lightness ::float)
(s/def ::colorize-mix ::float)
(s/def ::colorize (s/keys :opt-un [::colorize-hue ::colorize-saturation ::colorize-lightness ::colorize-mix]))

(s/def ::colortemperature-temperature ::float)
(s/def ::colortemperature-mix ::float)
(s/def ::colortemperature-pl ::float)
(s/def ::colortemperature (s/keys :opt-un [::colortemperature-temperature ::colortemperature-mix ::colortemperature-pl]))

(s/def ::lut1d-file ::string)
(s/def ::lut1d-interp (s/nilable #{"nearest" "linear" "cosine" "cubic" "spline"}))
(s/def ::lut1d (s/keys :opt-un [::lut1d-file ::lut1d-interp]))

(s/def ::haldclut-clut ::int)
(s/def ::haldclut-interp (s/nilable #{"nearest" "trilinear" "tetrahedral"}))
(s/def ::haldclut (s/keys :opt-un [::haldclut-clut ::haldclut-interp]))

(s/def ::pseudocolor-c0 ::string)
(s/def ::pseudocolor-c1 ::string)
(s/def ::pseudocolor-c2 ::string)
(s/def ::pseudocolor-c3 ::string)
(s/def ::pseudocolor-i (s/nilable #{"lumba" "brightness"}))
(s/def ::pseudocolor-opacity ::float)
(s/def ::pseudocolor (s/keys :opt-un [::pseudocolor-c0 ::pseudocolor-c1 ::pseudocolor-c2 ::pseudocolor-c3 ::pseudocolor-i ::pseudocolor-opacity]))

;; Professional Denoising
(s/def ::bm3d-sigma ::float)
(s/def ::bm3d-block ::int)
(s/def ::bm3d-bstep ::int)
(s/def ::bm3d-group ::int)
(s/def ::bm3d-range ::int)
(s/def ::bm3d-mstep ::int)
(s/def ::bm3d-thmse ::float)
(s/def ::bm3d-hdthr ::float)
(s/def ::bm3d-estim (s/nilable #{"basic" "final" "both"}))
(s/def ::bm3d-ref (s/nilable ::boolean))
(s/def ::bm3d-planes ::int)
(s/def ::bm3d (s/keys :opt-un [::bm3d-sigma ::bm3d-block ::bm3d-bstep ::bm3d-group ::bm3d-range ::bm3d-mstep ::bm3d-thmse ::bm3d-hdthr ::bm3d-estim ::bm3d-ref ::bm3d-planes]))

(s/def ::hqdn3d-luma_spatial ::float)
(s/def ::hqdn3d-chroma_spatial ::float)
(s/def ::hqdn3d-luma_tmp ::float)
(s/def ::hqdn3d-chroma_tmp ::float)
(s/def ::hqdn3d (s/keys :opt-un [::hqdn3d-luma_spatial ::hqdn3d-chroma_spatial ::hqdn3d-luma_tmp ::hqdn3d-chroma_tmp]))

(s/def ::nlmeans-s ::float)
(s/def ::nlmeans-p ::int)
(s/def ::nlmeans-pc ::int)
(s/def ::nlmeans-r ::int)
(s/def ::nlmeans-rc ::int)
(s/def ::nlmeans (s/keys :opt-un [::nlmeans-s ::nlmeans-p ::nlmeans-pc ::nlmeans-r ::nlmeans-rc]))

(s/def ::vaguedenoiser-threshold ::float)
(s/def ::vaguedenoiser-method (s/nilable #{"soft" "hard"}))
(s/def ::vaguedenoiser-nsteps ::int)
(s/def ::vaguedenoiser-percent ::float)
(s/def ::vaguedenoiser-planes ::int)
(s/def ::vaguedenoiser-type (s/nilable #{"bior4.4" "db4" "db6" "db8" "haar"}))
(s/def ::vaguedenoiser (s/keys :opt-un [::vaguedenoiser-threshold ::vaguedenoiser-method ::vaguedenoiser-nsteps ::vaguedenoiser-percent ::vaguedenoiser-planes ::vaguedenoiser-type]))

;; Restoration Filters
(s/def ::deband-1thr ::float)
(s/def ::deband-2thr ::float)
(s/def ::deband-3thr ::float)
(s/def ::deband-4thr ::float)
(s/def ::deband-range ::int)
(s/def ::deband-direction ::float)
(s/def ::deband-blur (s/nilable ::boolean))
(s/def ::deband-coupling (s/nilable ::boolean))
(s/def ::deband (s/keys :opt-un [::deband-1thr ::deband-2thr ::deband-3thr ::deband-4thr ::deband-range ::deband-direction ::deband-blur ::deband-coupling]))

(s/def ::gradfun-strength ::float)
(s/def ::gradfun-radius ::int)
(s/def ::gradfun (s/keys :opt-un [::gradfun-strength ::gradfun-radius]))

(s/def ::removegrain-m0 ::int)
(s/def ::removegrain-m1 ::int)
(s/def ::removegrain-m2 ::int)
(s/def ::removegrain-m3 ::int)
(s/def ::removegrain (s/keys :opt-un [::removegrain-m0 ::removegrain-m1 ::removegrain-m2 ::removegrain-m3]))

(s/def ::repair-m0 ::int)
(s/def ::repair-m1 ::int)
(s/def ::repair-m2 ::int)
(s/def ::repair-m3 ::int)
(s/def ::repair (s/keys :opt-un [::repair-m0 ::repair-m1 ::repair-m2 ::repair-m3]))

(s/def ::temporalsoften-radius ::int)
(s/def ::temporalsoften-luma_threshold ::int)
(s/def ::temporalsoften-chroma_threshold ::int)
(s/def ::temporalsoften-scenechange ::int)
(s/def ::temporalsoften-planes ::int)
(s/def ::temporalsoften (s/keys :opt-un [::temporalsoften-radius ::temporalsoften-luma_threshold ::temporalsoften-chroma_threshold ::temporalsoften-scenechange ::temporalsoften-planes]))

(s/def ::dctdnoiz-sigma ::float)
(s/def ::dctdnoiz-overlap ::int)
(s/def ::dctdnoiz-expr ::string)
(s/def ::dctdnoiz-n ::int)
(s/def ::dctdnoiz (s/keys :opt-un [::dctdnoiz-sigma ::dctdnoiz-overlap ::dctdnoiz-expr ::dctdnoiz-n]))

(s/def ::fftdnoiz-sigma ::float)
(s/def ::fftdnoiz-amount ::float)
(s/def ::fftdnoiz-block ::int)
(s/def ::fftdnoiz-overlap ::float)
(s/def ::fftdnoiz-method (s/nilable #{"wiener" "spectral"}))
(s/def ::fftdnoiz-prev ::int)
(s/def ::fftdnoiz-next ::int)
(s/def ::fftdnoiz-planes ::int)
(s/def ::fftdnoiz-window (s/nilable #{"rect" "bartlett" "hann" "hanning" "hamming" "blackman" "welch" "flattop" "bharris" "bnuttall" "bhann" "sine" "nuttall" "lanczos" "gauss" "tukey" "dolph" "cauchy" "parzen" "poisson" "bohman"}))
(s/def ::fftdnoiz (s/keys :opt-un [::fftdnoiz-sigma ::fftdnoiz-amount ::fftdnoiz-block ::fftdnoiz-overlap ::fftdnoiz-method ::fftdnoiz-prev ::fftdnoiz-next ::fftdnoiz-planes ::fftdnoiz-window]))

;; Compositing Operations
(s/def ::blend-mode (s/nilable #{"addition" "addition128" "grainmerge" "and" "average" "burn" "darken" "difference" "difference128" "grainextract" "divide" "dodge" "freeze" "exclusion" "extremity" "glow" "hardlight" "hardmix" "heat" "lighten" "linearlight" "multiply" "multiply128" "negation" "normal" "or" "overlay" "phoenix" "pinlight" "reflect" "screen" "softlight" "subtract" "vividlight" "xor"}))
(s/def ::blend-opacity ::float)
(s/def ::blend-expr ::string)
(s/def ::blend (s/keys :opt-un [::blend-mode ::blend-opacity ::blend-expr]))

(s/def ::threshold-threshold ::float)
(s/def ::threshold (s/keys :opt-un [::threshold-threshold]))

(s/def ::mix-inputs ::int)
(s/def ::mix-weights ::string)
(s/def ::mix-scale ::float)
(s/def ::mix-planes ::int)
(s/def ::mix-duration (s/nilable #{"longest" "shortest" "first"}))
(s/def ::mix (s/keys :opt-un [::mix-inputs ::mix-weights ::mix-scale ::mix-planes ::mix-duration]))

;; Motion and Temporal Effects
(s/def ::tmedian-radius ::int)
(s/def ::tmedian-planes ::int)
(s/def ::tmedian-percentile ::float)
(s/def ::tmedian (s/keys :opt-un [::tmedian-radius ::tmedian-planes ::tmedian-percentile]))

(s/def ::tmix-frames ::int)
(s/def ::tmix-weights ::string)
(s/def ::tmix-scale ::float)
(s/def ::tmix-planes ::int)
(s/def ::tmix (s/keys :opt-un [::tmix-frames ::tmix-weights ::tmix-scale ::tmix-planes]))

(s/def ::slowmo-rate ::string)
(s/def ::slowmo-eval (s/nilable #{"init" "frame"}))
(s/def ::slowmo (s/keys :opt-un [::slowmo-rate ::slowmo-eval]))

(s/def ::estdif-mode (s/nilable #{"frame" "field"}))
(s/def ::estdif-parity (s/nilable #{"tff" "bff" "auto"}))
(s/def ::estdif-deint (s/nilable #{"all" "interlaced"}))
(s/def ::estdif-rslope ::int)
(s/def ::estdif-redge ::int)
(s/def ::estdif-ecost ::float)
(s/def ::estdif-mcost ::float)
(s/def ::estdif-dcost ::float)
(s/def ::estdif-interp (s/nilable #{"2p" "4p" "6p"}))
(s/def ::estdif (s/keys :opt-un [::estdif-mode ::estdif-parity ::estdif-deint ::estdif-rslope ::estdif-redge ::estdif-ecost ::estdif-mcost ::estdif-dcost ::estdif-interp]))

(s/def ::motion-global_value ::int)
(s/def ::motion-filter ::string)
(s/def ::motion (s/keys :opt-un [::motion-global_value ::motion-filter]))

(s/def ::midequalizer-planes ::int)
(s/def ::midequalizer (s/keys :opt-un [::midequalizer-planes]))

;; Frame Processing and Utilities
(s/def ::reverse-filename ::string)
(s/def ::reverse (s/keys :opt-un [::reverse-filename]))

(s/def ::loop-loop ::int)
(s/def ::loop-size ::int)
(s/def ::loop-start ::int)
(s/def ::loop (s/keys :opt-un [::loop-loop ::loop-size ::loop-start]))

(s/def ::select-expr ::string)
(s/def ::select-outputs ::int)
(s/def ::select (s/keys :req-un [::select-expr] :opt-un [::select-outputs]))

(s/def ::thumbnail-n ::int)
(s/def ::thumbnail-log (s/nilable #{"quiet" "info" "verbose" "debug"}))
(s/def ::thumbnail (s/keys :opt-un [::thumbnail-n ::thumbnail-log]))

(s/def ::tile-layout ::string)
(s/def ::tile-nb_frames ::int)
(s/def ::tile-margin ::int)
(s/def ::tile-padding ::int)
(s/def ::tile-color ::string)
(s/def ::tile-overlap ::int)
(s/def ::tile-init_padding ::int)
(s/def ::tile (s/keys :opt-un [::tile-layout ::tile-nb_frames ::tile-margin ::tile-padding ::tile-color ::tile-overlap ::tile-init_padding]))

(s/def ::stack-inputs ::int)
(s/def ::stack-layout ::string)
(s/def ::stack-grid ::string)
(s/def ::stack-shortest (s/nilable ::boolean))
(s/def ::stack-fill ::string)
(s/def ::stack (s/keys :opt-un [::stack-inputs ::stack-layout ::stack-grid ::stack-shortest ::stack-fill]))

(s/def ::xstack-inputs ::int)
(s/def ::xstack-layout ::string)
(s/def ::xstack-grid ::string)
(s/def ::xstack-shortest (s/nilable ::boolean))
(s/def ::xstack-fill ::string)
(s/def ::xstack (s/keys :opt-un [::xstack-inputs ::xstack-layout ::xstack-grid ::xstack-shortest ::xstack-fill]))

;; Advanced Processing Filters
(s/def ::pp-subfilters ::string)
(s/def ::pp (s/keys :opt-un [::pp-subfilters]))

(s/def ::spp-quality ::int)
(s/def ::spp-qp ::int)
(s/def ::spp-mode (s/nilable #{"hard" "soft"}))
(s/def ::spp-use_bframe_qp (s/nilable ::boolean))
(s/def ::spp (s/keys :opt-un [::spp-quality ::spp-qp ::spp-mode ::spp-use_bframe_qp]))

(s/def ::uspp-quality ::int)
(s/def ::uspp-qp ::int)
(s/def ::uspp-use_bframe_qp (s/nilable ::boolean))
(s/def ::uspp (s/keys :opt-un [::uspp-quality ::uspp-qp ::uspp-use_bframe_qp]))

;; Geometric and Distortion Effects  
(s/def ::fisheye-strength ::float)
(s/def ::fisheye (s/keys :opt-un [::fisheye-strength]))

(s/def ::barrel-luma_radius ::float)
(s/def ::barrel-luma_power ::int)
(s/def ::barrel-chroma_radius ::float)
(s/def ::barrel-chroma_power ::int)
(s/def ::barrel (s/keys :opt-un [::barrel-luma_radius ::barrel-luma_power ::barrel-chroma_radius ::barrel-chroma_power]))

(s/def ::pincushion-amount ::float)
(s/def ::pincushion (s/keys :opt-un [::pincushion-amount]))

(s/def ::remap-format (s/nilable #{"color" "gray"}))
(s/def ::remap-fill ::string)
(s/def ::remap (s/keys :opt-un [::remap-format ::remap-fill]))

;; Palette Operations
(s/def ::palettegen-max_colors ::int)
(s/def ::palettegen-reserve_transparent (s/nilable ::boolean))
(s/def ::palettegen-transparency_color ::string)
(s/def ::palettegen-stats_mode (s/nilable #{"full" "diff" "single"}))
(s/def ::palettegen (s/keys :opt-un [::palettegen-max_colors ::palettegen-reserve_transparent ::palettegen-transparency_color ::palettegen-stats_mode]))

(s/def ::paletteuse-dither (s/nilable #{"bayer" "heckbert" "floyd_steinberg" "sierra2" "sierra2_4a" "sierra3" "burkes" "atkinson" "none"}))
(s/def ::paletteuse-bayer_scale ::int)
(s/def ::paletteuse-diff_mode (s/nilable #{"rectangle" "none"}))
(s/def ::paletteuse-new (s/nilable ::boolean))
(s/def ::paletteuse-alpha_threshold ::int)
(s/def ::paletteuse-debug_kdtree ::string)
(s/def ::paletteuse (s/keys :opt-un [::paletteuse-dither ::paletteuse-bayer_scale ::paletteuse-diff_mode ::paletteuse-new ::paletteuse-alpha_threshold ::paletteuse-debug_kdtree]))

(s/def ::showpalette-s ::int)
(s/def ::showpalette (s/keys :opt-un [::showpalette-s]))
;; Phase 7: Hardware & Platform Integration

;; GPU Acceleration (CUDA)
(s/def ::scale_cuda-w ::string)
(s/def ::scale_cuda-h ::string)
(s/def ::scale_cuda-format ::string)
(s/def ::scale_cuda-interp_algo (s/nilable #{"nearest" "linear" "cubic" "cubic2" "lanczos" "super"}))
(s/def ::scale_cuda-force_original_aspect_ratio (s/nilable #{"disable" "decrease" "increase"}))
(s/def ::scale_cuda-force_divisible_by ::int)
(s/def ::scale_cuda (s/keys :opt-un [::scale_cuda-w ::scale_cuda-h ::scale_cuda-format ::scale_cuda-interp_algo ::scale_cuda-force_original_aspect_ratio ::scale_cuda-force_divisible_by]))

(s/def ::overlay_cuda-x ::string)
(s/def ::overlay_cuda-y ::string)
(s/def ::overlay_cuda-eof_action (s/nilable #{"repeat" "endall" "pass"}))
(s/def ::overlay_cuda-eval (s/nilable #{"init" "frame"}))
(s/def ::overlay_cuda-shortest (s/nilable ::boolean))
(s/def ::overlay_cuda-format (s/nilable #{"yuv420" "yuv422" "yuv444" "auto"}))
(s/def ::overlay_cuda-repeatlast (s/nilable ::boolean))
(s/def ::overlay_cuda-alpha (s/nilable #{"straight" "premultiplied"}))
(s/def ::overlay_cuda (s/keys :opt-un [::overlay_cuda-x ::overlay_cuda-y ::overlay_cuda-eof_action ::overlay_cuda-eval ::overlay_cuda-shortest ::overlay_cuda-format ::overlay_cuda-repeatlast ::overlay_cuda-alpha]))

(s/def ::chromakey_cuda-color ::string)
(s/def ::chromakey_cuda-similarity ::float)
(s/def ::chromakey_cuda-blend ::float)
(s/def ::chromakey_cuda-yuv (s/nilable ::boolean))
(s/def ::chromakey_cuda (s/keys :req-un [::chromakey_cuda-color] :opt-un [::chromakey_cuda-similarity ::chromakey_cuda-blend ::chromakey_cuda-yuv]))

(s/def ::yadif_cuda-mode (s/nilable #{"0" "1" "2" "3" "send_frame" "send_field" "send_frame_nospatial" "send_field_nospatial"}))
(s/def ::yadif_cuda-parity (s/nilable #{"-1" "0" "1" "auto" "tff" "bff"}))
(s/def ::yadif_cuda-deint (s/nilable #{"0" "1" "all" "interlaced"}))
(s/def ::yadif_cuda (s/keys :opt-un [::yadif_cuda-mode ::yadif_cuda-parity ::yadif_cuda-deint]))

(s/def ::bwdif_cuda-mode (s/nilable #{"0" "1" "send_frame" "send_field"}))
(s/def ::bwdif_cuda-parity (s/nilable #{"-1" "0" "1" "auto" "tff" "bff"}))
(s/def ::bwdif_cuda-deint (s/nilable #{"0" "1" "all" "interlaced"}))
(s/def ::bwdif_cuda (s/keys :opt-un [::bwdif_cuda-mode ::bwdif_cuda-parity ::bwdif_cuda-deint]))

(s/def ::transpose_cuda-dir (s/nilable #{"0" "1" "2" "3" "cclock_flip" "clock" "cclock" "clock_flip"}))
(s/def ::transpose_cuda-passthrough (s/nilable #{"none" "portrait" "landscape"}))
(s/def ::transpose_cuda (s/keys :opt-un [::transpose_cuda-dir ::transpose_cuda-passthrough]))

(s/def ::thumbnail_cuda-n ::int)
(s/def ::thumbnail_cuda-log (s/nilable #{"quiet" "info" "verbose" "debug"}))
(s/def ::thumbnail_cuda (s/keys :opt-un [::thumbnail_cuda-n ::thumbnail_cuda-log]))

(s/def ::hwupload_cuda-device ::int)
(s/def ::hwupload_cuda (s/keys :opt-un [::hwupload_cuda-device]))

(s/def ::hwdownload_cuda (s/keys :opt-un []))

;; OpenCL Acceleration
(s/def ::format_opencl-pix_fmts ::string)
(s/def ::format_opencl (s/keys :req-un [::format_opencl-pix_fmts]))

(s/def ::tonemap_opencl-tonemap (s/nilable #{"none" "linear" "gamma" "clip" "reinhard" "hable" "mobius"}))
(s/def ::tonemap_opencl-param ::float)
(s/def ::tonemap_opencl-desat ::float)
(s/def ::tonemap_opencl-peak ::float)
(s/def ::tonemap_opencl-scene_threshold ::float)
(s/def ::tonemap_opencl-max_boost ::float)
(s/def ::tonemap_opencl (s/keys :opt-un [::tonemap_opencl-tonemap ::tonemap_opencl-param ::tonemap_opencl-desat ::tonemap_opencl-peak ::tonemap_opencl-scene_threshold ::tonemap_opencl-max_boost]))

(s/def ::nlmeans_opencl-s ::float)
(s/def ::nlmeans_opencl-p ::int)
(s/def ::nlmeans_opencl-pc ::int)
(s/def ::nlmeans_opencl-r ::int)
(s/def ::nlmeans_opencl-rc ::int)
(s/def ::nlmeans_opencl (s/keys :opt-un [::nlmeans_opencl-s ::nlmeans_opencl-p ::nlmeans_opencl-pc ::nlmeans_opencl-r ::nlmeans_opencl-rc]))

(s/def ::deshake_opencl-x ::int)
(s/def ::deshake_opencl-y ::int)
(s/def ::deshake_opencl-w ::int)
(s/def ::deshake_opencl-h ::int)
(s/def ::deshake_opencl-rx ::int)
(s/def ::deshake_opencl-ry ::int)
(s/def ::deshake_opencl-edge (s/nilable #{"blank" "original" "clamp" "mirror"}))
(s/def ::deshake_opencl-blocksize ::int)
(s/def ::deshake_opencl-contrast ::float)
(s/def ::deshake_opencl-search ::int)
(s/def ::deshake_opencl-filename ::string)
(s/def ::deshake_opencl-opencl_device ::int)
(s/def ::deshake_opencl (s/keys :opt-un [::deshake_opencl-x ::deshake_opencl-y ::deshake_opencl-w ::deshake_opencl-h ::deshake_opencl-rx ::deshake_opencl-ry ::deshake_opencl-edge ::deshake_opencl-blocksize ::deshake_opencl-contrast ::deshake_opencl-search ::deshake_opencl-filename ::deshake_opencl-opencl_device]))

(s/def ::perspective_opencl-x0 ::string)
(s/def ::perspective_opencl-y0 ::string)
(s/def ::perspective_opencl-x1 ::string)
(s/def ::perspective_opencl-y1 ::string)
(s/def ::perspective_opencl-x2 ::string)
(s/def ::perspective_opencl-y2 ::string)
(s/def ::perspective_opencl-x3 ::string)
(s/def ::perspective_opencl-y3 ::string)
(s/def ::perspective_opencl-interpolation (s/nilable #{"linear" "cubic"}))
(s/def ::perspective_opencl-sense (s/nilable #{"source" "destination"}))
(s/def ::perspective_opencl-eval (s/nilable #{"init" "frame"}))
(s/def ::perspective_opencl (s/keys :opt-un [::perspective_opencl-x0 ::perspective_opencl-y0 ::perspective_opencl-x1 ::perspective_opencl-y1 ::perspective_opencl-x2 ::perspective_opencl-y2 ::perspective_opencl-x3 ::perspective_opencl-y3 ::perspective_opencl-interpolation ::perspective_opencl-sense ::perspective_opencl-eval]))

(s/def ::overlay_opencl-x ::string)
(s/def ::overlay_opencl-y ::string)
(s/def ::overlay_opencl-eof_action (s/nilable #{"repeat" "endall" "pass"}))
(s/def ::overlay_opencl-eval (s/nilable #{"init" "frame"}))
(s/def ::overlay_opencl-shortest (s/nilable ::boolean))
(s/def ::overlay_opencl-format (s/nilable #{"yuv420" "yuv422" "yuv444" "auto"}))
(s/def ::overlay_opencl-repeatlast (s/nilable ::boolean))
(s/def ::overlay_opencl-alpha (s/nilable #{"straight" "premultiplied"}))
(s/def ::overlay_opencl (s/keys :opt-un [::overlay_opencl-x ::overlay_opencl-y ::overlay_opencl-eof_action ::overlay_opencl-eval ::overlay_opencl-shortest ::overlay_opencl-format ::overlay_opencl-repeatlast ::overlay_opencl-alpha]))

(s/def ::hwupload_opencl-device ::int)
(s/def ::hwupload_opencl (s/keys :opt-un [::hwupload_opencl-device]))

(s/def ::hwdownload_opencl (s/keys :opt-un []))

;; Platform Capture (Video4Linux)
(s/def ::v4l2-standard ::string)
(s/def ::v4l2-channel ::int)
(s/def ::v4l2-video_size ::string)
(s/def ::v4l2-pixel_format ::string)
(s/def ::v4l2-input_format ::string)
(s/def ::v4l2-framerate ::string)
(s/def ::v4l2-use_libv4l2 (s/nilable ::boolean))
(s/def ::v4l2-ts (s/nilable #{"default" "abs" "mono2abs"}))
(s/def ::v4l2 (s/keys :opt-un [::v4l2-standard ::v4l2-channel ::v4l2-video_size ::v4l2-pixel_format ::v4l2-input_format ::v4l2-framerate ::v4l2-use_libv4l2 ::v4l2-ts]))

;; Platform Capture (DirectShow - Windows)
(s/def ::dshow-video_device_name ::string)
(s/def ::dshow-audio_device_name ::string)
(s/def ::dshow-video_size ::string)
(s/def ::dshow-pixel_format ::string)
(s/def ::dshow-framerate ::string)
(s/def ::dshow-sample_rate ::int)
(s/def ::dshow-sample_size ::int)
(s/def ::dshow-channels ::int)
(s/def ::dshow-list_devices (s/nilable ::boolean))
(s/def ::dshow-list_options (s/nilable ::boolean))
(s/def ::dshow-video_device_number ::int)
(s/def ::dshow-audio_device_number ::int)
(s/def ::dshow-audio_buffer_size ::int)
(s/def ::dshow-video_pin_name ::string)
(s/def ::dshow-audio_pin_name ::string)
(s/def ::dshow-crossbar_video_input_pin_number ::int)
(s/def ::dshow-crossbar_audio_input_pin_number ::int)
(s/def ::dshow-show_video_device_dialog (s/nilable ::boolean))
(s/def ::dshow-show_audio_device_dialog (s/nilable ::boolean))
(s/def ::dshow-show_video_crossbar_connection_dialog (s/nilable ::boolean))
(s/def ::dshow-show_audio_crossbar_connection_dialog (s/nilable ::boolean))
(s/def ::dshow-show_analog_tv_tuner_dialog (s/nilable ::boolean))
(s/def ::dshow-show_analog_tv_tuner_audio_dialog (s/nilable ::boolean))
(s/def ::dshow (s/keys :opt-un [::dshow-video_device_name ::dshow-audio_device_name ::dshow-video_size ::dshow-pixel_format ::dshow-framerate ::dshow-sample_rate ::dshow-sample_size ::dshow-channels ::dshow-list_devices ::dshow-list_options ::dshow-video_device_number ::dshow-audio_device_number ::dshow-audio_buffer_size ::dshow-video_pin_name ::dshow-audio_pin_name ::dshow-crossbar_video_input_pin_number ::dshow-crossbar_audio_input_pin_number ::dshow-show_video_device_dialog ::dshow-show_audio_device_dialog ::dshow-show_video_crossbar_connection_dialog ::dshow-show_audio_crossbar_connection_dialog ::dshow-show_analog_tv_tuner_dialog ::dshow-show_analog_tv_tuner_audio_dialog]))

;; Platform Capture (AVFoundation - macOS)
(s/def ::avfoundation-list_devices (s/nilable ::boolean))
(s/def ::avfoundation-video_device_index ::int)
(s/def ::avfoundation-audio_device_index ::int)
(s/def ::avfoundation-video_filename ::string)
(s/def ::avfoundation-audio_filename ::string)
(s/def ::avfoundation-video_size ::string)
(s/def ::avfoundation-framerate ::string)
(s/def ::avfoundation-pixel_format ::string)
(s/def ::avfoundation-capture_cursor (s/nilable ::boolean))
(s/def ::avfoundation-capture_mouse_clicks (s/nilable ::boolean))
(s/def ::avfoundation (s/keys :opt-un [::avfoundation-list_devices ::avfoundation-video_device_index ::avfoundation-audio_device_index ::avfoundation-video_filename ::avfoundation-audio_filename ::avfoundation-video_size ::avfoundation-framerate ::avfoundation-pixel_format ::avfoundation-capture_cursor ::avfoundation-capture_mouse_clicks]))

;; Screen Capture (GDI - Windows)
(s/def ::gdigrab-framerate ::string)
(s/def ::gdigrab-draw_mouse (s/nilable ::boolean))
(s/def ::gdigrab-show_region (s/nilable ::boolean))
(s/def ::gdigrab-video_size ::string)
(s/def ::gdigrab-offset_x ::int)
(s/def ::gdigrab-offset_y ::int)
(s/def ::gdigrab (s/keys :opt-un [::gdigrab-framerate ::gdigrab-draw_mouse ::gdigrab-show_region ::gdigrab-video_size ::gdigrab-offset_x ::gdigrab-offset_y]))

;; Screen Capture (KMS - Linux)
(s/def ::kmsgrab-device ::string)
(s/def ::kmsgrab-format ::string)
(s/def ::kmsgrab-format_modifier ::string)
(s/def ::kmsgrab-crtc_id ::int)
(s/def ::kmsgrab-plane_id ::int)
(s/def ::kmsgrab-framerate ::string)
(s/def ::kmsgrab (s/keys :opt-un [::kmsgrab-device ::kmsgrab-format ::kmsgrab-format_modifier ::kmsgrab-crtc_id ::kmsgrab-plane_id ::kmsgrab-framerate]))

;; Screen Capture (X11 - Linux/Unix)
(s/def ::x11grab-video_size ::string)
(s/def ::x11grab-framerate ::string)
(s/def ::x11grab-draw_mouse (s/nilable ::boolean))
(s/def ::x11grab-follow_mouse (s/nilable #{"centered" "0"}))
(s/def ::x11grab-show_region (s/nilable ::boolean))
(s/def ::x11grab-region_border ::int)
(s/def ::x11grab-select_region (s/nilable ::boolean))
(s/def ::x11grab-use_shm (s/nilable ::boolean))
(s/def ::x11grab (s/keys :opt-un [::x11grab-video_size ::x11grab-framerate ::x11grab-draw_mouse ::x11grab-follow_mouse ::x11grab-show_region ::x11grab-region_border ::x11grab-select_region ::x11grab-use_shm]))

;; Graphics Integration
(s/def ::opengl-window_title ::string)
(s/def ::opengl-window_size ::string)
(s/def ::opengl-rate ::string)
(s/def ::opengl (s/keys :opt-un [::opengl-window_title ::opengl-window_size ::opengl-rate]))

(s/def ::coreimage-list_filters (s/nilable ::boolean))
(s/def ::coreimage-filter ::string)
(s/def ::coreimage-output_rect ::string)
(s/def ::coreimage (s/keys :opt-un [::coreimage-list_filters ::coreimage-filter ::coreimage-output_rect]))

;; Third-party Integration
(s/def ::frei0r-filter_name ::string)
(s/def ::frei0r-filter_params ::string)
(s/def ::frei0r (s/keys :req-un [::frei0r-filter_name] :opt-un [::frei0r-filter_params]))

(s/def ::opencv-filter_name ::string)
(s/def ::opencv-filter_params ::string)
(s/def ::opencv (s/keys :req-un [::opencv-filter_name] :opt-un [::opencv-filter_params]))

;; Network Streaming Sources
(s/def ::rtmp-rtmp_app ::string)
(s/def ::rtmp-rtmp_buffer ::int)
(s/def ::rtmp-rtmp_conn ::string)
(s/def ::rtmp-rtmp_flashver ::string)
(s/def ::rtmp-rtmp_flush_interval ::int)
(s/def ::rtmp-rtmp_live (s/nilable #{"live" "recorded" "append"}))
(s/def ::rtmp-rtmp_pageurl ::string)
(s/def ::rtmp-rtmp_playpath ::string)
(s/def ::rtmp-rtmp_subscribe ::string)
(s/def ::rtmp-rtmp_swfhash ::string)
(s/def ::rtmp-rtmp_swfsize ::int)
(s/def ::rtmp-rtmp_swfurl ::string)
(s/def ::rtmp-rtmp_swfvfy (s/nilable ::boolean))
(s/def ::rtmp-rtmp_tcurl ::string)
(s/def ::rtmp-tcp_nodelay (s/nilable ::boolean))
(s/def ::rtmp-timeout ::int)
(s/def ::rtmp (s/keys :opt-un [::rtmp-rtmp_app ::rtmp-rtmp_buffer ::rtmp-rtmp_conn ::rtmp-rtmp_flashver ::rtmp-rtmp_flush_interval ::rtmp-rtmp_live ::rtmp-rtmp_pageurl ::rtmp-rtmp_playpath ::rtmp-rtmp_subscribe ::rtmp-rtmp_swfhash ::rtmp-rtmp_swfsize ::rtmp-rtmp_swfurl ::rtmp-rtmp_swfvfy ::rtmp-rtmp_tcurl ::rtmp-tcp_nodelay ::rtmp-timeout]))

(s/def ::rtp-buffer_size ::int)
(s/def ::rtp-rtcp_port ::int)
(s/def ::rtp-ttl ::int)
(s/def ::rtp-connect (s/nilable ::boolean))
(s/def ::rtp-write_to_source (s/nilable ::boolean))
(s/def ::rtp-pkt_size ::int)
(s/def ::rtp-dscp ::int)
(s/def ::rtp (s/keys :opt-un [::rtp-buffer_size ::rtp-rtcp_port ::rtp-ttl ::rtp-connect ::rtp-write_to_source ::rtp-pkt_size ::rtp-dscp]))

(s/def ::udp-buffer_size ::int)
(s/def ::udp-localport ::int)
(s/def ::udp-localaddr ::string)
(s/def ::udp-pkt_size ::int)
(s/def ::udp-reuse (s/nilable ::boolean))
(s/def ::udp-ttl ::int)
(s/def ::udp-connect (s/nilable ::boolean))
(s/def ::udp-fifo_size ::int)
(s/def ::udp-overrun_nonfatal (s/nilable ::boolean))
(s/def ::udp-timeout ::int)
(s/def ::udp-broadcast (s/nilable ::boolean))
(s/def ::udp (s/keys :opt-un [::udp-buffer_size ::udp-localport ::udp-localaddr ::udp-pkt_size ::udp-reuse ::udp-ttl ::udp-connect ::udp-fifo_size ::udp-overrun_nonfatal ::udp-timeout ::udp-broadcast]))

(s/def ::tcp-listen (s/nilable ::boolean))
(s/def ::tcp-timeout ::int)
(s/def ::tcp-listen_timeout ::int)
(s/def ::tcp-recv_buffer_size ::int)
(s/def ::tcp-send_buffer_size ::int)
(s/def ::tcp-tcp_nodelay (s/nilable ::boolean))
(s/def ::tcp-tcp_mss ::int)
(s/def ::tcp (s/keys :opt-un [::tcp-listen ::tcp-timeout ::tcp-listen_timeout ::tcp-recv_buffer_size ::tcp-send_buffer_size ::tcp-tcp_nodelay ::tcp-tcp_mss]))

(s/def ::http-seekable (s/nilable ::boolean))
(s/def ::http-chunked_post (s/nilable ::boolean))
(s/def ::http-http_proxy ::string)
(s/def ::http-headers ::string)
(s/def ::http-content_type ::string)
(s/def ::http-user_agent ::string)
(s/def ::http-referer ::string)
(s/def ::http-multiple_requests (s/nilable ::boolean))
(s/def ::http-post_data ::string)
(s/def ::http-mime_type ::string)
(s/def ::http-cookies ::string)
(s/def ::http-icy (s/nilable ::boolean))
(s/def ::http-icy_metadata_headers ::string)
(s/def ::http-icy_metadata_packet ::string)
(s/def ::http-auth_type (s/nilable #{"none" "basic" "digest"}))
(s/def ::http-send_expect_100 (s/nilable ::boolean))
(s/def ::http-location ::string)
(s/def ::http-offset ::int)
(s/def ::http-end_offset ::int)
(s/def ::http-method ::string)
(s/def ::http-reconnect (s/nilable ::boolean))
(s/def ::http-reconnect_at_eof (s/nilable ::boolean))
(s/def ::http-reconnect_streamed (s/nilable ::boolean))
(s/def ::http-reconnect_delay_max ::int)
(s/def ::http-reconnect_on_network_error (s/nilable ::boolean))
(s/def ::http-reconnect_on_http_error ::string)
(s/def ::http-listen (s/nilable ::boolean))
(s/def ::http-resource ::string)
(s/def ::http-reply_code ::int)
(s/def ::http (s/keys :opt-un [::http-seekable ::http-chunked_post ::http-http_proxy ::http-headers ::http-content_type ::http-user_agent ::http-referer ::http-multiple_requests ::http-post_data ::http-mime_type ::http-cookies ::http-icy ::http-icy_metadata_headers ::http-icy_metadata_packet ::http-auth_type ::http-send_expect_100 ::http-location ::http-offset ::http-end_offset ::http-method ::http-reconnect ::http-reconnect_at_eof ::http-reconnect_streamed ::http-reconnect_delay_max ::http-reconnect_on_network_error ::http-reconnect_on_http_error ::http-listen ::http-resource ::http-reply_code]))

;; Streaming Output Formats
(s/def ::hls-hls_time ::int)
(s/def ::hls-hls_list_size ::int)
(s/def ::hls-hls_wrap ::int)
(s/def ::hls-hls_allow_cache (s/nilable ::boolean))
(s/def ::hls-hls_base_url ::string)
(s/def ::hls-hls_segment_filename ::string)
(s/def ::hls-hls_segment_size ::int)
(s/def ::hls-hls_key_info_file ::string)
(s/def ::hls-hls_subtitle_path ::string)
(s/def ::hls-hls_flags ::string)
(s/def ::hls-hls_playlist_type (s/nilable #{"event" "vod"}))
(s/def ::hls-method (s/nilable #{"PUT" "POST"}))
(s/def ::hls-hls_start_number_source (s/nilable #{"generic" "epoch" "epoch_us" "datetime"}))
(s/def ::hls-hls_init_time ::float)
(s/def ::hls-strftime (s/nilable ::boolean))
(s/def ::hls-strftime_mkdir (s/nilable ::boolean))
(s/def ::hls-hls_segment_options ::string)
(s/def ::hls-use_localtime (s/nilable ::boolean))
(s/def ::hls-use_localtime_mkdir (s/nilable ::boolean))
(s/def ::hls-hls_segment_type (s/nilable #{"mpegts" "fmp4"}))
(s/def ::hls (s/keys :opt-un [::hls-hls_time ::hls-hls_list_size ::hls-hls_wrap ::hls-hls_allow_cache ::hls-hls_base_url ::hls-hls_segment_filename ::hls-hls_segment_size ::hls-hls_key_info_file ::hls-hls_subtitle_path ::hls-hls_flags ::hls-hls_playlist_type ::hls-method ::hls-hls_start_number_source ::hls-hls_init_time ::hls-strftime ::hls-strftime_mkdir ::hls-hls_segment_options ::hls-use_localtime ::hls-use_localtime_mkdir ::hls-hls_segment_type]))

(s/def ::dash-adaptation_sets ::string)
(s/def ::dash-window_size ::int)
(s/def ::dash-extra_window_size ::int)
(s/def ::dash-min_seg_duration ::int)
(s/def ::dash-media_seg_name ::string)
(s/def ::dash-init_seg_name ::string)
(s/def ::dash-utc_timing_url ::string)
(s/def ::dash-method ::string)
(s/def ::dash-http_user_agent ::string)
(s/def ::dash-http_persistent (s/nilable ::boolean))
(s/def ::dash-hls_playlist (s/nilable ::boolean))
(s/def ::dash-streaming (s/nilable ::boolean))
(s/def ::dash-timeout ::int)
(s/def ::dash-index_correction (s/nilable ::boolean))
(s/def ::dash-format_options ::string)
(s/def ::dash-global_sidx (s/nilable ::boolean))
(s/def ::dash-dash_segment_type (s/nilable #{"auto" "mp4" "webm"}))
(s/def ::dash-ignore_io_errors (s/nilable ::boolean))
(s/def ::dash-lhls (s/nilable ::boolean))
(s/def ::dash-ldash (s/nilable ::boolean))
(s/def ::dash-master_m3u8_publish_rate ::int)
(s/def ::dash-http_opts ::string)
(s/def ::dash-target_latency ::int)
(s/def ::dash-min_playback_rate ::float)
(s/def ::dash-max_playback_rate ::float)
(s/def ::dash (s/keys :opt-un [::dash-adaptation_sets ::dash-window_size ::dash-extra_window_size ::dash-min_seg_duration ::dash-media_seg_name ::dash-init_seg_name ::dash-utc_timing_url ::dash-method ::dash-http_user_agent ::dash-http_persistent ::dash-hls_playlist ::dash-streaming ::dash-timeout ::dash-index_correction ::dash-format_options ::dash-global_sidx ::dash-dash_segment_type ::dash-ignore_io_errors ::dash-lhls ::dash-ldash ::dash-master_m3u8_publish_rate ::dash-http_opts ::dash-target_latency ::dash-min_playback_rate ::dash-max_playback_rate]))

(s/def ::segment-segment_format ::string)
(s/def ::segment-segment_list ::string)
(s/def ::segment-segment_list_flags ::string)
(s/def ::segment-segment_list_size ::int)
(s/def ::segment-segment_list_type (s/nilable #{"flat" "csv" "ext" "ffconcat" "m3u8"}))
(s/def ::segment-segment_atclocktime (s/nilable ::boolean))
(s/def ::segment-segment_clocktime_offset ::int)
(s/def ::segment-segment_clocktime_wrap_duration ::int)
(s/def ::segment-segment_time ::string)
(s/def ::segment-segment_time_delta ::float)
(s/def ::segment-segment_times ::string)
(s/def ::segment-segment_frames ::string)
(s/def ::segment-segment_wrap ::int)
(s/def ::segment-segment_list_entry_prefix ::string)
(s/def ::segment-segment_start_number ::int)
(s/def ::segment-strftime (s/nilable ::boolean))
(s/def ::segment-increment_tc (s/nilable ::boolean))
(s/def ::segment-break_non_keyframes (s/nilable ::boolean))
(s/def ::segment-individual_header_trailer (s/nilable ::boolean))
(s/def ::segment-write_header_trailer (s/nilable ::boolean))
(s/def ::segment-reset_timestamps (s/nilable ::boolean))
(s/def ::segment-initial_offset ::string)
(s/def ::segment-write_empty_segments (s/nilable ::boolean))
(s/def ::segment (s/keys :opt-un [::segment-segment_format ::segment-segment_list ::segment-segment_list_flags ::segment-segment_list_size ::segment-segment_list_type ::segment-segment_atclocktime ::segment-segment_clocktime_offset ::segment-segment_clocktime_wrap_duration ::segment-segment_time ::segment-segment_time_delta ::segment-segment_times ::segment-segment_frames ::segment-segment_wrap ::segment-segment_list_entry_prefix ::segment-segment_start_number ::segment-strftime ::segment-increment_tc ::segment-break_non_keyframes ::segment-individual_header_trailer ::segment-write_header_trailer ::segment-reset_timestamps ::segment-initial_offset ::segment-write_empty_segments]))

;; Hardware Context Management
(s/def ::hwcontext-init_hw_device ::string)
(s/def ::hwcontext-filter_hw_device ::string)
(s/def ::hwcontext (s/keys :opt-un [::hwcontext-init_hw_device ::hwcontext-filter_hw_device]))

;; VAAPI (Video Acceleration API)
(s/def ::scale_vaapi-w ::string)
(s/def ::scale_vaapi-h ::string)
(s/def ::scale_vaapi-format ::string)
(s/def ::scale_vaapi-mode (s/nilable #{"default" "fast" "hq" "nl_anamorphic"}))
(s/def ::scale_vaapi-colour_primaries ::string)
(s/def ::scale_vaapi-colour_matrix ::string)
(s/def ::scale_vaapi-chroma_location ::string)
(s/def ::scale_vaapi (s/keys :opt-un [::scale_vaapi-w ::scale_vaapi-h ::scale_vaapi-format ::scale_vaapi-mode ::scale_vaapi-colour_primaries ::scale_vaapi-colour_matrix ::scale_vaapi-chroma_location]))

(s/def ::deinterlace_vaapi-mode (s/nilable #{"default" "bob" "weave" "motion_adaptive" "motion_compensated"}))
(s/def ::deinterlace_vaapi-rate (s/nilable #{"frame" "field"}))
(s/def ::deinterlace_vaapi-auto (s/nilable ::boolean))
(s/def ::deinterlace_vaapi (s/keys :opt-un [::deinterlace_vaapi-mode ::deinterlace_vaapi-rate ::deinterlace_vaapi-auto]))

(s/def ::denoise_vaapi-denoise ::int)
(s/def ::denoise_vaapi (s/keys :opt-un [::denoise_vaapi-denoise]))

(s/def ::sharpness_vaapi-sharpness ::int)
(s/def ::sharpness_vaapi (s/keys :opt-un [::sharpness_vaapi-sharpness]))

(s/def ::procamp_vaapi-brightness ::float)
(s/def ::procamp_vaapi-contrast ::float)
(s/def ::procamp_vaapi-hue ::float)
(s/def ::procamp_vaapi-saturation ::float)
(s/def ::procamp_vaapi (s/keys :opt-un [::procamp_vaapi-brightness ::procamp_vaapi-contrast ::procamp_vaapi-hue ::procamp_vaapi-saturation]))

;; QSV (Intel Quick Sync Video)
(s/def ::scale_qsv-w ::string)
(s/def ::scale_qsv-h ::string)
(s/def ::scale_qsv-format ::string)
(s/def ::scale_qsv-mode (s/nilable #{"low_power" "hq"}))
(s/def ::scale_qsv (s/keys :opt-un [::scale_qsv-w ::scale_qsv-h ::scale_qsv-format ::scale_qsv-mode]))

(s/def ::deinterlace_qsv-mode (s/nilable #{"bob" "advanced"}))
(s/def ::deinterlace_qsv (s/keys :opt-un [::deinterlace_qsv-mode]))

(s/def ::vpp_qsv-deinterlace (s/nilable #{"0" "1" "2"}))
(s/def ::vpp_qsv-denoise ::int)
(s/def ::vpp_qsv-detail ::int)
(s/def ::vpp_qsv-framerate ::string)
(s/def ::vpp_qsv-procamp ::int)
(s/def ::vpp_qsv-hue ::float)
(s/def ::vpp_qsv-saturation ::float)
(s/def ::vpp_qsv-brightness ::float)
(s/def ::vpp_qsv-contrast ::float)
(s/def ::vpp_qsv-transpose (s/nilable #{"0" "1" "2" "3"}))
(s/def ::vpp_qsv-cw ::string)
(s/def ::vpp_qsv-ch ::string)
(s/def ::vpp_qsv-cx ::string)
(s/def ::vpp_qsv-cy ::string)
(s/def ::vpp_qsv-w ::string)
(s/def ::vpp_qsv-h ::string)
(s/def ::vpp_qsv-format ::string)
(s/def ::vpp_qsv-async_depth ::int)
(s/def ::vpp_qsv-scale_mode (s/nilable #{"auto" "low_power" "hq"}))
(s/def ::vpp_qsv (s/keys :opt-un [::vpp_qsv-deinterlace ::vpp_qsv-denoise ::vpp_qsv-detail ::vpp_qsv-framerate ::vpp_qsv-procamp ::vpp_qsv-hue ::vpp_qsv-saturation ::vpp_qsv-brightness ::vpp_qsv-contrast ::vpp_qsv-transpose ::vpp_qsv-cw ::vpp_qsv-ch ::vpp_qsv-cx ::vpp_qsv-cy ::vpp_qsv-w ::vpp_qsv-h ::vpp_qsv-format ::vpp_qsv-async_depth ::vpp_qsv-scale_mode]))

;; Video Toolbox (macOS/iOS)
(s/def ::scale_vt-w ::string)
(s/def ::scale_vt-h ::string)
(s/def ::scale_vt-force_original_aspect_ratio (s/nilable #{"disable" "decrease" "increase"}))
(s/def ::scale_vt-force_divisible_by ::int)
(s/def ::scale_vt-color_matrix (s/nilable #{"auto" "bt709" "bt601" "smpte240m"}))
(s/def ::scale_vt-color_primaries (s/nilable #{"auto" "bt709" "bt470m" "bt470bg" "smpte170m" "smpte240m" "film" "bt2020"}))
(s/def ::scale_vt-color_transfer (s/nilable #{"auto" "bt709" "gamma22" "gamma28" "smpte170m" "smpte240m" "linear" "log" "log_sqrt" "iec61966_2_4" "bt1361_ecg" "iec61966_2_1" "bt2020_10" "bt2020_12" "smpte2084" "smpte428" "arib_std_b67"}))
(s/def ::scale_vt (s/keys :opt-un [::scale_vt-w ::scale_vt-h ::scale_vt-force_original_aspect_ratio ::scale_vt-force_divisible_by ::scale_vt-color_matrix ::scale_vt-color_primaries ::scale_vt-color_transfer]))

;; AMD AMF
(s/def ::scale_amf-w ::string)
(s/def ::scale_amf-h ::string)
(s/def ::scale_amf-format ::string)
(s/def ::scale_amf-algorithm (s/nilable #{"bilinear" "bicubic"}))
(s/def ::scale_amf (s/keys :opt-un [::scale_amf-w ::scale_amf-h ::scale_amf-format ::scale_amf-algorithm]))

;; NVENC (NVIDIA)
(s/def ::scale_nvenc-w ::string)
(s/def ::scale_nvenc-h ::string)
(s/def ::scale_nvenc-format ::string)
(s/def ::scale_nvenc-interp_algo (s/nilable #{"nearest" "linear" "cubic" "cubic2" "lanczos" "super"}))
(s/def ::scale_nvenc-force_original_aspect_ratio (s/nilable #{"disable" "decrease" "increase"}))
(s/def ::scale_nvenc-force_divisible_by ::int)
(s/def ::scale_nvenc (s/keys :opt-un [::scale_nvenc-w ::scale_nvenc-h ::scale_nvenc-format ::scale_nvenc-interp_algo ::scale_nvenc-force_original_aspect_ratio ::scale_nvenc-force_divisible_by]))

;; Hardware Memory Transfer
(s/def ::hwupload-derive_device ::string)
(s/def ::hwupload-extra_hw_frames ::int)
(s/def ::hwupload (s/keys :opt-un [::hwupload-derive_device ::hwupload-extra_hw_frames]))

(s/def ::hwdownload-derive_device ::string)
(s/def ::hwdownload (s/keys :opt-un [::hwdownload-derive_device]))

(s/def ::hwmap-mode (s/nilable #{"read" "write" "overwrite" "direct"}))
(s/def ::hwmap-derive_device ::string)
(s/def ::hwmap-reverse (s/nilable ::boolean))
(s/def ::hwmap (s/keys :opt-un [::hwmap-mode ::hwmap-derive_device ::hwmap-reverse]))
(s/def ::hstack (s/keys :opt-un [::inputs ::shortest]))
