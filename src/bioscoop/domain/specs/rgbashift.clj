(ns bioscoop.domain.specs.rgbashift
  (:require [clojure.spec.alpha :as s]))

(s/def ::edge #{"smear" "wrap"})
(s/def ::rh (s/int-in -255 256))
(s/def ::rg (s/int-in -255 256))
(s/def ::gh (s/int-in -255 256))
(s/def ::gv (s/int-in -255 256))
(s/def ::bh (s/int-in -255 256))
(s/def ::bv (s/int-in -255 256))
(s/def ::ah (s/int-in -255 256))
(s/def ::av (s/int-in -255 256))

(s/def ::rgbashift (s/keys :opt-un [::rh ::rg ::gh ::gv ::bh ::bv ::ah ::av ::edge]))
