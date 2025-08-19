(ns bioscoop.dsl
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [bioscoop.domain.records :refer [make-filter make-filtergraph make-filterchain join-filtergraphs compose+]]
            [bioscoop.registry :as registry])
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
            :else
            (if (and (map? single) (= :clojure.spec.alpha/problems (key (first single))))
              (throw (ex-info "Not a valid parameter" {:value (:clojure.spec.alpha/value single)                                                       
                                                       :problems (:clojure.spec.alpha/problems single)}))
              (throw (ex-info "Not a valid ffmpeg program (filtergraph)"
                              {:expr single
                               :type (type single)
                               :hint "End your program with a filter, chain, or graph operation"})))))
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

(defmethod transform-ast :compose [[_ & content] env]
  (let [children (mapv #(transform-ast % env) (rest content))]
    (apply compose+ children)))

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
  [(transform-ast sym env) (transform-ast expr env)])

(defmethod transform-ast :list [[_ op & args] env]
  (let [transformed-op (transform-ast op env)
        transformed-args (mapv #(transform-ast % env) args)]
    (case transformed-op
      "chain" (make-filterchain transformed-args)
      "graph" (make-filtergraph transformed-args)
      "input-labels" (with-meta (vec transformed-args) {:labels :input})
      "output-labels" (with-meta (vec transformed-args) {:labels :output})
      (let [base-filter (let [fn-args (remove vector? transformed-args)]
                          (if (seq fn-args)
                            ((resolve-function transformed-op) fn-args)
                            (make-filter transformed-op)))
            label-args (filter vector? transformed-args)]
        (if (seq label-args)
          (let [{:keys [input output]} (group-by (fn [x] (:labels (meta x))) label-args)]
            (cond-> base-filter
              (seq input) (with-input-labels (apply concat input))
              (seq output) (with-output-labels (apply concat output))))
          base-filter)))))

(defmethod transform-ast :map [[_ kw v :as m] env]
  (case (count (rest m))
    1 m ;; empty map
    2 (let [k (transform-ast kw env)
            v (transform-ast v env)]
        (case k
          :input (with-meta [v] {:labels :input})
          :output (with-meta [v] {:labels :output})
          {k v})) ;; one key-value pair
    (let [xs (map #(transform-ast % env) (rest m))]
      (into {}  (map vec (partition 2 xs)))) ;; multiple arguments map
    ))

  (defmethod transform-ast :symbol [[_ sym] env]
    (or (registry/get-graph (symbol sym)) (env-get env sym) sym))

(defmethod transform-ast :keyword [[_ kw] env]
  (keyword kw))

(defmethod transform-ast :string [[_ s] env]
  s)

(defmethod transform-ast :number [[_ n] env]
  (if (str/includes? n ".")
    (Double/parseDouble n)
    (Long/parseLong n)))

(defmethod transform-ast :boolean [[_ b] env]
  (parse-boolean b))

(defn resolve-function [op]
  (let [f (ns-resolve 'bioscoop.built-in (symbol op))]
    (case (str (:ns (meta f)))
      "bioscoop.built-in" f
      "clojure.core" (fn [arg] (apply f arg))
      (throw (ex-info "Cannot resolve function" {:name op})))))

;; Compiler: DSL -> Clojure data structures
(defn compile-dsl [dsl-code]
  (let [ast (dsl-parser dsl-code)]
    (if (insta/failure? ast)
      (throw (ex-info "Parse error" {:error ast}))
      (transform-ast ast (make-env)))))
