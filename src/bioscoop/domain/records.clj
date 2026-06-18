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
  (make-filtergraph (mapcat :chains filtergraphs)))

(defn promote-to-filtergraph* [f]
  (fn [x env]
    (cond
      (instance? FilterGraph x) x
      (instance? FilterChain x) (make-filtergraph [x])
      (instance? Filter x)      (make-filtergraph [(make-filterchain [x])])
      :else (f env x :not-a-filtergraph))))

(defn promote-to-filterchain* [f]
  (fn [x env]
    (cond
      (instance? Filter x) [x]
      (instance? FilterChain x) (:filters x)
      (instance? FilterGraph x) (if (= 1 (count (:chains x)))
                                  (:filters (first (:chains x)))
                                  (f env x :chain-parallel-filtergraph))
      :else (f env x :not-a-filtergraph))))
