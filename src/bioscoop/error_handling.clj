(ns bioscoop.error-handling
  (:require [clojure.spec.alpha :as s]
            [clojure.walk :refer [postwalk]]
            [clojure.tools.logging :as log]
            [bioscoop.domain.records :refer [make-filtergraph]]
            [bioscoop.config :refer [*debug-mode* *warn-verbose*]]))

(def errors {:not-a-filtergraph (fn [sym]
                                  (ex-info "Expression does not produce a filtergraph"
                                           {:value sym
                                            :value-type (type sym)
                                            :error-type :not-a-filtergraph
                                            :explanation "Every Bioscoop expression must produce a filter, chain, or graph"}))
             :reserved-word (fn [sym] (let [explanation (str "Reserved word: '" sym "'\n"
                                                         "This is the name of an existing ffmpeg filter and is reserved. Please use a different name")]
                                       (ex-info explanation
                                                {:symbol sym
                                                 :error-type :reserved-word
                                                 :explanation explanation})))
             :clj-reserved-word  (fn [sym] (let [explanation (str "Reserved word: '" sym "'\n"
                                                                 "You are binding a clojure.core name in the let binding. Caution advised")]
                                       (ex-info explanation
                                                {:symbol sym
                                                 :error-type :clj-reserved-word
                                                 :explanation explanation})))
             :unresolved-function (fn [sym] (ex-info "Cannot resolve function" {:error-type :unresolved-function
                                                                                :explanation "Cannot resolve function"
                                                                                :symbol sym }))
             :not-implemented (fn [sym] (ex-info "Not implemented" {:error-type :not-implemented
                                                                   :explanation "This filter is not yet implemented."
                                                                   :symbol sym}))
             :invalid-parameter (fn [sym spec] (ex-info "Not a valid parameter" {:symbol sym
                                                                                :error-type :invalid-parameter
                                                                                :explanation (s/explain-str spec sym)
                                                                                :explanation-data (s/explain-data spec sym)}))
             :padded-graph-multiple-filterchains (fn [sym] (ex-info "You can only label pads on a filtergraph that consists of one and only one filterchain."
                                                                   {:symbol sym
                                                                    :error-type :padded-graph
                                                                    :explanation "Multiple filterchains found. You can only label pads one filterchain at the time"}))
             :padded-graph-empty-chain (fn [sym] (ex-info "Cannot label pads on an empty filterchain"
                                                         {:symbol sym
                                                          :error-type :padded-graph-empty-chain
                                                          :explanation "The expression inside this padded graph produced a filterchain with zero filters, so there's nothing to attach input/output pad labels to."}))
             :ambiguous-symbol (fn [sym] (ex-info (str "Ambiguous symbol reference: '" sym "'\n")
                                                 {:symbol sym
                                                  :error-type :ambiguous-symbol
                                                  :explanation "This symbol exists as both a local binding and a graph definition. To resolve this ambiguity, please use a different name for either one of them."}))
             :unscoped-top-level-var (fn [sym]
                                       (let [msg (str "Symbol '" sym "' refers to a top-level Var, but its value is "
                                                      "plain data (not a filtergraph, not a function) — so it isn't "
                                                      "visible from inside this bioscoop expression.\n\n"
                                                      "`bioscoop` only auto-captures *lexical* locals (let-bindings, "
                                                      "fn parameters) present at the (bioscoop ...) call site; it "
                                                      "cannot see top-level defs.\n\n"
                                                      "Fix — shadow it locally:\n"
                                                      "  (let [" sym " " sym "] (bioscoop ... " sym " ...))\n"
                                                      "or receive it as a parameter:\n"
                                                      "  (defn my-graph [" sym "] (bioscoop ... " sym " ...))")]
                                         (ex-info msg {:symbol sym :error-type :unscoped-top-level-var :explanation msg})))
             :chain-parallel-filtergraph (fn [sym] (ex-info "Cannot use a parallel filtergraph inside chain"
                                                           {:value       sym
                                                            :error-type  :chain-parallel-filtergraph
                                                            :explanation "chain requires linear filter sequences. A filtergraph with multiple chains represents parallel structure that cannot be flattened into a single chain. Use compose instead."}))})

(defn accumulate-error*
  ([env error]
   (accumulate-error* (make-filtergraph []) env error))
  ([return-val env error]
   (let [info (ex-data error)]
     (when-not (some #(= (ex-data %) info) @(:errors env))
       (log/warn (if *warn-verbose* info (:error-type info)))
       (swap! (:errors env) conj error)))
   return-val))

(defn accumulate-error
  ([env sym err-code]
   (accumulate-error* env ((err-code errors) sym)))
  ([return-val env sym err-code]
   (accumulate-error* return-val env ((err-code errors) sym)))
  ([return-val env sym spec err-code]
   (accumulate-error* return-val env ((err-code errors) sym spec))))

(defn error-processing [env]
  (when *debug-mode* (log/debug env))
  (case (count @(:errors env))    
    1 (ex-data (first @(:errors env)))
    (ex-data (first @(:errors env)))))

(defn collect-errors [env]
  (let [errors (atom [])]
    (postwalk (fn [node] (if (map? node)
                          (swap! errors conj @(:errors node))
                          node)) @env)
    @errors))
