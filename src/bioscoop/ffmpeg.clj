(ns bioscoop.ffmpeg
  (:require [clojure.java.io :as io])
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
