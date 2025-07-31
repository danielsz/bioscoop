(ns bioscoop.ffmpeg
  (:import [net.bramp.ffmpeg FFmpeg FFprobe FFmpegExecutor RunProcessFunction]
           [net.bramp.ffmpeg.builder FFmpegBuilder FFmpegOutputBuilder]))

(defn img2video [working-dir & {:keys [image sound subtitles output]}]
  (let [run (doto (RunProcessFunction.)
              (.setWorkingDirectory working-dir))
        ffmpeg (FFmpeg. "/usr/bin/ffmpeg" run)
        ffprobe (FFprobe. "/usr/bin/ffprobe" run)
        output (doto (FFmpegOutputBuilder.)
                 (.setFormat "mp4")
                 (.setVideoFrameRate (int 1) (int 1))
                 (.setAudioCodec "aac")
                 (.addExtraArgs (into-array String ["-shortest"]))
                 (.setFilename output))
        builder (doto (FFmpegBuilder.)
                  (.addInput image)
                  (.addExtraArgs (into-array String ["-loop" "1" "-r" "1"]))
                  (.addInput sound)
                  (cond-> (some? subtitles)
                    (.setVideoFilter (str "subtitles=" subtitles ",scale=-2:'min(720,ih)'")))
                  (.overrideOutputFiles true)
                  (.addOutput output))
        executor (FFmpegExecutor. ffmpeg ffprobe)
        job (.createJob executor builder)]
    (try (.run job)
         (catch Exception _ (.getState job)))
    (.getState job)))
