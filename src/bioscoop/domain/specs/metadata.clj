(ns bioscoop.domain.specs.metadata
  (:require [clojure.spec.alpha :as s]))

(s/def ::mode #{"select" "add" "modify" "delete" "print"})
(s/def ::key string?)
(s/def ::value string?)
(s/def ::function #{"same_str" "starts_with" "less" "equal" "greater" "expr" "ends_with"})
(s/def ::expr string?)
(s/def ::file string?)
(s/def ::direct boolean?)

(s/def ::metadata (s/keys :opt-un [::mode ::key ::value ::function ::expr ::file ::direct]))
