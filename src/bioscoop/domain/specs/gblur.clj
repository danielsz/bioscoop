(ns bioscoop.domain.specs.gblur
  (:require [clojure.spec.alpha :as s]))

(s/def ::sigma (s/double-in :min 0 :max 1024))
(s/def ::steps (s/int-in 1 7))
(s/def ::planes (s/int-in 0 16) )
(s/def ::sigmaV (s/double-in :min -1 :max 1024))

(s/def ::gblur
  (s/keys :opt-un [::sigma ::steps ::planes ::sigmaV]))
