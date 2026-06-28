(ns bioscoop.trace
  (:require [bioscoop.render :refer [to-ffmpeg]])
  (:import [bioscoop.domain.records FilterGraph]))

(defn trace> [node env result]
  (tap> (cond-> {:node-type (first node)
                 :node node
                 :env  env
                 :result result}
          (instance? FilterGraph result) (assoc :ffmpeg (to-ffmpeg result))))
  result)
