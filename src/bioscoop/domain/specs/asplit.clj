(ns bioscoop.domain.specs.asplit
  (:require [clojure.spec.alpha :as s]))

(s/def ::outputs (s/int-in 1 Integer/MAX_VALUE))
(s/def ::asplit (s/keys :opt-un [::outputs]))
