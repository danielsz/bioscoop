(ns bioscoop.domain.specs.afir
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.image-size :as image-size]))

;; Dry gain (0-10)
(s/def ::dry (s/double-in :min 0 :max 10))

;; Wet gain (0-10)
(s/def ::wet (s/double-in :min 0 :max 10))

;; IR length (0-1)
(s/def ::length (s/double-in :min 0 :max 1))

;; IR auto gain type enum
(s/def ::gtype #{"none" "peak" "dc" "gn" "ac" "rms"})

;; IR norm (-1 to 2)
(s/def ::irnorm (s/double-in :min -1 :max 2))

;; IR link (boolean)
(s/def ::irlink boolean?)

;; IR gain (0-1)
(s/def ::irgain (s/double-in :min 0 :max 1))

;; IR format enum
(s/def ::irfmt #{"mono" "input"})

;; Max IR length (0.1-60)
(s/def ::maxir (s/double-in :min 0.1 :max 60))

;; Show IR frequency response (boolean)
(s/def ::response boolean?)

;; IR channel to display frequency response (0-1024)
(s/def ::channel (s/int-in 0 1025))

;; Video size (image_size)
(s/def ::size ::image-size/image-size)

;; Video rate (string)
(s/def ::rate string?)

;; Min partition size (1-65536)
(s/def ::minp (s/int-in 1 65537))

;; Max partition size (8-65536)
(s/def ::maxp (s/int-in 8 65537))

;; Number of input IRs (1-32)
(s/def ::nbirs (s/int-in 1 33))

;; Select IR (0-31)
(s/def ::ir (s/int-in 0 32))

;; Processing precision enum
(s/def ::precision #{"auto" "float" "double"})

;; IR loading type enum
(s/def ::irload #{"init" "access"})

;; Main afir filter spec
(s/def ::afir (s/keys :opt-un [::dry ::wet ::length ::gtype ::irnorm ::irlink ::irgain ::irfmt ::maxir ::response ::channel ::size ::rate ::minp ::maxp ::nbirs ::ir ::precision ::irload]))