(ns bioscoop.domain.specs.chromanr
  (:require [clojure.spec.alpha :as s]))

(s/def ::thres (s/double-in :min 1 :max 200 :NaN false :infinite? false))
(s/def ::sizew (s/int-in 1 101))
(s/def ::sizeh (s/int-in 1 101))
(s/def ::stepw (s/int-in 1 51))
(s/def ::steph (s/int-in 1 51))
(s/def ::threy (s/double-in :min 1 :max 200 :NaN false :infinite? false))
(s/def ::threu (s/double-in :min 1 :max 200 :NaN false :infinite? false))
(s/def ::threv (s/double-in :min 1 :max 200 :NaN false :infinite? false))
(s/def ::distance #{"manhattan" "euclidean"})

(s/def ::chromanr
  (s/keys :opt-un [::thres ::sizew ::sizeh ::stepw ::steph
                   ::threy ::threu ::threv ::distance]))
