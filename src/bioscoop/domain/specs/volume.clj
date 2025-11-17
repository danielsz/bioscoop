(ns bioscoop.domain.specs.volume
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.volume :as shared]))

(s/def ::precision #{"fixed" "float" "double"})
(s/def ::eval #{"once" "frame"})
(s/def ::replaygain #{"drop" "ignore" "track" "album"})
(s/def ::replaygain_preamp (s/double-in :min -15 :max 15))
(s/def ::replaygain_noclip boolean?)

(s/def ::volume (s/keys :opt-un [::shared/volume ::precision ::eval ::replaygain ::replaygain_preamp ::replaygain_noclip]))
