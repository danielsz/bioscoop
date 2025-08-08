(ns bioscoop.domain.specs.shared.image-size
  (:require [clojure.spec.alpha :as s]))

(s/def ::presets #{   "ntsc"      ; 720x480
   "pal"       ; 720x576
   "qntsc"     ; 352x240
   "qpal"      ; 352x288
   "sntsc"     ; 640x480
   "spal"      ; 768x576
   "film"      ; 352x240
   "ntsc-film" ; 352x240
   "sqcif"     ; 128x96
   "qcif"      ; 176x144
   "cif"       ; 352x288
   "4cif"      ; 704x576
   "16cif"     ; 1408x1152
   "qqvga"     ; 160x120
   "qvga"      ; 320x240
   "vga"       ; 640x480
   "svga"      ; 800x600
   "xga"       ; 1024x768
   "uxga"      ; 1600x1200
   "qxga"      ; 2048x1536
   "sxga"      ; 1280x1024
   "qsxga"     ; 2560x2048
   "hsxga"     ; 5120x4096
   "wvga"      ; 852x480
   "wxga"      ; 1366x768
   "wsxga"     ; 1600x1024
   "wuxga"     ; 1920x1200
   "woxga"     ; 2560x1600
   "wqsxga"    ; 3200x2048
   "wquxga"    ; 3840x2400
   "whsxga"    ; 6400x4096
   "whuxga"    ; 7680x4800
   "cga"       ; 320x200
   "ega"       ; 640x350
   "hd480"     ; 720x480
   "hd720"     ; 1280x720
   "hd1080"    ; 1920x1080
                   })

(s/def ::dimension
  (s/or :number pos-int?
        :expr string?
        :keyword #{"iw" "ih" "ow" "oh" "a" "dar" "sar"}))

(s/def ::image-size
  (s/or :string (s/and string? #(re-matches #"^\d+[x:]\d+$" %))
        :dimension ::dimension
        :preset ::presets))



