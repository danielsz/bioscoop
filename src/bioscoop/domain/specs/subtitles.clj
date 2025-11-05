(ns bioscoop.domain.specs.subtitles
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.image-size :as image-size]))

(s/def ::filename string?)
(s/def ::original_size ::image-size/image-size)
(s/def ::fontsdir string?)
(s/def ::alpha boolean?)
(s/def ::charenc string?)
(s/def ::stream_index (s/int-in -1 Integer/MAX_VALUE))
(s/def ::force_style string?)
(s/def ::wrap_unicode boolean?)
(s/def ::shaping #{"auto" "simple" "complex"})

(s/def ::subtitles (s/keys :opt-un [::filename ::original_size ::fontsdir ::alpha ::charenc ::stream_index ::force_style ::wrap_unicode]))
(s/def ::ass (s/keys :opt-un [::filename ::original_size ::fontsdir ::alpha ::shaping]))
