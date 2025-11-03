(ns bioscoop.domain.specs.dctdnoiz
  (:require [clojure.spec.alpha :as s]))

(s/def ::sigma (s/double-in :min 0 :max 999))
(s/def ::overlap (s/int-in -1 16))
(s/def ::expr string?)
(s/def ::n (s/int-in 3 5))

(s/def ::dctdnoiz (s/keys :opt-un [::sigma ::overlap ::expr ::n]))
