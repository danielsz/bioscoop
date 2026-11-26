(ns bioscoop.ffmpeg
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import [java.lang ProcessBuilder ProcessBuilder$Redirect]))

(def ffmpeg-bin (System/getProperty "ffmpeg.bin" "/usr/bin/ffmpeg"))
(def ffplay-bin (System/getProperty "ffplay.bin" "/usr/bin/ffplay"))
(def ffprobe-bin (System/getProperty "ffprobe.bin" "/usr/bin/ffprobe") )

(defn- device-list [device]
  (cond
    (nil? device) []
    (sequential? device) device
    :else [device]))

(defn build-command
  "Build the FFmpeg argument vector without starting a process.

  Global hardware-device options are emitted before inputs, with
  -init_hw_device preceding -filter_hw_device."
  [{:keys [filtergraph maps out-filename gop preset
           init-hw-device filter-hw-device t]
    :or {out-filename "output.mp4"
         preset []}}
   inputs]
  (let [presets {:h264 ["-c:v" "libx264" "-preset" "slow" "-crf" "18" "-pix_fmt" "yuv420p"]
                 :h265 ["-c:v" "libx265" "-tag:v" "hvc1" "-preset" "slow" "-crf" "18"]
                 :av1 ["-c:v" "libsvtav1" "-preset" "5" "-crf" "18"]
                 :progressive ["-movflags" "+faststart"]}
        resolve-presets (fn [preset]
                          (let [ks (cond (keyword? preset) [preset]
                                         (string? preset) [(keyword preset)]
                                         :else preset)]
                            (mapcat (fn [k]
                                      (or (get presets k)
                                          (throw (ex-info "Unknown preset" {:preset k :known (keys presets)}))))
                                    ks)))]
    (-> [ffmpeg-bin "-y" "-nostdin" "-hide_banner"]
        (into (mapcat (fn [device]
                        ["-init_hw_device" (str device)])
                      (device-list init-hw-device)))
        (into (when filter-hw-device ["-filter_hw_device" (str filter-hw-device)]))
        (into (interleave (repeat "-i") inputs))
        (conj "-filter_complex" filtergraph)
        (into maps)
        (into (when gop ["-g" (str gop)]))
        (into (resolve-presets preset))
        (into (when (some? t) ["-t" (str t)]))
        (conj out-filename))))

(defn with-inputs
  "Start an FFmpeg process with inputs and a filtergraph.

  Usage:
    (with-inputs {:filtergraph \"[0:v]scale=1280:-2[v]\"
                  :maps [\"-map\" \"[v]\"]
                  :preset [:h264 :progressive]}
      \"in.mp4\")

  Inputs are strings (paths or URLs), given as separate arguments or as
  sequences of strings. Other types, such as java.io.File, must be
  converted with `str` first.

  Options:
    :filtergraph  - FFmpeg filtergraph string, passed as -filter_complex
                    (required)
    :maps         - Vector of -map arguments, e.g. [\"-map\" \"[v]\" \"-map\" \"0:a\"]
    :out-dir      - Working and output directory (default: java.io.tmpdir).
                    Must already exist. Relative input paths are resolved
                    against it, not against the JVM's working directory.
    :out-filename - Output file, relative to :out-dir (default: \"output.mp4\")
    :preset       - A keyword or vector of keywords selecting output options
                    (default: none, so ffmpeg's own defaults apply):
                      :h264        libx264, slow, crf 18, yuv420p
                      :h265        libx265, slow, crf 18, hvc1 tag
                      :av1         libsvtav1, preset 5, crf 18
                      :progressive -movflags +faststart (MP4/MOV outputs only;
                                   ffmpeg fails on other containers)
                    Presets are applied in order, so for duplicate options the
                    later preset wins. Unknown presets throw ex-info.
    :gop          - Keyframe interval, passed as -g
    :init-hw-device - Global hardware device declaration, passed as
                      -init_hw_device. Accepts a string or a collection of
                      strings, allowing multiple initialized devices.
    :filter-hw-device - Name of the global filter hardware device, passed as
                        -filter_hw_device. It must name a device created by
                        :init-hw-device. FFmpeg allows only one global filter
                        device.
    :verbose      - Print the full command (as a vector of arguments)
    :blocking     - Wait for ffmpeg to exit before returning
    :t            - Output duration limit, passed as -t before the output
                    filename. Processing stops once the output reaches this
                    duration. Accepts a number or a duration string.  

  Output files are overwritten (-y). stdout and stderr are merged into
  bioscoop.log inside :out-dir.
  
  Returns the java.lang.Process. Stop it with (.destroy p), (.isAlive
  p) to check if running, (.exitValue p) to inspect the outcome."
  
  [{:keys [out-dir verbose blocking]
    :or {out-dir (System/getProperty "java.io.tmpdir")
         verbose false
         blocking false}
    :as opts}
   & inputs]
  (let [inputs (reduce (fn [acc xs] (if (sequential? xs) (into acc xs) (conj acc xs))) [] inputs)
        cmd (build-command opts inputs)
        log (io/file (str out-dir "/bioscoop.log"))]
    (when verbose (println (pr-str cmd)))
    (let [pb (doto (ProcessBuilder. ^java.util.List cmd)
               (.redirectErrorStream true)
               (.redirectOutput (ProcessBuilder$Redirect/to log))
               (.directory (io/file out-dir)))
          process (.start pb)]
      (when blocking (.waitFor process))
      process)))

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

  ;; Vulkan denoising with hardware frame upload/download
  (with-inputs
    {:filtergraph "[0:v]hwupload=derive_device=vulkan,nlmeans_vulkan=s=1.0,hwdownload,format=yuv420p[out]"
     :maps ["-map" "[out]"]
     :init-hw-device "vulkan=vk:0"
     :filter-hw-device "vk"
     :preset [:h264]}
    "input.mp4")
  
  ;; Destroy process when done
  (let [proc (with-inputs {} "input1" "input2")]
    ;; ... do work ...
    (.destroy proc)))
