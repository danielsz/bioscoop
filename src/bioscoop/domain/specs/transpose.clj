(ns bioscoop.domain.specs.transpose
  (:require [clojure.spec.alpha :as s]))


(s/def ::dir #{"cclock_flip" "clock" "cclock" "clock_flip"})
(s/def ::passthrough #{"none" "portrait" "landscape"})

(s/def ::transpose
  (s/keys :opt-un [::dir ::passthrough]))
