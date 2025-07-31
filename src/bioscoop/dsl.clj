(ns bioscoop.dsl
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clojure.java.io :as io]))

;; Data structure specifications
(s/def ::name (s/and string? #(re-matches #"[a-zA-Z0-9_]+" %)))
(s/def ::filter-name (s/and string? #(re-matches #"[a-zA-Z0-9_]+(@[a-zA-Z0-9_]+)?" %)))
(s/def ::link-label (s/and string? #(re-matches #"[a-zA-Z0-9_]+" %)))
(s/def ::filter-args string?)

(s/def ::filter (s/keys :req-un [::filter-name]
                       :opt-un [::input-labels ::output-labels ::args]))
(s/def ::filterchain (s/coll-of ::filter))
(s/def ::filtergraph (s/coll-of ::filterchain))

(def whitespace
  (insta/parser
   "whitespace = #'\\s+'"))

(def dsl-parser (insta/parser (io/resource "lisp-grammar.bnf") :auto-whitespace whitespace))

;; Core data structures for our DSL
(defrecord Filter [name args input-labels output-labels])
(defrecord FilterChain [filters])
(defrecord FilterGraph [chains])

;; Environment for let bindings
(defn make-env
  ([] {})
  ([parent] (assoc {} :parent parent)))

(defn env-get [env sym]
  (if-let [val (get env sym)]
    val
    (when-let [parent (:parent env)]
      (env-get parent sym))))

(defn env-put [env sym val]
  (assoc env sym val))


;; Filter construction functions
(defn make-filter
  ([name] (->Filter name nil [] []))
  ([name args] (->Filter name args [] []))
  ([name args input-labels output-labels]
   (->Filter name args (or input-labels []) (or output-labels []))))

(defn make-filterchain [filters]
  (->FilterChain (vec filters)))

(defn make-filtergraph [chains]
  (->FilterGraph (vec chains)))

(defn make-input-labels [labels]
  (vec labels))  ; Just return a vector of strings

(defn make-output-labels [labels]
  (vec labels))  ; Just return a vector of strings

;; Corrected AST transformation functions
(declare resolve-function)
(defmulti transform-ast (fn [node env] 
  (if (vector? node) 
    (first node) 
    :literal)))

(defmethod transform-ast :program [[_ & expressions] env]
  (let [transformed (mapv #(transform-ast % env) expressions)]
    (case (count transformed)
      0 (make-filtergraph [])
      1 (let [single (first transformed)]
          (cond
            (instance? FilterGraph single) single
            (instance? FilterChain single) (make-filtergraph [single])
            (instance? Filter single) (make-filtergraph [(make-filterchain [single])])
            ;; Better error for invalid programs
            :else (throw (ex-info "DSL programs must produce filter operations, not primitive values" 
                                {:expr single 
                                 :type (type single)
                                 :hint "End your program with a filter, chain, or graph operation"}))))
      ;; Multiple expressions
      (if (every? #(or (instance? Filter %) 
                       (instance? FilterChain %) 
                       (instance? FilterGraph %)) transformed)
        (make-filtergraph
          (mapv #(cond
                   (instance? FilterChain %) %
                   (instance? Filter %) (make-filterchain [%])
                   (instance? FilterGraph %) (first (:chains %)))
                transformed))
        (throw (ex-info "All expressions in DSL program must produce filter operations"
                       {:expressions transformed
                        :hint "Each expression should create filters, chains, or graphs"}))))))


(defmethod transform-ast :let-binding [[_ & content] env]
  (let [bindings (take-while #(= :binding (first %)) content)
        body (drop (count bindings) content)
        new-env (reduce (fn [acc-env [_ sym expr]]
                         (let [sym-val (transform-ast sym env)
                               expr-val (transform-ast expr acc-env)] ; ← Only change needed
                           (env-put acc-env sym-val expr-val)))
                       (make-env env)
                       bindings)
        transformed-body (mapv #(transform-ast % new-env) body)]
    (last transformed-body)))

(defmethod transform-ast :binding [[_ sym expr] env]
  ;; This shouldn't be called directly in normal flow
  [(transform-ast sym env) (transform-ast expr env)])

(defmethod transform-ast :list [[_ op & args] env]
  ;; KEY FIX: Transform the operator first
  (let [transformed-op (transform-ast op env)
        transformed-args (mapv #(transform-ast % env) args)]
    (case transformed-op
      "filter" (apply make-filter transformed-args)
      "chain" (make-filterchain transformed-args)
      "graph" (make-filtergraph transformed-args)
      ;; Default: resolve as function
      (apply (resolve-function transformed-op env) transformed-args))))

(defmethod transform-ast :symbol [[_ sym] env]
  (or (env-get env sym) sym))  ; Returns string, not keyword

(defmethod transform-ast :keyword [[_ kw] env]
  kw)  ; Returns string (the keyword name)

(defmethod transform-ast :string [[_ s] env]
  s)

(defmethod transform-ast :number [[_ n] env]
  (if (str/includes? n ".")
    (Double/parseDouble n)
    (Long/parseLong n)))

(defmethod transform-ast :boolean [[_ b] env]
  (= "true" b))

;; Handle direct literals (not wrapped in vectors)
(defmethod transform-ast :literal [node env]
  node)

;; Fixed resolve-function that expects string ops
(defn resolve-function [op env]
  (let [op-keyword (keyword op)]  ; Convert string to keyword for case matching
    (case op-keyword
      :scale (fn [w h] (make-filter "scale" (str w ":" h)))
      :crop (fn [w h x y] (make-filter "crop" (str w ":" h ":" x ":" y)))
      :overlay (fn [] (make-filter "overlay"))
      :fade (fn [type start duration] 
             (make-filter "fade" (str (name type) ":" start ":" duration)))
      
      ;; Arithmetic and utility functions
      :str str
      :+ +
      :- -
      :/ /
      :* *
      
      ;; Default: treat as filter name
      (fn [& args] (make-filter op (when (seq args) (str/join ":" args)))))))

;; Compiler: DSL -> Clojure data structures
(defn compile-dsl [dsl-code]
  (let [ast (dsl-parser dsl-code)]
    (if (insta/failure? ast)
      (throw (ex-info "Parse error" {:error ast}))
      (transform-ast ast (make-env)))))

;; Add these for debugging parse trees and transformation
(defn debug-parse [input]
  (println "Input:" input)
  (let [ast (dsl-parser input)]
    (if (insta/failure? ast)
      (do (println "Parse error:")
          (clojure.pprint/pprint ast))
      (do (println "Parse tree:")
          (clojure.pprint/pprint ast)))))

(defn debug-transform [dsl-code]
  "Parse, transform, and print each step for debugging"
  (let [ast (dsl-parser dsl-code)]
    (if (insta/failure? ast)
      (println "Parse error:" ast)
      (do
        (println "Parse tree:")
        (clojure.pprint/pprint ast)
        (println "\nTransformed:")
        (let [result (transform-ast ast (make-env))]
          (clojure.pprint/pprint result)
          result)))))

;; Usage for debugging:
;; (debug-parse "(filter \"scale\" \"1920:1080\")")
;; (debug-transform "(filter \"scale\" \"1920:1080\")")


