(ns bioscoop.domain.specs.lut
  (:require [clojure.spec.alpha :as s]))

(s/def ::c0 string?)
(s/def ::c1 string?)
(s/def ::c2 string?)
(s/def ::c3 string?)
(s/def ::y string?)
(s/def ::u string?)
(s/def ::v string?)
(s/def ::r string?)
(s/def ::g string?)
(s/def ::b string?)
(s/def ::a string?)

(s/def ::file string?)

(s/def :1d/interp #{"nearest" "linear" "cosine" "cubic" "spline"})
(s/def ::lut1d (s/keys :opt-un [::file :1d/interp]))

(s/def ::clut #{"first" "all"})
(s/def :3d/interp #{"nearest" "trilinear" "tetrahedral" "pyramid" "prism"})
(s/def ::lut3d (s/keys :opt-un [::file ::clut :3d/interp]))

(s/def ::lut (s/keys :opt-un [::c0 ::c1 ::c2 ::c3 ::y ::u ::v ::r ::g ::b ::a]))
(s/def ::lutrgb (s/keys :opt-un [::c0 ::c1 ::c2 ::c3 ::y ::u ::v ::r ::g ::b ::a]))
(s/def ::lutyuv (s/keys :opt-un [::c0 ::c1 ::c2 ::c3 ::y ::u ::v ::r ::g ::b ::a]))
