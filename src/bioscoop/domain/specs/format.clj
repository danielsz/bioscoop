(ns bioscoop.domain.specs.format
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::color-range #{"unknown" "unspecified" "tv" "mpeg" "limited" "pc" "jpeg" "full"})

(s/def ::color-space
  #{"rgb" "bt709" "unspecified" "reserved" "fcc" "bt470bg"
    "smpte170m" "smpte240m" "ycgco" "bt2020nc" "bt2020c"
    "smpte2085" "chroma-derived-nc" "chroma-derived-c" "ictcp"})

(s/def ::pix_fmts 
  (s/and string? 
         #(re-matches #"^[a-zA-Z0-9_]+(?:\|[a-zA-Z0-9_]+)*$" %)))

(s/def ::color_spaces 
  (fn [s] (every? (partial s/valid? ::color-space)
                 (str/split s #"\|"))))

(s/def ::color_ranges 
  (fn [s] (every? (partial s/valid? ::color-range)
                 (str/split s #"\|"))))

(s/def ::format
  (s/keys :opt-un [::pix_fmts ::color_spaces ::color_ranges]))



