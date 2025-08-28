(ns bioscoop.domain.specs.geq
  (:require [clojure.spec.alpha :as s]))

(s/def ::lum_expr string?)
(s/def ::cb_expr string?)
(s/def ::cr_expr string?)
(s/def ::alpha_expr string?)
(s/def ::red_expr string?)
(s/def ::green_expr string?)
(s/def ::blue_expr string?)
(s/def ::interpolation #{"nearest" "bilinear"})
(s/def ::geq (s/keys :opt-un [::lum_expr ::cb_expr ::cr_expr ::alpha_expr ::red_expr ::green_expr ::blue_expr ::interpolation]))
