(ns bioscoop.dsl
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [bioscoop.domain.records :refer [make-filter make-filtergraph make-filterchain compose-filtergraphs]]
            [bioscoop.registry :as registry]
            [bioscoop.error-handling :refer [accumulate-error error-processing]])
  (:import [bioscoop.domain.records Filter FilterChain FilterGraph]))

(def dsl-parser (insta/parser (io/resource "lisp-grammar.bnf") :auto-whitespace :standard))

(def dsl-parses (partial insta/parses dsl-parser))

;; Environment for let bindings
(defn make-env
  ([] {:errors (atom [])})
  ([parent] (assoc {:errors (atom [])} :parent parent)))

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
  (let [defgraph-exprs (filter #(= :graph-definition (first %)) expressions)
        regular-exprs (remove #(= :graph-definition (first %)) expressions)
        transformed (mapv #(transform-ast % env) regular-exprs)]
    (doseq [defgraph-expr defgraph-exprs]
      (transform-ast defgraph-expr env))
    (case (count transformed)
      0 (make-filtergraph [])
      1 (let [single (first transformed)]
          (cond
            (instance? FilterGraph single) single
            (instance? FilterChain single) (make-filtergraph [single])
            (instance? Filter single) (make-filtergraph [(make-filterchain [single])])
            :else
            (do (accumulate-error env single :not-a-filtergraph)
                env)))
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
        (do (accumulate-error env transformed :bad-apple)
            env)))))

(defmethod transform-ast :compose [[_ & content] env]
  (let [children (mapv #(transform-ast % env) (rest content))]
    (apply compose-filtergraphs children)))

(defmethod transform-ast :graph-definition [[_ graph-name & body] env]
  (let [graph-name (transform-ast graph-name env)
        graph-body (into [:program] body)
        graph (transform-ast graph-body env)]
    (when (string? graph-name)
      (registry/register-graph! (symbol graph-name) graph))
    graph))

(defn padded-graph-helper [body]
  (loop [xs body
         result {:input [] :expr nil :output []}
         flag false]
    (if (empty? xs)
      result
      (let [[label _ :as x] (first xs)]
        (cond
          (and (= label :label) (not flag)) (recur (next xs) (update  result :input conj x) flag)
          (and (= label :label) flag) (recur (next xs) (update  result :output conj x) flag)
          :else (recur (next xs) (assoc result :expr x) true)))) ))

(defmethod transform-ast :padded-graph [[_ & body] env]
  (let [{:keys [input expr output]} (padded-graph-helper body)
        filtergraph (transform-ast expr env)
        f (fn [filters] (make-filtergraph [(make-filterchain (-> filters
                                                               (update 0 with-input-labels (mapv #(transform-ast % env) input))
                                                               (update (dec (count filters)) with-output-labels (mapv #(transform-ast % env) output))))]))]    
    (cond
      (instance? Filter filtergraph) (f [filtergraph])
      (instance? FilterChain filtergraph) (let [filters (.-filters filtergraph)]
                                            (f filters))
      (and (instance? FilterGraph filtergraph) (> 1 (count (.-chains filtergraph)))) (accumulate-error env filtergraph :padded-graph-multiple-filterchains)
      (instance? FilterGraph filtergraph) (let [filters (.-filters (first (.-chains filtergraph)))]
                                            (f filters))
      :else (accumulate-error env filtergraph :padded-graph-not-a-filtergraph))))

(defmethod transform-ast :let-binding [[_ & content] env]
  (let [bindings (take-while #(= :binding (first %)) content)
        body (drop (count bindings) content)
        validate (fn [sym] (when-let [resolved (ns-resolve 'bioscoop.built-in (symbol sym))]
                            (let [namespace (str (ns-name (:ns (meta resolved))))]
                              (case namespace
                                "clojure.core" (log/warn "You are binding a clojure.core name in the let binding. Caution advised")
                                "bioscoop.built-in" (accumulate-error env sym :reserved-word)))))
        new-env (reduce (fn [acc-env [_ [_ sym-name] expr]]
                          (validate sym-name)
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
                            (let [resolved (resolve-function transformed-op env)]
                              (resolved fn-args env))
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
      (into {} (map vec (partition 2 xs)))) ;; multiple arguments map
    ))

(defmethod transform-ast :symbol [[_ sym] env]
  (let [env-val (env-get env sym)
        graph-val (registry/get-graph (symbol sym))]
    (cond
      (and env-val graph-val) (throw (ex-info (str "Ambiguous symbol reference: '" sym "'\n"
                                                   "This symbol exists as both a local binding and a graph definition.\n"
                                                   "To resolve this ambiguity, please use a different name for either one of them.")
                                              {:symbol sym
                                               :local-value env-val
                                               :graph-value graph-val
                                               :type :ambiguous-symbol}))
      graph-val graph-val
      env-val env-val
      :else sym)))

(defmethod transform-ast :keyword [[_ kw] env]
  (keyword kw))

(defmethod transform-ast :string [[_ s] env]
  s)

(defmethod transform-ast :label [[_ label] env]
  label)

(defmethod transform-ast :number [[_ n] env]
  (if (str/includes? n ".")
    (Double/parseDouble n)
    (Long/parseLong n)))

(defmethod transform-ast :boolean [[_ b] env]
  (parse-boolean b))

(defn resolve-function [op env]
  (let [f (ns-resolve 'bioscoop.built-in (symbol op))]
    (case (str (:ns (meta f)))
      "bioscoop.built-in" f
      "clojure.core" (fn [arg _] (apply f arg))
      (do (accumulate-error env op :unresolved-function)
          (fn [_ _] ())))))

;; Compiler: DSL -> Clojure data structures
(defn compile-dsl [dsl-code]
  (let [ast (dsl-parser dsl-code)]
    (if (insta/failure? ast)
      (throw (ex-info "Parse error" {:error ast}))
      (let [result (transform-ast ast (make-env))]
        (if (instance? FilterGraph result)
          result
          (error-processing result))))))
