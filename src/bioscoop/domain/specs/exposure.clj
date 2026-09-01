(ns bioscoop.domain.specs.exposure
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.exposure :as shared]))

(s/def ::black (s/double-in :min -1 :max 1))
(s/def ::exposure (s/keys :opt-un [::shared/exposure ::black]))
