(ns bioscoop.built-in
  (:require
   [bioscoop.domain.records :refer [make-filter]]
   [bioscoop.domain.spec :as spec]
   [clojure.spec.alpha :as s]
   [clojure.tools.logging :as log]))


(defn template [arg spec]
  (if (map? (first arg))
    (let [m (first arg)]
      (if (s/valid? spec m)
        (make-filter (name spec) m)
        (s/explain-data spec m)))
    (let [formal-keys (last (s/form spec))]
      (make-filter (name spec) (zipmap formal-keys arg)))))

(defn crop [arg]
  (template arg ::spec/crop))

(defn scale [arg]
  (template arg ::spec/scale))

(defn fade [arg]
  (template arg ::spec/fade))
