(ns bioscoop.domain.specs.shared.image-size
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::dimension
  (s/or :number pos-int?
        :expr string?
        :keyword #{"iw" "ih" "ow" "oh" "a" "dar" "sar"}))

(s/def ::image-size
  (s/or :string (s/and string? #(re-matches #"^\d+[x:]\d+$" %))
        :tuple (s/tuple ::dimension ::dimension)
        :kw-pair (s/tuple #{"iw" "ih" "ow" "oh" "a" "dar" "sar"} 
                          #{"iw" "ih" "ow" "oh" "a" "dar" "sar"})))


(defn image-size->str
  "Convert image size spec to FFmpeg string format"
  [size]
  (let [[type val] (s/conform ::image-size size)]
    (case type
      :string val
      :tuple (str/join "x" val)
      :kw-pair (str/join "x" val))))

