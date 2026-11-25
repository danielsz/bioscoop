(ns bioscoop.domain.specs.hwupload
  (:require [clojure.spec.alpha :as s]))

(s/def ::derive_device string?)

(s/def ::hwupload (s/keys :opt-un [::derive_device]))
