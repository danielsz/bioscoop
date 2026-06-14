(ns bioscoop.dsl
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [bioscoop.domain.records :refer [make-filter make-filtergraph make-filterchain compose-filtergraphs with-input-labels with-output-labels]]
            [bioscoop.registry :as registry]
            [bioscoop.resolve :as r :refer [resolve-function]]
            [bioscoop.error-handling :refer [accumulate-error error-processing]])
  (:import [bioscoop.domain.records Filter FilterChain FilterGraph]))

(def dsl-parser (insta/parser (io/resource "lisp-grammar.bnf") :auto-whitespace :standard))

(def dsl-parses (partial insta/parses dsl-parser))

(defn promote-to-filtergraph [x env]
  (cond
    (instance? FilterGraph x) x
    (instance? FilterChain x) (make-filtergraph [x])
    (instance? Filter x)      (make-filtergraph [(make-filterchain [x])])
    :else (accumulate-error env x :not-a-filtergraph)))

(defn make-env
  ([] {:errors (atom [])})
  ([parent] (assoc {:errors (atom [])} :parent parent)))

(defn env-get [env sym]
  (if (contains? env sym)
    (get env sym)
    (when-let [parent (:parent env)]
      (env-get parent sym))))

(defn env-put [env sym val]
  (assoc env sym val))

(defmulti transform-ast (fn [node env]
                          (first node)))

(defmethod transform-ast :program [[_ & expressions] env]
  (let [defgraph-exprs (filter #(= :graph-definition (first %)) expressions)
        regular-exprs  (remove #(= :graph-definition (first %)) expressions)]
    (doseq [expr defgraph-exprs]
      (transform-ast expr env))
    (->> regular-exprs
         (mapv #(transform-ast % env))
         (mapv #(promote-to-filtergraph % env))
         (apply compose-filtergraphs))))

(defmethod transform-ast :compose [[_ & content] env]
  (let [children (->> (rest content)
                      (mapv #(transform-ast % env))
                      (mapv #(promote-to-filtergraph % env)))]
    (apply compose-filtergraphs children)))

(defmethod transform-ast :graph-definition [[_ [_ graph-name-str] & body] env]
  (let [graph-body (into [:program] body)
        graph      (transform-ast graph-body env)]
    (registry/register-graph! graph-name-str graph env)
    graph))

(defn padded-graph-helper [body]
  (loop [xs body
         result {:input [] :expr nil :output []}
         flag false]
    (if (empty? xs)
      result
      (let [[label _ :as x] (first xs)]
        (cond
          (and (= label :label) (not flag)) (recur (next xs) (update result :input conj x) flag)
          (and (= label :label) flag) (recur (next xs) (update result :output conj x) flag)
          :else (recur (next xs) (assoc result :expr x) true))))))

(defmethod transform-ast :padded-graph [[_ & body] env]
  (let [{:keys [input expr output]} (padded-graph-helper body)
        filtergraph (-> (transform-ast expr env)
                       (promote-to-filtergraph env))
        f (fn [filters]
            (make-filtergraph
              [(make-filterchain
                 (-> filters
                     (update 0 with-input-labels
                             (mapv #(transform-ast % env) input))
                     (update (dec (count filters)) with-output-labels
                             (mapv #(transform-ast % env) output))))]))]
    (cond
      (empty? (:chains filtergraph)) filtergraph
      (= 1 (count (:chains filtergraph))) (f (:filters (first (:chains filtergraph))))
      :else (accumulate-error env filtergraph :padded-graph-multiple-filterchains))))

(defmethod transform-ast :let-binding [[_ & content] env]
  (let [bindings (take-while #(= :binding (first %)) content)
        body (drop (count bindings) content)
        validate (fn [sym]
                   (when-let [reserved-type (r/reserved-word-type sym)]
                     (case reserved-type
                       :clojure-core (accumulate-error env sym :clj-reserved-word)
                       :built-in (accumulate-error env sym :reserved-word)
                       nil)))
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
      "if" (if (first transformed-args) (second transformed-args) (nth transformed-args 2 nil))
      (let [base-filter (let [fn-args (remove vector? transformed-args)]
                          (if (seq fn-args)
                            (let [resolved (resolve-function transformed-op env)]
                              (resolved fn-args env))
                            (make-filter transformed-op)))
            label-args (filter vector? transformed-args)]
        (if (and (seq label-args) (instance? Filter base-filter))
          (let [{:keys [input output]} (group-by #(:labels (meta %)) label-args)]
            (cond-> base-filter
              (seq input)  (with-input-labels  (apply concat input))
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

(defmethod transform-ast :for-binding [[_ [_ sym-name] range-node & body] env]
  (let [range-val (transform-ast range-node env)
        xs     (cond
                 (and (integer? range-val) (pos? range-val)) (range range-val)
                 (and (seqable? range-val) (not (string? range-val))) range-val                 
                 :else                                        [])]
    (apply compose-filtergraphs
           (for [x xs
                 :let [loop-env (env-put env sym-name x)]]
              (->> body
                  (mapv #(transform-ast % loop-env))
                  (mapv #(promote-to-filtergraph % loop-env))
                  (apply compose-filtergraphs))))))

(defmethod transform-ast :symbol [[_ sym] env]
  (let [env-val (env-get env sym)
        graph-val (registry/get-graph (symbol sym))]
    (cond
      (and env-val graph-val) (do (accumulate-error env sym :ambiguous-symbol)
                                  env-val)
      graph-val graph-val
      env-val env-val
      :else sym)))

(defmethod transform-ast :keyword [[_ kw] env]
  (keyword kw))

(defmethod transform-ast :string [[_ s] env]
  s)

(defmethod transform-ast :label [[_ content] env]
  (if (string? content)
    content
    (transform-ast content env)))

(defmethod transform-ast :number [[_ n] env]
  (if (str/includes? n ".")
    (Double/parseDouble n)
    (Long/parseLong n)))

(defmethod transform-ast :boolean [[_ b] env]
  (parse-boolean b))


;; Compiler: DSL -> Clojure data structures

(def last-errors
  "Errors from the most recent compile-dsl call. Inspect at the REPL after a failed compilation."
  (atom []))

;; in bioscoop.dsl
(defn run-ast [program-ast env]
  (let [result (transform-ast program-ast env)]
    (reset! last-errors @(:errors env))
    result))

(defn compile-dsl
  ([dsl-code]
   (compile-dsl dsl-code (make-env)))
  ([dsl-code env]
   (let [ast (dsl-parser dsl-code)]
     (if (insta/failure? ast)
       (throw (ex-info "Parse error" {:error ast}))
       (run-ast ast env)))))






