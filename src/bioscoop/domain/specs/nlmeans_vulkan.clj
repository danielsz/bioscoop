(ns bioscoop.domain.specs.nlmeans-vulkan
  (:require [clojure.spec.alpha :as s]))

(s/def ::s (s/double-in :min 0 :max 100 :NaN false :infinite? false))
(s/def ::p (s/int-in 0 100))
(s/def ::r (s/int-in 0 100))
(s/def ::t (s/int-in 1 65))

(s/def ::s1 (s/double-in :min 0 :max 100 :NaN false :infinite? false))
(s/def ::s2 (s/double-in :min 0 :max 100 :NaN false :infinite? false))
(s/def ::s3 (s/double-in :min 0 :max 100 :NaN false :infinite? false))
(s/def ::s4 (s/double-in :min 0 :max 100 :NaN false :infinite? false))

(s/def ::p1 (s/int-in 0 100))
(s/def ::p2 (s/int-in 0 100))
(s/def ::p3 (s/int-in 0 100))
(s/def ::p4 (s/int-in 0 100))

(s/def ::nlmeans_vulkan
  (s/keys :opt-un [::s ::p ::r ::t
                   ::s1 ::s2 ::s3 ::s4
                   ::p1 ::p2 ::p3 ::p4]))
