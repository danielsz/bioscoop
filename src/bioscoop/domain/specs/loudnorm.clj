(ns bioscoop.domain.specs.loudnorm
  (:require [clojure.spec.alpha :as s]))

(s/def ::I (s/double-in :min -70 :max -5))
(s/def ::LRA (s/double-in :min 1 :max 50))
(s/def ::TP (s/double-in :min -9 :max 0))
(s/def ::measured_I (s/double-in :min -99 :max 0))
(s/def ::measured_LRA (s/double-in :min 0 :max 99))
(s/def ::measured_TP (s/double-in :min -99 :max 99))
(s/def ::measured_thresh (s/double-in :min -99 :max 0))
(s/def ::offset (s/double-in :min -99 :max 99))
(s/def ::linear boolean?)
(s/def ::dual_mono boolean?)
(s/def ::print_format #{"none" "json" "summary"})

(s/def ::loudnorm (s/keys :opt-un [::I ::LRA ::TP ::measured_I ::measured_LRA
                                   ::measured_TP ::measured_thresh ::offset
                                   ::linear ::dual_mono ::print_format]))