(ns bioscoop.domain.specs.fade
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.color :as color]))


;; Parameter types
(s/def ::type #{"in" "out"})              ; Fade type (default: "in")
(s/def ::start_frame (s/or :int int? :string string?)) ; Start frame (default: 0)
(s/def ::nb_frames (s/or :int int? :string string?))   ; Number of frames (default: 25)
(s/def ::alpha (s/or :int int? :boolean boolean?))     ; Alpha-only fade (default: 0/false)
(s/def ::start_time (s/or :number number? :string string?)) ; Start time in seconds
(s/def ::duration (s/or :number number? :string string?))   ; Duration in seconds
(s/def ::color ::color/color)                    ; Fade color (default: "black")
(s/def ::enable string?)                   ; Timeline expression to enable/disable

(s/def ::fade
  (s/keys :opt-un [::type ::start_frame ::nb_frames ::alpha 
                   ::start_time ::duration ::color ::enable]))
