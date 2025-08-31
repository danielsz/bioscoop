(ns bioscoop.registry
  (:require [clojure.tools.logging :as log]))

(def ^:private graph-registry (atom {}))

(defn register-graph! [name graph]
  {:pre [(not (ns-resolve 'bioscoop.built-in name))]} ;; catches clojure.core too
  (swap! graph-registry assoc name graph))

(defn get-graph [name]
  (when-let [graph (get @graph-registry name)]
    graph))

(defn clear-registry!
  "Clear registry (mainly for testing)"
  []
  (doseq [[name _] @graph-registry]
    (ns-unmap *ns* name))
  (reset! graph-registry {}))

(defn debug []
  (keys @graph-registry))
