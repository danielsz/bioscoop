(ns bioscoop.registry)

(def ^:private graph-registry (atom {}))

(defn register-graph! [name graph]
  (swap! graph-registry assoc name graph))

(defn get-graph [name]
  (when-let [graph (get @graph-registry name)]
    graph))

(defn clear-registry!
  "Clear registry (mainly for testing)"
  []
  (reset! graph-registry {}))
