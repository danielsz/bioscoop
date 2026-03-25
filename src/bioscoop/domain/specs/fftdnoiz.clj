(ns bioscoop.domain.specs.fftdnoiz
  (:require [clojure.spec.alpha :as s]))

(s/def ::sigma (s/double-in :min 0 :max 100))
(s/def ::amount (s/double-in :min 0.01 :max 1))
(s/def ::block (s/int-in 8 257))
(s/def ::overlap (s/double-in :min 0.2 :max 0.8))
(s/def ::method #{"wiener" "hard"})
(s/def ::prev (s/int-in 0 2))
(s/def ::next (s/int-in 0 2))
(s/def ::planes (s/int-in 0 16))
(s/def ::window #{"rect" "bartlett" "hann" "hanning" "hamming" "blackman" "welch" "flattop" "bharris" "bnuttall" "bhann" "sine" "nuttall" "lanczos" "gauss" "tukey" "dolph" "cauchy" "parzen" "poisson" "bohman" "kaiser"})

(s/def ::fftdnoiz (s/keys :opt-un [::sigma ::amount ::block ::overlap ::method
                                   ::prev ::next ::planes ::window]))