(ns bioscoop.built-in
  (:require
   [bioscoop.domain.records :refer [make-filter]]
   [bioscoop.domain.spec :as spec]
   [clojure.spec.alpha :as s]
   [clojure.tools.logging :as log]))

(defn crop [arg]
  (if (map? (first arg))
    (let [m (first arg)]
      (if (s/valid? ::spec/crop m)
        (make-filter "crop" m)
        (s/explain-data ::spec/crop m)))
    (let [formal-keys (last (s/form ::spec/crop))]
      (make-filter "crop" (zipmap formal-keys arg)))))

(defn scale [arg]
  (if (map? (first arg))
    (let [m (first arg)]
      (if (s/valid? ::spec/scale m)
        (make-filter "scale" m)
        (s/explain-data ::spec/scale m)))
    (let [formal-keys (last (s/form ::spec/scale))]
      (make-filter "scale" (zipmap formal-keys arg)))))

(defn fade [arg]
  (if (map? (first arg))
    (let [m (first arg)]
      (if (s/valid? ::spec/fade m)
        (make-filter "fade" m)
        (s/explain-data ::spec/fade m)))
    (let [formal-keys (last (s/describe ::spec/fade))]
      (make-filter "fade" (zipmap formal-keys arg)))))
