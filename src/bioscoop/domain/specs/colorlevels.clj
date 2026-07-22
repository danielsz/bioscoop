(ns bioscoop.domain.specs.colorlevels
  (:require [clojure.spec.alpha :as s]))

(s/def ::rimin (s/double-in :min -1 :max 1 :NaN false :infinite? false))
(s/def ::gimin (s/double-in :min -1 :max 1 :NaN false :infinite? false))
(s/def ::bimin (s/double-in :min -1 :max 1 :NaN false :infinite? false))
(s/def ::aimin (s/double-in :min -1 :max 1 :NaN false :infinite? false))

(s/def ::rimax (s/double-in :min -1 :max 1 :NaN false :infinite? false))
(s/def ::gimax (s/double-in :min -1 :max 1 :NaN false :infinite? false))
(s/def ::bimax (s/double-in :min -1 :max 1 :NaN false :infinite? false))
(s/def ::aimax (s/double-in :min -1 :max 1 :NaN false :infinite? false))

(s/def ::romin (s/double-in :min 0 :max 1 :NaN false :infinite? false))
(s/def ::gomin (s/double-in :min 0 :max 1 :NaN false :infinite? false))
(s/def ::bomin (s/double-in :min 0 :max 1 :NaN false :infinite? false))
(s/def ::aomin (s/double-in :min 0 :max 1 :NaN false :infinite? false))

(s/def ::romax (s/double-in :min 0 :max 1 :NaN false :infinite? false))
(s/def ::gomax (s/double-in :min 0 :max 1 :NaN false :infinite? false))
(s/def ::bomax (s/double-in :min 0 :max 1 :NaN false :infinite? false))
(s/def ::aomax (s/double-in :min 0 :max 1 :NaN false :infinite? false))

(s/def ::preserve #{"none" "lum" "max" "avg" "sum" "nrm" "pwr"})

(s/def ::colorlevels
  (s/keys :opt-un [::rimin ::gimin ::bimin ::aimin
                   ::rimax ::gimax ::bimax ::aimax
                   ::romin ::gomin ::bomin ::aomin
                   ::romax ::gomax ::bomax ::aomax
                   ::preserve]))
