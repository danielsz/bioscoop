(ns bioscoop.registry
  (:require [bioscoop.config :refer [*dynamic-resolution* *trace-registry*]]
            [bioscoop.resolve  :refer [reserved-word?]]
            [clojure.tools.logging :as log]
            [bioscoop.error-handling :refer [accumulate-error]])
  (:import [bioscoop.domain.records FilterGraph]))

(def ^:private graph-registry (atom {}))

(defn trace! [name]
  (when *trace-registry*
      (if (contains? @graph-registry name)
        (log/info "Redefining" name)
        (log/info "Registering graph definition" name))))

(defn register! [name graph]
  (trace! name)
  (swap! graph-registry assoc name graph))

(defn register-graph! [name graph env]
  (if (reserved-word? name)
    (accumulate-error env name :reserved-word)
    (register! (symbol name) graph)))

(defn get-var [name]
  (when-let [v (if (namespace name) (find-var name) (resolve name))] ;; allows aliasing of filtergraph in defs
      (when (and (bound? v) (instance? FilterGraph (var-get v)))
        (var-get v))))

(defmulti get-graph (fn [name] *dynamic-resolution*))

(defmethod get-graph false [name]
  (get @graph-registry name))

(defmethod get-graph true [name]
  (get-var name))

(defn clear-registry!
  "Clear registry (mainly for testing)"
  []
  (doseq [[name _] @graph-registry]
    (ns-unmap *ns* name))
  (reset! graph-registry {}))

(defn debug [& {:keys [verbose] :or {verbose false}}]
  (if verbose
    @graph-registry
    (keys @graph-registry)))

