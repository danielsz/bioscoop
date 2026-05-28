(ns bioscoop.domain.specs.fade
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.color :as color]
            [bioscoop.domain.specs.shared
             [duration :as duration]]))


;; Parameter types
(s/def ::type #{"in" "out"})
(s/def ::start_frame (s/or :int int? :string string?))
(s/def ::nb_frames (s/or :int int? :string string?))
(s/def ::alpha (s/or :int int? :boolean boolean?))
(s/def ::start_time ::duration/duration)
(s/def ::color ::color/color)
(s/def ::enable string?)

(s/def ::fade
  (s/keys :opt-un [::type ::start_frame ::nb_frames ::alpha 
                   ::start_time ::duration/duration ::color ::enable]))
