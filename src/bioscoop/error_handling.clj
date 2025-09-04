(ns bioscoop.error-handling
  (:require [clojure.spec.alpha :as s]))

(def errors {:not-a-filtergraph (fn [sym] (ex-info "Not a valid ffmpeg program (filtergraph)"
                                                {:symbol sym
                                                 :symbol-type (type sym)
                                                 :error-type :not-a-filtergraph
                                                 :explanation "End your program with a filter, chain, or graph operation"}))
             :reserved-word (fn [sym] (let [explanation (str "Reserved word: '" sym "'\n"
                                                         "This is the name of an existing ffmpeg filter and is reserved. Please use a different name")]
                                       (ex-info explanation
                                                {:symbol sym
                                                 :error-type :reserved-word
                                                 :explanation explanation})))
             :unresolved-function (fn [sym] (ex-info "Cannot resolve function" {:error-type :unresolved-function
                                                                                :explanation "Cannot resolve function"
                                                                                :symbol sym }))
             :bad-apple (fn [sym] (ex-info "All expressions in DSL program must produce filter operations"
                                              {:symbol sym
                                               :error-type :bad-apple
                                               :explanation "Each expression should create filters, chains, or graphs"}))
             :invalid-parameter (fn [sym spec] (ex-info "Not a valid parameter" {:symbol sym
                                                                                :error-type :invalid-parameter
                                                                                :explanation (s/explain-str spec sym)
                                                                                :explanation-data (s/explain-data spec sym)}))
             :padded-graph-multiple-filterchains (fn [sym] (ex-info "You can only label pads on a filtergraph that consists of one and only one filterchain."
                                                                   {:symbol sym
                                                                    :error-type :padded-graph
                                                                    :explanation "Multiple filterchains found. You can only label pads one filterchain at the time"}))
             :padded-graph-not-a-filtergraph (fn [sym] (ex-info "You can only label pads on a filtergraph expression. "
                                                               {:symbol sym
                                                                :error-type :padded-graph
                                                                :explanation "Not a filtergraph expression. You can only label pads on a filtergraph expression."}))})

(defn accumulate-error* [env error]
  (swap! (:errors env) conj error))

(defn accumulate-error
  ([env sym err-code]
   (accumulate-error* env ((err-code errors) sym)))
  ([env sym spec err-code]
   (accumulate-error* env ((err-code errors) sym spec))))

(defn error-processing [env]
  (case (count @(:errors env))    
    1 (ex-data (first @(:errors env)))
    (ex-data (first @(:errors env)))))



