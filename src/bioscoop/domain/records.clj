(ns bioscoop.domain.records)

(defrecord Filter [name args input-labels output-labels])
(defrecord FilterChain [filters])
(defrecord FilterGraph [chains])

(defn make-filter
  ([name]
   (->Filter name nil [] []))
  ([name args]
   (->Filter name args [] []))
  ([name args input-labels output-labels]
   (->Filter name args
             (vec input-labels)
             (vec output-labels))))

(defn get-input-labels  [filter] (:input-labels  filter []))
(defn get-output-labels [filter] (:output-labels filter []))

(defn with-input-labels  [filter labels] (assoc filter :input-labels  (vec labels)))
(defn with-output-labels [filter labels] (assoc filter :output-labels (vec labels)))
(defn with-labels        [filter in out] (-> filter
                                             (with-input-labels  in)
                                             (with-output-labels out)))

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

(defn promote-to-filtergraph* [error-f]
  (fn promote [x env]
    (cond
      (instance? FilterGraph x) x
      (instance? FilterChain x) (make-filtergraph [x])
      (instance? Filter x)      (make-filtergraph [(make-filterchain [x])])
      (sequential? x)           (apply compose-filtergraphs (map #(promote % env) x))
      :else                     (error-f env x :not-a-filtergraph))))

(defn promote-to-filterchain* [error-f]
  (fn promote [x env]
    (cond
      (instance? Filter x) [x]
      (instance? FilterChain x) (:filters x)
      (instance? FilterGraph x) (if (= 1 (count (:chains x)))
                                  (:filters (first (:chains x)))
                                  (error-f env x :chain-parallel-filtergraph))
      (sequential? x) (vec (mapcat #(promote % env) x))
      :else (error-f env x :not-a-filtergraph))))
