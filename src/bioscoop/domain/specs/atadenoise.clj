(ns bioscoop.domain.specs.atadenoise
  (:require [clojure.spec.alpha :as s]))

(s/def ::0a (s/double-in :min 0 :max 0.3))
(s/def ::0b (s/double-in :min 0 :max 5))
(s/def ::1a (s/double-in :min 0 :max 0.3))
(s/def ::1b (s/double-in :min 0 :max 5))
(s/def ::2a (s/double-in :min 0 :max 0.3))
(s/def ::2b (s/double-in :min 0 :max 5))
(s/def ::s (s/int-in 5 130))
(s/def ::p string?)
(s/def ::a #{"p" "s"})
(s/def ::0s (s/double-in :min 0 :max 32767))
(s/def ::1s (s/double-in :min 0 :max 32767))
(s/def ::2s (s/double-in :min 0 :max 32767))

(s/def ::atadenoise (s/keys :opt-un [::0a ::0b ::1a ::1b ::2a ::2b
                                      ::s ::p ::a ::0s ::1s ::2s]))