(ns bioscoop.registry
  (:require [bioscoop.config :refer [*dynamic-resolution*]]
            [bioscoop.resolve  :refer [reserved-word?]]
            [bioscoop.error-handling :refer [accumulate-error]])
  (:import [bioscoop.domain.records FilterGraph]))

(def ^:private graph-registry (atom {}))

(defn register-graph!
  ([name graph]
   (swap! graph-registry assoc name graph))
  ([name graph env]
   (if (reserved-word? name)
     (accumulate-error env name :reserved-word)
     (register-graph! (symbol name) graph))))

(defn get-var [name]
  (when-let [v (if (namespace name) (find-var name) (resolve name))] ;; allows aliasing of filtergraph in defs
      (when (and (bound? v) (instance? FilterGraph (var-get v)))
        (var-get v))))

(defn get-graph [name]
  (if-let [graph (get @graph-registry name)]
    graph
    (when *dynamic-resolution*
      (get-var name))))

(defn clear-registry!
  "Clear registry (mainly for testing)"
  []
  (doseq [[name _] @graph-registry]
    (ns-unmap *ns* name))
  (reset! graph-registry {}))

(defn debug []
  (keys @graph-registry))

