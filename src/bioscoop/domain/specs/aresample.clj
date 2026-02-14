(ns bioscoop.domain.specs.aresample
  (:require [clojure.spec.alpha :as s]))

(s/def ::sample_rate (s/int-in 0 Integer/MAX_VALUE))
(s/def ::in_sample_rate ::sample_rate)
(s/def ::out_sample_rate ::sample_rate)

(s/def ::in_sample_fmt string?)
(s/def ::out_sample_fmt string?)
(s/def ::internal_sample_fmt string?)

(s/def ::in_chlayout string?)
(s/def ::out_chlayout string?)
(s/def ::used_chlayout string?)

(s/def ::center_mix_level (s/double-in :min -32 :max 32))
(s/def ::surround_mix_level (s/double-in :min -32 :max 32))
(s/def ::lfe_mix_level (s/double-in :min -32 :max 32))

(s/def ::rematrix_volume (s/double-in :min -1000 :max 1000))
(s/def ::rematrix_maxval (s/double-in :min 0 :max 1000))

(s/def ::flags string?)
(s/def ::swr_flags ::flags)

(s/def ::dither_scale (s/double-in :min 0 :max Integer/MAX_VALUE))
(s/def ::dither_method (s/int-in 0 71))
; Note: dither_method has specific named values but we'll keep the range for flexibility

(s/def ::filter_size (s/int-in 0 Integer/MAX_VALUE))
(s/def ::phase_shift (s/int-in 0 24))
(s/def ::linear_interp boolean?)
(s/def ::exact_rational boolean?)

(s/def ::cutoff (s/double-in :min 0 :max 1))
(s/def ::resample_cutoff ::cutoff)

(s/def ::resampler #{"swr" "soxr"})
(s/def ::precision (s/double-in :min 15 :max 33))
(s/def ::cheby boolean?)

(s/def ::min_comp float?)
(s/def ::min_hard_comp float?)
(s/def ::comp_duration float?)
(s/def ::max_soft_comp float?)
(s/def ::async float?)
(s/def ::first_pts int?)

(s/def ::matrix_encoding #{"none" "dolby" "dplii"})

(s/def ::filter_type #{"cubic" "blackman_nuttall" "kaiser"})

(s/def ::kaiser_beta (s/double-in :min 2 :max 16))
(s/def ::output_sample_bits (s/int-in 0 64))

(s/def ::aresample (s/keys :opt-un [::sample_rate ::in_sample_rate ::out_sample_rate
                                    ::in_sample_fmt ::out_sample_fmt ::internal_sample_fmt
                                    ::in_chlayout ::out_chlayout ::used_chlayout
                                    ::center_mix_level ::surround_mix_level ::lfe_mix_level
                                    ::rematrix_volume ::rematrix_maxval
                                    ::flags ::swr_flags
                                    ::dither_scale ::dither_method
                                    ::filter_size ::phase_shift ::linear_interp ::exact_rational
                                    ::cutoff ::resample_cutoff
                                    ::resampler ::precision ::cheby
                                    ::min_comp ::min_hard_comp ::comp_duration ::max_soft_comp ::async ::first_pts
                                    ::matrix_encoding
                                    ::filter_type ::kaiser_beta ::output_sample_bits]))