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
          output-labels (get-output-labels filter)

          ;; Split args back into individual arguments
          arg-parts (when args (str/split args #":"))

          ;; Helper to format arguments - preserve quotes for string-like args
          format-args (fn [parts]
                        (map (fn [part]
                               ;; If the part looks like it might need quotes (contains letters but not just numbers)
                               (if (and (string? part)
                                        (re-find #"[a-zA-Z/]" part)
                                        (not (re-matches #"\d+" part)))
                                 (pr-str part)
                                 part))
                             parts))

          ;; Check if this is a known built-in filter with shorthand syntax
          shorthand-fn (case (str name)
                         "scale" :scale
                         "crop" :crop
                         "overlay" :overlay
                         "fade" :fade
                         nil)]

      ;; If we have labels, always use generic filter syntax
      (if (or (seq input-labels) (seq output-labels))
        (let [args-part (if args (str " " (str/join " " (format-args arg-parts))) "")
              ;; For multiple labels, create separate {:input "label"} entries
              input-parts (when (seq input-labels)
                            (str/join "" (map #(format " {:input %s}" (pr-str %)) input-labels)))
              output-parts (when (seq output-labels)
                             (str/join "" (map #(format " {:output %s}" (pr-str %)) output-labels)))]
          (format "(filter \"%s\"%s%s%s)"
                  name
                  args-part
                  (or input-parts "")
                  (or output-parts "")))

        ;; No labels - use shorthand if available, otherwise generic
        (if shorthand-fn
          ;; Use shorthand syntax
          (case shorthand-fn
            :scale (format "(scale %s %s)" (first arg-parts) (second arg-parts))
            :crop (format "(crop %s %s %s %s)"
                          (first (format-args [(first arg-parts)]))
                          (first (format-args [(second arg-parts)]))
                          (first (format-args [(nth arg-parts 2)]))
                          (first (format-args [(nth arg-parts 3)])))
            :overlay "(overlay)"
            :fade (format "(fade %s %s %s)"
                          (first (format-args [(first arg-parts)]))
                          (first (format-args [(second arg-parts)]))
                          (first (format-args [(nth arg-parts 2)]))))
          ;; Use generic filter syntax
          (if args
            (format "(filter \"%s\" %s)" name (str/join " " (format-args arg-parts)))
            (format "(filter \"%s\")" name))))))

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
