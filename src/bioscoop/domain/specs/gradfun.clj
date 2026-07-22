(ns bioscoop.domain.specs.gradfun
  (:require [clojure.spec.alpha :as s]))

(s/def ::strength (s/double-in :min 0.51 :max 64 :NaN false :infinite? false))
(s/def ::radius (s/int-in 4 33))

(s/def ::gradfun (s/keys :opt-un [::strength ::radius]))
