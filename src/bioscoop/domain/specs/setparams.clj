(ns bioscoop.domain.specs.setparams
  (:require [clojure.spec.alpha :as s]))

(s/def ::field_mode #{"auto" "bff" "tff" "prog"})
(s/def ::range #{"auto" "unspecified" "unknown" "limited" "tv" "mpeg" "full" "pc" "jpeg"})
(s/def ::color_primaries #{"auto" "bt709" "unknown" "bt470m" "bt470bg" "smpte170m" "smpte240m" "film" "bt2020" "smpte428" "smpte431" "smpte432" "jedec-p22" "ebu3213"})
(s/def ::color_trc #{"auto" "bt709" "unknown" "bt470m" "bt470bg" "smpte170m" "smpte240m" "linear" "log100" "log316" "iec61966-2-4" "bt1361e" "iec61966-2-1" "bt2020-10" "bt2020-12" "smpte2084" "smpte428" "arib-std-b67"})
(s/def ::colorspace #{"auto" "gbr" "bt709" "unknown" "fcc" "bt470bg" "smpte170m" "smpte240m" "ycgco" "ycgco-re" "ycgco-ro" "bt2020nc" "bt2020c" "smpte2085" "chroma-derived-nc" "chroma-derived-c" "ictcp" "ipt-c2"})
(s/def ::chroma_location #{"auto" "left" "center" "topleft" "top" "bottomleft" "bottom"})

(s/def ::setparams (s/keys :opt-un [::field_mode ::range ::color_primaries
                                    ::color_trc ::colorspace ::chroma_location]))