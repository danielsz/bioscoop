(ns bioscoop.domain.specs.afade
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared
             [duration :as duration]]))

(s/def ::type #{"in" "out"})
(s/def ::start_sample (s/int-in 0 Integer/MAX_VALUE))
(s/def ::nb_samples (s/int-in 0 Integer/MAX_VALUE))
(s/def ::start_time ::duration/duration)
(s/def ::duration ::duration/duration)
(s/def ::curve #{"nofade" "tri" "qsin" "esin" "hsin" "log" "ipar" "qua" "cub" "squ" "cbr" "par" "exp" "iqsin" "ihsin" "dese" "desi" "losi" "sinc" "isinc" "quat" "quatr" "qsin2" "hsin2"})
(s/def ::silence (s/double-in :min 0 :max 1))
(s/def ::unity (s/double-in :min 0 :max 1))

(s/def ::fade (s/keys :opt-un [::type ::start_sample ::nb_samples ::start_time ::duration ::curve ::silence ::unity]))
