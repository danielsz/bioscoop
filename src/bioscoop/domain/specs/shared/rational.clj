(ns bioscoop.domain.specs.shared.rational
  (:require [clojure.spec.alpha :as s]))

(s/def ::rational
  (s/or :decimal (s/and double? #(not= 0 %))
        :expr string?))





