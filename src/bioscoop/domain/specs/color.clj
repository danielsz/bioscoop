(ns bioscoop.domain.specs.color
  (:require [clojure.spec.alpha :as s]
            [bioscoop.domain.specs.shared.color]
            [bioscoop.domain.specs.shared.image-size :as image-size]
            [bioscoop.domain.specs.shared.video-rate :as video-rate]
            [bioscoop.domain.specs.shared.duration :as duration]
            [bioscoop.domain.specs.shared.rational :as rational]))

(s/def ::size ::image-size/image-size)    
(s/def ::rate ::video-rate/video-rate)   
(s/def ::duration ::duration/duration)  
(s/def ::sar ::rational/rational)        

(s/def ::color
  (s/keys :opt-un [:bioscoop.domain.specs.shared.color/color
                   ::size 
                   ::rate 
                   ::duration 
                   ::sar]))


