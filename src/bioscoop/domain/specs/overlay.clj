(ns bioscoop.domain.specs.overlay
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.framesync :as framesync]))

(s/def ::x (s/or :int int? :string string?))
(s/def ::y (s/or :int int? :string string?))
(s/def ::eval #{"init" "frame"})
(s/def ::format #{"yuv420" "yuv420p10" "yuv422" "yuv444" "yuv444p10" "rgb" "gbrp" "auto"})
(s/def ::alpha #{"straight" "premultiplied"}) ; Alpha format

(s/def ::overlay
  (s/keys :opt-un [::x ::y ::framesync/eof_action ::eval ::framesync/shortest ::format ::framesync/repeatlast ::alpha]))
