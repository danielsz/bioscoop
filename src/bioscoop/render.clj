(ns bioscoop.render
  (:require [clojure.string :as str]
            [bioscoop.dsl :refer [get-input-labels get-output-labels]]
            [bioscoop.domain.records :refer [make-filterchain make-filtergraph]]
            [clojure.tools.logging :as log])
  (:import [bioscoop.domain.records Filter FilterChain FilterGraph]))

;; Transform our data structures to ffmpeg filter format
(defprotocol FFmpegRenderable
  (to-ffmpeg [this] "Convert to ffmpeg filter string"))

;; Transform our data structures back to DSL format
(defprotocol DSLRenderable
  (to-dsl [this] "Convert to DSL string"))

(extend-protocol FFmpegRenderable
  Filter
  (to-ffmpeg [filter]
    (let [{:keys [name args]} filter
          input-labels (get-input-labels filter)
          output-labels (get-output-labels filter)
          input-str (when (seq input-labels)
                      (str/join "" (map #(str "[" % "]") input-labels)))
          output-str (when (seq output-labels)
                       (str/join "" (map #(str "[" % "]") output-labels)))
          args-str (when args
                     (str "=" (str/join ":" (map (fn [[k v]] (str (clojure.core/name k) "=" v)) args))))]
      (str input-str name args-str output-str)))

  FilterChain
  (to-ffmpeg [{:keys [filters]}]
    (str/join "," (map to-ffmpeg filters)))

  FilterGraph
  (to-ffmpeg [{:keys [chains]}]
    (str/join ";" (mapv to-ffmpeg chains))))

(declare drop-namespace-from-map)

(extend-protocol DSLRenderable
  Filter
  (to-dsl [filter]
    (let [{:keys [name args]} filter
          input-labels (get-input-labels filter)
          output-labels (get-output-labels filter)
          filter (if args
                   [(symbol name) (drop-namespace-from-map args)]
                   [(symbol name)])
          with-labels (cond-> filter
                        (> (count input-labels) 1) (conj `(~(symbol "input-labels") ~@input-labels))
                        (= 1 (count input-labels)) (conj {:input (first input-labels)})
                        (> (count output-labels) 1) (conj `(~(symbol "output-labels") ~@output-labels))
                        (= 1 (count output-labels)) (conj {:output (first output-labels)}))]
      (str (apply list with-labels))))

  FilterChain
  (to-dsl [{:keys [filters]}]
    (if (= 1 (count filters))
      ;; Single filter, no need for chain wrapper
      (to-dsl (first filters))
      ;; Multiple filters, use chain
      (format "(chain %s)" (str/join " " (map to-dsl filters)))))

  FilterGraph
  (to-dsl [{:keys [chains]}]
    (cond
      ;; Single chain with single filter - just return the filter
      (and (= 1 (count chains))
           (= 1 (count (:filters (first chains)))))
      (to-dsl (first (:filters (first chains))))

      ;; Single chain with multiple filters - return the chain
      (= 1 (count chains))
      (to-dsl (first chains))

      ;; Multiple chains - use graph
      :else
      (format "(graph %s)" (str/join " " (map to-dsl chains))))))

(defn drop-namespace-from-map
  "Transforms a map by removing the namespace from qualified keyword keys."
  [m]
  (reduce-kv (fn [acc k v]
               (assoc acc (if (qualified-keyword? k) (keyword (name k)) k) v))
             {}
             m))
