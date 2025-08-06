(ns bioscoop.render
  (:require [clojure.string :as str]
            [bioscoop.dsl :refer [get-input-labels get-output-labels]]
            [bioscoop.domain.records :refer [make-filterchain make-filtergraph]])
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

(extend-protocol DSLRenderable
  Filter
  (to-dsl [filter]
    (let [{:keys [name args]} filter
          input-labels (get-input-labels filter)
          output-labels (get-output-labels filter)]
      (if args
            (format "(filter %s %s)" name (str args))
                    (format "(filter \"%s\")" name))))

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
