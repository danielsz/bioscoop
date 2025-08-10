(ns bioscoop.domain.specs.overlay
  (:require [clojure.spec.alpha :as s]))


;; Parameter types
(s/def ::x (s/or :int int? :string string?))   ; Horizontal position
(s/def ::y (s/or :int int? :string string?))   ; Vertical position
(s/def ::eof_action #{"repeat" "endall" "pass"}) ; EOF handling
(s/def ::eval #{"init" "frame"})               ; Expression evaluation timing
(s/def ::shortest boolean?)                    ; Terminate on shortest stream
(s/def ::format #{"yuv420" "yuv422" "yuv444" "rgb" "gbrp" "auto"}) ; Pixel format
(s/def ::repeatlast boolean?)
(s/def ::alpha #{"straight" "premultiplied"}) ; Alpha format


(s/def ::overlay
  (s/keys :opt-un [::x ::y ::eof_action ::eval ::shortest ::format ::repeatlast ::alpha]))
