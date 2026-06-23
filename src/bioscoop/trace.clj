(ns bioscoop.trace
  (:require [bioscoop.render :refer [to-ffmpeg]])
  (:import [bioscoop.domain.records FilterGraph]))

(defn trace> [node env result]
  (tap> {:node-type (first node)
         :node node
         :env  env
         :result result
         :ffmpeg (when (instance? FilterGraph result)
                   (to-ffmpeg result))
         :partial? (not (instance? FilterGraph result))})
  result)
