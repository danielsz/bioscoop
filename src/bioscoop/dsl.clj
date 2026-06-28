(ns bioscoop.dsl
  (:require [instaparse.core :as insta]
            [clojure.string :as str]
            [bioscoop.domain.records :refer [make-filter make-filtergraph make-filterchain compose-filtergraphs with-input-labels with-output-labels promote-to-filtergraph* promote-to-filterchain*]]
            [bioscoop.registry :as registry]
            [bioscoop.parse :refer [dsl-parser]]
            [bioscoop.env :refer [make-env env-get env-put]]
            [bioscoop.resolve :as r :refer [resolve-function]]
            [bioscoop.error-handling :refer [accumulate-error]]
            [bioscoop.trace :refer [trace>]]
            [clojure.tools.logging :as log])
  (:import [bioscoop.domain.records Filter]))


(declare transform-ast)

(def promote-to-filtergraph (promote-to-filtergraph* accumulate-error))
(def promote-to-filterchain (promote-to-filterchain* (partial accumulate-error [])))  ; must return [] since the result feeds into mapcat
(defmulti transform-ast* (fn [node env] (first node)))

(defmethod transform-ast* :program [[_ & expressions] env]
  (let [defgraph-exprs (filter #(= :graph-definition (first %)) expressions)
        regular-exprs  (remove #(= :graph-definition (first %)) expressions)]
    (doseq [expr defgraph-exprs]
      (transform-ast expr env))
    (->> regular-exprs
         (mapv #(transform-ast % env))
         (mapv #(promote-to-filtergraph % env))
         (apply compose-filtergraphs))))

(defmethod transform-ast* :compose [[_ & content] env]
  (let [children (->> (rest content)
                      (mapv #(transform-ast % env))
                      (mapv #(promote-to-filtergraph % env)))]
    (apply compose-filtergraphs children)))

(defmethod transform-ast* :graph-definition [[_ [_ graph-name-str] & body] env]
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

(defmethod transform-ast* :padded-graph [[_ & body] env]
  (let [{:keys [input expr output]} (padded-graph-helper body)
        filtergraph (-> (transform-ast expr env)
                       (promote-to-filtergraph env))
        f (fn [filters]
            (make-filtergraph
             [(make-filterchain
               (-> filters
                  (update 0 with-input-labels
                          (vec (mapcat (fn [node]
                                         (let [v (transform-ast node env)]
                                           (if (sequential? v) v [v])))
                                       input)))
                  (update (dec (count filters)) with-output-labels
                          (vec (mapcat (fn [node]
                                         (let [v (transform-ast node env)]
                                           (if (sequential? v) v [v])))
                                       output)))))]))]
    (cond
      (empty? (:chains filtergraph)) filtergraph
      (= 1 (count (:chains filtergraph))) (f (:filters (first (:chains filtergraph))))
      :else (accumulate-error env filtergraph :padded-graph-multiple-filterchains))))

(defmethod transform-ast* :let-binding [[_ & content] env]
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

(defmethod transform-ast* :binding [[_ sym expr] env]
  [(transform-ast sym env) (transform-ast expr env)])

(defmethod transform-ast* :list [[_ op & args] env]
  (let [transformed-op (transform-ast op env)
        transformed-args (mapv #(transform-ast % env) args)]
    (case transformed-op
      "chain" (make-filterchain (vec (mapcat #(promote-to-filterchain % env) transformed-args)))
      "if" (if (first transformed-args) (second transformed-args) (nth transformed-args 2 nil))
      "when" (if (first transformed-args) (second transformed-args) (make-filtergraph []))
      (let [resolved (resolve-function transformed-op env)]
        (if (seq transformed-args)
        (resolved transformed-args env)
        (resolved nil env))))))

(defmethod transform-ast* :map [[_ kw v :as m] env]
  (let [xs (map #(transform-ast % env) (rest m))]
      (into {} (map vec (partition 2 xs)))))

(defmethod transform-ast* :for-binding [[_ [_ sym-name] range-node body] env]
  (let [xs (transform-ast range-node env)]
    (vec (for [x xs
               :let [loop-env (env-put env sym-name x)]]
           (transform-ast body loop-env)))))

(defmethod transform-ast* :symbol [[_ sym] env]
  (let [env-val (env-get env sym)
        graph-val (registry/get-graph (symbol sym))]
    (cond
      (and env-val graph-val) (do (accumulate-error env sym :ambiguous-symbol)
                                  env-val)
      graph-val graph-val
      env-val env-val
      :else sym)))

(defmethod transform-ast* :keyword [[_ kw] env]
  (keyword kw))

(defmethod transform-ast* :string [[_ s] env]
  s)

(defmethod transform-ast* :label [[_ content] env]
  (if (string? content)
    content
    (transform-ast content env)))

(defmethod transform-ast* :number [[_ n] env]
  (if (str/includes? n ".")
    (Double/parseDouble n)
    (Long/parseLong n)))

(defmethod transform-ast* :boolean [[_ b] env]
  (parse-boolean b))

(def last-errors
  "Errors from the most recent compile-dsl call. Inspect at the REPL after a failed compilation."
  (atom []))

(defn transform-ast [node env]
  (trace> node env (transform-ast* node env)))

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

