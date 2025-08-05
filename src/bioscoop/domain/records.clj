(ns bioscoop.domain.records)

;; Core data structures for our DSL
(defrecord Filter [name args])
(defrecord FilterChain [filters])
(defrecord FilterGraph [chains])

;; Filter construction functions

(defn make-filter
  ([name] (->Filter name nil))
  ([name args] (->Filter name args)))

(defn make-filterchain [filters]
  (->FilterChain (vec filters)))

(defn make-filtergraph [chains]
  (->FilterGraph (vec chains)))

(defn chain-filters [& filters]
  (make-filterchain filters))

(defn parallel-filters [& chains]
  (make-filtergraph chains))
