(ns bioscoop.domain.specs.shared.framesync
  (:require [clojure.spec.alpha :as s]))

(s/def ::eof_action #{"repeat" "endall" "pass"})
(s/def ::shortest boolean?)
(s/def ::repeatlast boolean?)
(s/def ::ts_sync_mode #{"default" "nearest"})
