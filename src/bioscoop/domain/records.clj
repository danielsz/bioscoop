(ns bioscoop.domain.records)

(declare join-filtergraphs)
(defprotocol Composable
  (compose [this other]))
(defprotocol Sinkable
  (with-labels [this left right])
  (with-input-labels [this labels])
  (with-output-labels [this labels])
  (get-input-labels [this])
  (get-output-labels [this]))

;; Core data structures for our DSL
(defrecord Filter [name args]
  Sinkable
  (with-input-labels [this labels]
    (with-meta this (assoc (meta this) :input-labels (vec labels))))
  (with-output-labels [this labels]
    (with-meta this (assoc (meta this) :output-labels (vec labels))))
  (with-labels [this input-labels output-labels]
  (-> this
      (with-input-labels input-labels)
      (with-output-labels output-labels)))
  (get-input-labels [this]
    (:input-labels (meta this) []))
  (get-output-labels [this]
    (:output-labels (meta this) [])))

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

(defn compose-filtergraphs [& filtergraphs]
  (make-filtergraph (mapcat #(.-chains %) filtergraphs)))
