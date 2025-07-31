(ns bioscoop.render
  (:require [clojure.string :as str]
            [bioscoop.dsl :refer [make-filterchain make-filtergraph]])
  (:import [bioscoop.dsl Filter FilterChain FilterGraph]))

;; Transform our data structures to ffmpeg filter format
(defprotocol FFmpegRenderable
  (to-ffmpeg [this] "Convert to ffmpeg filter string"))

(extend-protocol FFmpegRenderable
  Filter
  (to-ffmpeg [{:keys [name args input-labels output-labels]}]
    (let [input-str (when (seq input-labels)
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
(defn with-labels [filter input-labels output-labels]
  (assoc filter 
         :input-labels (vec input-labels)
         :output-labels (vec output-labels)))

(defn chain-filters [& filters]
  (make-filterchain filters))

(defn parallel-filters [& chains]
  (make-filtergraph chains))
