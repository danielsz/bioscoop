(ns bioscoop.trace
  (:require [bioscoop.render :refer [to-ffmpeg]]
            [bioscoop.config :refer [*debug-mode*]]
            [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:import [bioscoop.domain.records FilterGraph]))

(defn trace> [node env result]
  (when (seq *debug-mode*)
    (when (some *debug-mode* [(first node)])
      (log/debug node result (type result))))
  (tap> (cond-> {:node-type (first node)
                 :node node
                 :env  env
                 :result result}
          (instance? FilterGraph result) (assoc :ffmpeg (to-ffmpeg result))))
  result)
