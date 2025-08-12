(ns bioscoop.ffmpeg
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import [java.lang ProcessBuilder]))

(def ffmpeg-bin "/usr/bin/ffmpeg")
(def ffplay-bin "/usr/bin/ffplay")
(def ffprobe-bin "/usr/bin/ffprobe")

(defn filter-complex [filter & {:keys [working-dir output] :or {working-dir (System/getProperty "java.io.tmpdir") output "output.mp4"}}]
  (let [log (io/file (str (System/getProperty "java.io.tmpdir") "/bioscoop.log"))
        cmd [ffmpeg-bin "-y" "-filter_complex" filter "-map" "[out]" output]
        pb (ProcessBuilder. cmd)]
    (.redirectOutput pb log)
    (.redirectError pb log)
    (.directory pb (io/file working-dir))
    (.start pb)))


(defn with-inputs
  "It's possible to destroy the process if we keep a handle on the Process instance"
  [filter & inputs]
  (let [log (io/file (str (System/getProperty "java.io.tmpdir") "/bioscoop.log"))
        cmd (-> [ffmpeg-bin "-y" "-i"]
               (into (interpose "-i" inputs))
               (conj "-filter_complex" filter "-map" "[out]" "-t" "360" "output.mp4"))
        pb (ProcessBuilder. cmd)]
    (log/debug cmd)
    (.redirectOutput pb log)
    (.redirectError pb log)
    (.directory pb (io/file (System/getProperty "java.io.tmpdir")))
    (.start pb)))
