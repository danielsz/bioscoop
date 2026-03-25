(ns bioscoop.domain.specs.arnndn
  (:require [clojure.spec.alpha :as s]))

(s/def ::model string?)
(s/def ::mix (s/double-in :min -1 :max 1))

(s/def ::arnndn (s/keys :opt-un [::model ::mix]))
