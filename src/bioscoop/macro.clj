(ns bioscoop.macro
  (:require [bioscoop.dsl :as dsl]
            [clojure.tools.logging :as log]
            [bioscoop.registry :as registry]))

(defn form->ast
  "Convert a Clojure form to the same AST structure that Instaparse produces"
  [form]
  (cond
    ;; Handle let bindings: (let [bindings...] body...)
    (and (seq? form) (= 'let (first form)))
    (let [[_ bindings & body] form
          ;; Convert binding vector to binding nodes
          binding-pairs (partition 2 bindings)
          binding-nodes (mapv (fn [[sym expr]]
                                [:binding
                                 (form->ast sym)
                                 (form->ast expr)])
                              binding-pairs)
          body-nodes (mapv form->ast body)]
      (vec (concat [:let-binding] binding-nodes body-nodes)))

    ;; Handle compose
    (and (seq? form) (= 'compose (first form)))
    (into [:compose "compose"] (mapv form->ast (rest form)))

    ;; Handle function calls and lists: (fn-name args...)
    (seq? form)
    (let [[op & args] form
          op-node (form->ast op)
          arg-nodes (mapv form->ast args)]
      (vec (concat [:list op-node] arg-nodes)))

    ;; Handle symbols
    (symbol? form)
    [:symbol (str form)]

    ;; Handle keywords
    (keyword? form)
    [:keyword (keyword form)]

    ;; Handle strings
    (string? form)
    [:string form]

    ;; Handle numbers
    (number? form)
    [:number (str form)]

    ;; Handle booleans
    (or (= form true) (= form false))
    [:boolean (str form)]

    ;; Handle vectors by treating them as binding vectors (for let syntax)
    (vector? form)
    form ; Return as-is, this handles binding vectors in let expressions

    (map? form)
    (let [kw (map form->ast (keys form))
          v (map form->ast (vals form))]
      (into [:map] (interleave kw v)))
    ;; Default: return the form as-is (for literals, etc.)
    :else
    form))

(defmacro bioscoop
  "Macro that takes Clojure DSL forms and produces the same AST as Instaparse parsing.
  
  Example:
  (bioscoop (let [width 1920] (scale width 1080)))
  
  This produces the same result as:
  (dsl/compile-dsl \"(let [width 1920] (scale width 1080))\")"
  [& forms]
  (let [ast-nodes (mapv form->ast forms)
        program-ast (vec (concat [:program] ast-nodes))]
    `(dsl/transform-ast ~program-ast (dsl/make-env))))

(defmacro defgraph [name & body]
  `(let [graph# (bioscoop ~@body)]
     (try (registry/register-graph! '~name graph#)
          (intern *ns* '~name graph#)
          (catch AssertionError e# (throw e#)))))


