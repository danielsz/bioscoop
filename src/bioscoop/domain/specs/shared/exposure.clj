(ns bioscoop.domain.specs.shared.exposure
  (:require [clojure.spec.alpha :as s]))

(s/def ::exposure (s/double-in :min -3 :max 3))
