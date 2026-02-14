(ns bioscoop.domain.specs.compand
  (:require [clojure.spec.alpha :as s]))

(s/def ::attacks string?)
(s/def ::decays string?)
(s/def ::points string?)

(s/def ::soft-knee (s/double-in :min 0.01 :max 900))
(s/def ::gain (s/double-in :min -900 :max 900))
(s/def ::volume (s/double-in :min -900 :max 0))
(s/def ::delay (s/double-in :min 0 :max 20))

(s/def ::compand (s/keys :opt-un [::attacks ::decays ::points ::soft-knee ::gain ::volume ::delay]))