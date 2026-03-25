(ns bioscoop.ffmpeg
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import [java.lang ProcessBuilder]))

(def ffmpeg-bin (System/getProperty "ffmpeg.bin" "/usr/bin/ffmpeg"))
(def ffplay-bin (System/getProperty "ffplay.bin" "/usr/bin/ffplay"))
(def ffprobe-bin (System/getProperty "ffprobe.bin" "/usr/bin/ffprobe") )

(defn with-inputs
  "Start FFmpeg process with inputs and filtergraph.
  
  Usage:
    (with-inputs {:filtergraph \"...\" :maps [...] :out-dir \"...\" :out-filename \"...\"} & inputs)
  
  Options:
    :filtergraph - FFmpeg filtergraph string
    :maps - Vector of -map arguments
    :out-dir - Output directory (defaults to java.io.tmpdir)
    :out-filename - Output filename (defaults to \"output.mp4\")
  
  Returns the process instance. Which can be destroyed with (.destroy handle)"
  [{:keys [filtergraph maps out-dir out-filename verbose]
    :or {out-dir (System/getProperty "java.io.tmpdir")
         out-filename "output.mp4"
         verbose false}} & inputs]
  (let [log (io/file (str out-dir "/bioscoop.log"))
        cmd (-> [ffmpeg-bin "-y"]
                (into (interleave (repeat "-i") inputs))
                (conj "-filter_complex" filtergraph)
                (into maps)
                (conj out-filename))
        pb (ProcessBuilder. cmd)]
    (when verbose (println cmd))
    (.redirectOutput pb log)
    (.redirectError pb log)
    (.directory pb (io/file out-dir))
    (.start pb)))

(comment
  ;; Scenario 1: Duck music with voice (audio only)
  (with-inputs
    {:filtergraph "[0:a][1:a]sidechaincompress=threshold=0.03:ratio=6:attack=100:release=800:makeup=3[bg];[bg][1:a]amix=inputs=2:duration=longest[out]"
     :maps ["-map" "[out]"]}
    "music.mp3" "voice.mp3")
  
  ;; Scenario 2: Video with separate music and voice (duck music with voice)
  (with-inputs
    {:filtergraph "[1:a][2:a]sidechaincompress=threshold=0.03:ratio=6:attack=100:release=800:makeup=3[bg];[bg][2:a]amix=inputs=2:duration=longest[audio]"
     :maps ["-map" "0:v" "-map" "[audio]"]}
    "video.mp4" "music.mp3" "voice.mp3")
  
  ;; Scenario 3: Mix video audio + music, then duck with voice
  (with-inputs
    {:filtergraph "[0:a][1:a]amix=inputs=2:duration=longest[music_mix];[music_mix][2:a]sidechaincompress=threshold=0.03:ratio=6:attack=100:release=800:makeup=3[bg];[bg][2:a]amix=inputs=2:duration=longest[audio]"
     :maps ["-map" "0:v" "-map" "[audio]"]}
    "video.mp4" "music.mp3" "voice.mp3")
  
  ;; Simple mix example
  (with-inputs
    {:filtergraph "[0:a][1:a]amix[out]"
     :maps ["-map" "[out]"]}
    "music.mp3" "voice.mp3")
  
  ;; Destroy process when done
  (let [proc (with-inputs {} "input1" "input2")]
    ;; ... do work ...
    (.destroy proc)))
