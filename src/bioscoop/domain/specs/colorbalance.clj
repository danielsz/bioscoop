(ns bioscoop.domain.specs.colorbalance
  (:require [clojure.spec.alpha :as s]))

(s/def ::rs (s/double-in :min -1 :max 1))
(s/def ::gs (s/double-in :min -1 :max 1))
(s/def ::bs (s/double-in :min -1 :max 1))
(s/def ::rm (s/double-in :min -1 :max 1))
(s/def ::gm (s/double-in :min -1 :max 1))
(s/def ::bm (s/double-in :min -1 :max 1))
(s/def ::rh (s/double-in :min -1 :max 1))
(s/def ::gh (s/double-in :min -1 :max 1))
(s/def ::bh (s/double-in :min -1 :max 1))

(s/def ::pl boolean?)
(s/def ::colorbalance (s/keys :opt-un [::rs ::gs ::bs ::rm ::gm ::bm ::rh ::gh ::bh ::pl]))
