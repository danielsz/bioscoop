(ns bioscoop.domain.specs.lagfun
  (:require [clojure.spec.alpha :as s]))

(s/def ::decay (s/double-in :min 0 :max 1))
(s/def ::planes (s/int-in 0 16))

(s/def ::lagfun (s/keys :opt-un [::decay ::planes]))
