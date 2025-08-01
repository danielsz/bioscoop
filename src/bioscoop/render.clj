(ns bioscoop.render
  (:require [clojure.string :as str]
            [bioscoop.dsl :refer [make-filterchain make-filtergraph get-input-labels get-output-labels]])
  (:import [bioscoop.dsl Filter FilterChain FilterGraph]))

;; Transform our data structures to ffmpeg filter format
(defprotocol FFmpegRenderable
  (to-ffmpeg [this] "Convert to ffmpeg filter string"))

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
          args-str (when args (str "=" args))]
      (str input-str name args-str output-str)))

  FilterChain
  (to-ffmpeg [{:keys [filters]}]
    (str/join "," (map to-ffmpeg filters)))

  FilterGraph
  (to-ffmpeg [{:keys [chains]}]
    (str/join ";" (map to-ffmpeg chains))))

;; Helper functions for common patterns
(defn chain-filters [& filters]
  (make-filterchain filters))

(defn parallel-filters [& chains]
  (make-filtergraph chains))
