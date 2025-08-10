(ns bioscoop.domain.specs.scale
  (:require [clojure.spec.alpha :as s]))

;; Dimension parameters
(s/def ::width (s/or :int int? :string string?))   ; Output width
(s/def ::height (s/or :int int? :string string?))  ; Output height
(s/def ::size string?)                             ; Output size (WxH format)

;; Color processing
(s/def ::in_color_matrix #{"auto" "bt709" "fcc" "bt601" "bt470" "smpte170m" "smpte240m" "bt2020"})
(s/def ::out_color_matrix #{"auto" "bt709" "fcc" "bt601" "bt470" "smpte170m" "smpte240m" "bt2020"})
(s/def ::in_range #{"auto" "unknown" "jpeg" "full" "pc" "mpeg" "limited" "tv"})
(s/def ::out_range #{"auto" "unknown" "jpeg" "full" "pc" "mpeg" "limited" "tv"})
(s/def ::in_chroma_loc #{"auto" "unknown" "left" "center" "topleft" "top" "bottomleft" "bottom"})
(s/def ::out_chroma_loc #{"auto" "unknown" "left" "center" "topleft" "top" "bottomleft" "bottom"})


;; Aspect ratio handling
(s/def ::force_original_aspect_ratio #{"disable" "decrease" "increase"})
(s/def ::force_divisible_by pos-int?)

;; Evaluation parameters
(s/def ::eval #{"init" "frame"})                   ; Expression evaluation timing
(s/def ::flags string?)                            ; Scaling flags

;; Advanced parameters
(s/def ::interl (s/or :int int? :boolean boolean?)) ; Interlaced scaling
(s/def ::param0 number?)                           ; Scaler param 0
(s/def ::param1 number?)                           ; Scaler param 1
(s/def ::in_v_chr_pos int?)                        ; Input vertical chroma position
(s/def ::in_h_chr_pos int?)                        ; Input horizontal chroma position
(s/def ::out_v_chr_pos int?)                       ; Output vertical chroma position
(s/def ::out_h_chr_pos int?)                       ; Output horizontal chroma position

;; Compatibility parameters
(s/def ::sws_flags string?)                        ; Software scaler flags
(s/def ::src_range int?)                           ; Source color range
(s/def ::dst_range int?)                           ; Destination color range


(s/def ::scale
  (s/keys :opt-un [::width ::height ::eval ::flags ::interl
                   ::in_color_matrix ::out_color_matrix ::in_range ::out_range
                   ::in_chroma_loc ::out_chroma_loc ::force_original_aspect_ratio
                   ::force_divisible_by ::param0 ::param1 ::size
                   ::in_v_chr_pos ::in_h_chr_pos ::out_v_chr_pos ::out_h_chr_pos
                   ::sws_flags ::src_range ::dst_range]))
