(ns bioscoop.domain.specs.sources
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared
             [duration :as duration]
             [rational :as rational]
             [video-rate :as video-rate]
             [image-size :as image-size ]]))

(s/def ::size ::image-size/image-size)
(s/def ::rate ::video-rate/video-rate)
(s/def ::sar ::rational/rational)
(s/def ::decimals (s/int-in 0 18))
(s/def ::complement boolean?)
(s/def ::level (s/int-in 1 17))


(s/def ::testsrc (s/keys :opt-un [::size ::rate ::duration/duration ::sar ::decimals]))

(s/def ::rgbtestsrc (s/keys :opt-un [::size ::rate ::duration/duration ::sar ::complement]))

(s/def ::smptebars (s/keys :opt-un [::size ::rate ::duration/duration ::sar]))

(s/def ::smptehdbars (s/keys :opt-un [::size ::rate ::duration/duration ::sar]))

(s/def ::haldclutsrc  (s/keys :opt-un [::level ::rate ::duration/duration ::sar]))

(s/def ::yuvtestsrc (s/keys :opt-un [::size ::rate ::duration/duration ::sar]))
