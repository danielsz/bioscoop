(ns bioscoop.domain.specs.zoompan
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.image-size :as image-size]
            [bioscoop.domain.specs.shared.video-rate :as video-rate]))

(s/def ::zoom string?)   ; Zoom expression (default: "1")
(s/def ::z string?)      ; Zoom expression (shortcut alias for zoom)
(s/def ::x string?)      ; X coordinate expression (default: "0")
(s/def ::y string?)      ; Y coordinate expression (default: "0")
(s/def ::d string?)      ; Duration expression (default: "90")
(s/def ::s ::image-size/image-size)      ; Output size (default: "hd720")
(s/def ::fps ::video-rate/video-rate)    ; Output frame rate (default: "25")

(s/def ::zoompan
  (s/keys :opt-un [::zoom ::z ::x ::y ::d ::s ::fps]))
