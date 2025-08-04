(ns bioscoop.dsl
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [bioscoop.domain.records :refer [make-filter make-filtergraph make-filterchain]])
  (:import [bioscoop.domain.records Filter FilterChain FilterGraph]))

(def dsl-parser (insta/parser (io/resource "lisp-grammar.bnf") :auto-whitespace :standard))

(def dsl-parses (partial insta/parses dsl-parser))

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

;; Label metadata helper functions
(defn with-input-labels [filter labels]
    (log/debug filter labels)
  (with-meta filter (assoc (meta filter) :input-labels (vec labels))))

(defn with-output-labels [filter labels]
  (with-meta filter (assoc (meta filter) :output-labels (vec labels))))

(defn with-labels [filter input-labels output-labels]
  (-> filter
      (with-input-labels input-labels)
      (with-output-labels output-labels)))

(defn get-input-labels [filter]
  (:input-labels (meta filter) []))

(defn get-output-labels [filter]
  (:output-labels (meta filter) []))

(declare resolve-function)
(defmulti transform-ast (fn [node env]
                          (first node)))

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
        new-env (reduce (fn [acc-env [_ [_ sym-name] expr]]
                          (let [expr-val (transform-ast expr acc-env)]
                            (env-put acc-env sym-name expr-val)))
                        (make-env env)
                        bindings)
        transformed-body (mapv #(transform-ast % new-env) body)]
    (last transformed-body)))

(defmethod transform-ast :binding [[_ sym expr] env]
  ;; This shouldn't be called directly in normal flow
  [(transform-ast sym env) (transform-ast expr env)])

(defmethod transform-ast :list [[_ op & args] env]
  (let [transformed-op (transform-ast op env)
        transformed-args (mapv #(transform-ast % env) args)]
    (case transformed-op
      "filter" (let [[name & args] transformed-args
                     base-filter (if (seq args)
                                   (let [non-label-args (remove vector? args)]
                                     (apply (resolve-function name env) non-label-args))
                                   (make-filter name))
                     label-args (filter vector? args)]
                 (if (seq label-args)
                   (let [{:keys [input output]} (group-by (fn [x] (:labels (meta x))) label-args)]
                     (log/debug input output)
                     (cond-> base-filter
                       (seq input) (with-input-labels (apply concat input))
                       (seq output) (with-output-labels (apply concat output))))
                   base-filter))
      "chain" (make-filterchain transformed-args)
      "graph" (make-filtergraph transformed-args)
      ;; Default: resolve as function
      (apply (resolve-function transformed-op env) transformed-args))))

(defmethod transform-ast :map [[_ kw s] env]
  (let [k (transform-ast kw env)
        s (transform-ast s env)]
    (case k
      :input (with-meta [s] {:labels :input})
      :output (with-meta [s] {:labels :output}))))

(defmethod transform-ast :symbol [[_ sym] env]
  (or (env-get env sym) sym))

(defmethod transform-ast :keyword [[_ kw] env]
  (keyword kw))

(defmethod transform-ast :string [[_ s] env]
  s)

(defmethod transform-ast :number [[_ n] env]
  (if (str/includes? n ".")
    (Double/parseDouble n)
    (Long/parseLong n)))

(defmethod transform-ast :boolean [[_ b] env]
  (= "true" b))

(defn resolve-function [op env]
  (let [op-keyword (keyword op)]
    (case op-keyword
      ;; Built-in DSL functions (highest priority)
      :scale (fn [w h] (make-filter "scale" (str w ":" h)))
      :crop (fn [w h x y] (make-filter "crop" (str w ":" h ":" x ":" y)))
      :overlay (fn [] (make-filter "overlay"))
      :fade (fn [type start duration]
              (make-filter "fade" (str (name type) ":" start ":" duration)))

      ;; Label functions that return vectors directly
      :input-labels (fn [& labels] (with-meta (vec labels) {:labels :input}))
      :output-labels (fn [& labels] (with-meta (vec labels) {:labels :output}))

      ;; Try to resolve as Clojure function from clojure.core
      (if-let [clj-fn (try
                        (ns-resolve 'clojure.core (symbol op))
                        (catch Exception _ nil))]
        clj-fn
        ;; Default fallback: treat as filter name
        (fn [& args] (make-filter op (when (seq args) (str/join ":" args))))))))

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


