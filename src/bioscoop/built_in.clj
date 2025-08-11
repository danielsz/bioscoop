(ns bioscoop.built-in
  (:refer-clojure :exclude [format concat])
  (:require
   [bioscoop.domain.records :refer [make-filter]]
   [bioscoop.domain.spec :as spec]
   [bioscoop.domain.specs.color :as color]
   [bioscoop.domain.specs.format :as format]
   [bioscoop.domain.specs.drawtext :as drawtext]
   [bioscoop.domain.specs.zoompan :as zoompan]
   [bioscoop.domain.specs.concat :as concat]
   [bioscoop.domain.specs.fade :as fade]
   [bioscoop.domain.specs.scale :as scale]
   [bioscoop.domain.specs.pad :as pad]
   [bioscoop.domain.specs.overlay :as overlay]
   [bioscoop.domain.specs.sources :as sources]
   [bioscoop.domain.specs.layout :as layout]
   [bioscoop.domain.specs.effects :as effects]
   [clojure.spec.alpha :as s]
   [clojure.tools.logging :as log]))

(defn template [arg spec]
  (if (seq arg)
    (if (map? (first arg))
      (let [m (first arg)]
        (if (s/valid? spec m)
          (make-filter (name spec) m)
          (s/explain-data spec m)))
      (let [formal-keys (last (s/form spec))
            m (zipmap formal-keys arg)]
        (if (s/valid? spec m)
          (make-filter (name spec) m)
          (s/explain-data spec m))))
    (make-filter (name spec))))

(defn crop [arg]
  (template arg ::spec/crop))

(defn scale [arg]
  (template arg ::scale/scale))

(defn fade [arg]
  (template arg ::fade/fade))

(defn overlay [arg]
  (template arg ::overlay/overlay))

(defn hflip [arg]
  (template arg ::spec/hflip))

(defn split [arg]
  (template arg ::spec/split))

(defn color [arg]
  (template arg ::color/color))

(defn format [arg]
  (template arg ::format/format))

(defn drawtext
  [arg]
  (template arg ::drawtext/drawtext))

(defn zoompan
  [arg]
  (template arg ::zoompan/zoompan))

(defn concat
  [arg]
  (template arg ::concat/concat))

(defn pad [arg]
  (template arg ::pad/pad))

(defn testsrc [arg]
  (template arg ::sources/testsrc))

(defn rgbtestsrc [arg]
  (template arg ::sources/rgbtestsrc))

(defn smptebars [arg]
  (template arg ::sources/smptebars))

(defn smptehdbars [arg]
  (template arg ::sources/smptehdbars))

(defn haldclutsrc [arg]
  (template arg ::sources/haldclutsrc))

(defn yuvtestsrc [arg]
  (template arg ::sources/yuvtestsrc))

(defn hstack [arg]
  (template arg ::layout/hstack))

(defn vstack [arg]
  (template arg ::layout/vstack))

(defn xstack [arg]
  (template arg ::layout/xstack))

(defn tile [arg]
  (template arg ::layout/tile))

(defn xfade [arg]
  (template arg ::effects/xfade))
