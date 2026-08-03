(ns bioscoop.macro
  (:require [bioscoop.dsl :as dsl]
            [bioscoop.config :as config]
            [bioscoop.env :refer [make-env env-put]]
            [bioscoop.resolve :as r :refer [reserved-word?]]))

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

    (and (seq? form) (= 'for (first form)))
    (let [[_ [sym range-expr] & body] form]
      (into [:for-binding
             [:symbol (str sym)]
             (form->ast range-expr)]
            (mapv form->ast body)))
    
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
    [:keyword form]

    ;; Handle strings
    (string? form)
    [:string form]

    ;; Handle numbers
    (number? form)
    [:number (str form)]

    ;; Handle booleans
    (or (= form true) (= form false))
    [:boolean (str form)]

    ;; Handle padded graphs
    (vector? form)
    (into [:padded-graph]
      (mapcat (fn [item]
                (if (vector? item)
                  (map (fn [label-form] [:label (form->ast label-form)]) item)
                  [(form->ast item)]))
              form))

    (map? form)
    (let [kw (map form->ast (keys form))
          v (map form->ast (vals form))]
      (into [:map] (interleave kw v)))
    ;; Default: return the form as-is (for literals, etc.)
    :else
    form))

(defmacro bioscoop
  "Macro that takes Clojure DSL forms and produces the same AST as Instaparse parsing.
   Binds *dynamic-resolution* to true for runtime reflection support.

   Example:
   (bioscoop (let [width 1920] (scale width 1080)))
  
   This produces the same result as:
   (dsl/compile-dsl \"(let [width 1920] (scale width 1080))\")

  Note: bioscoop only sees *lexical* locals in scope at the call site
   (let-bindings, fn params) — not top-level defs. Shadow a top-level var
   locally before referencing it inside a bioscoop form."
  [& forms]
  (let [ast-nodes (mapv form->ast forms)
        program-ast (vec (concat [:program] ast-nodes))
        locals (keys &env)]
    `(binding [config/*dynamic-resolution* true]
       (let [env# (reduce (fn [e# [k# v#]]
                            (env-put e# k# v#))
                          (make-env)
                          ~(mapv (fn [sym] [(str sym) sym]) locals))]
         (dsl/run-ast ~program-ast env#)))))

(defmacro defgraph [name & body]
  `(binding [config/*dynamic-resolution* true]
     (let [graph# (bioscoop ~@body)]
        (if (reserved-word? (str '~name))
          (println (str '~name " is a reserved word."))
          (intern *ns* '~name graph#)))))


