(ns bioscoop.domain.specs.hwdownload
  (:require [clojure.spec.alpha :as s]))

(s/def ::hwdownload (s/keys :opt-un []))
