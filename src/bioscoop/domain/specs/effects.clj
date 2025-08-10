(ns bioscoop.domain.specs.effects
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared
             [duration :as duration]
             [rational :as rational]
             [video-rate :as video-rate]
             [image-size :as image-size ]]))

(s/def ::transition #{"fade" "wipeleft" "wiperight" "wipeup" "wipedown" 
                      "slideleft" "slideright" "slideup" "slidedown"
                      "circlecrop" "rectcrop" "distance" "fadeblack"
                      "fadewhite" "radial" "smoothleft" "smoothright"
                      "smoothup" "smoothdown" "circleopen" "circleclose"
                      "vertopen" "vertclose" "horzopen" "horzclose"
                      "dissolve" "pixelize" "diagtl" "diagtr" "diagbl"
                      "diagbr" "hlslice" "hrslice" "vuslice" "vdslice" 
                      "hblur" "fadegrays" "wipetl" "wipetr" "wipebl"
                      "wipebr" "squeezev" "squeezeh" "zoomin" 
                      "fadefast" "fadeslow" "hlwind" "hrwind" "vuwind"
                      "vdwind" "coverleft" "coverright" "coverup" "coverdown"
                      "revealleft" "revealright" "revealup" "revealdown" "custom"})

(s/def ::offset ::duration/duration)
(s/def ::expr string?)

(s/def ::xfade (s/keys :opt-un [::transition ::duration/duration ::offset ::expr]))
