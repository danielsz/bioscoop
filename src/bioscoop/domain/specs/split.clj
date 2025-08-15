(ns bioscoop.domain.specs.split
  (:require [clojure.spec.alpha :as s]))


(s/def ::outputs pos-int?)   ; Number of output streams (default: 2)

(s/def ::split
  (s/keys :opt-un [::outputs]))
