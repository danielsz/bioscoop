(ns bioscoop.built-in
  (:require
   [bioscoop.domain.records :refer [make-filter]]
   [bioscoop.domain.spec :as spec]
   [bioscoop.domain.specs.color :as color]
   [clojure.spec.alpha :as s]
   [clojure.tools.logging :as log]))

(defn template [arg spec]
  (if (seq arg)
    (if (map? (first arg))
      (let [m (first arg)]
        (if (s/valid? spec m)
          (make-filter (name spec) m)
          (s/explain-data spec m)))
      (let [formal-keys (last (s/form spec))]
        (make-filter (name spec) (zipmap formal-keys arg))))
    (make-filter (name spec))))

(defn crop [arg]
  (template arg ::spec/crop))

(defn scale [arg]
  (template arg ::spec/scale))

(defn fade [arg]
  (template arg ::spec/fade))

(defn overlay [arg]
  (template arg ::spec/overlay))

(defn hflip [arg]
  (template arg ::spec/hflip))

(defn split [arg]
  (template arg ::spec/split))

(defn color [arg]
  (template arg ::color/color))
